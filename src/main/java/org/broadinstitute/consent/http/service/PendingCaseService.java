package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteStatus;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DataOwnerCase;
import org.broadinstitute.consent.http.util.DarUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class PendingCaseService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ConsentDAO consentDAO;
    private DataAccessRequestService dataAccessRequestService;
    private DataSetDAO dataSetDAO;
    private ElectionDAO electionDAO;

    private VoteDAO voteDAO;
    private DacService dacService;
    private UserService userService;
    private VoteService voteService;

    @Inject
    public PendingCaseService(ConsentDAO consentDAO, DataAccessRequestService dataAccessRequestService,
                              DataSetDAO dataSetDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DacService dacService,
                              UserService userService, VoteService voteService) {
        this.consentDAO = consentDAO;
        this.dataAccessRequestService = dataAccessRequestService;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.dacService = dacService;
        this.userService = userService;
        this.voteService = voteService;
    }

    public List<PendingCase> describeConsentPendingCases(AuthUser authUser) throws NotFoundException {
        DACUser dacUser = userService.findUserByEmail(authUser.getName());
        List<Integer> roleIds = dacUser.getRoles().stream().
                map(UserRole::getRoleId).
                collect(Collectors.toList());
        Integer dacUserId = dacUser.getDacUserId();
        List<Election> elections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.TRANSLATE_DUL.getValue(), ElectionStatus.OPEN.getValue());
        List<PendingCase> pendingCases = dacService.filterElectionsByDAC(elections, authUser).
                stream().
                map(e -> {
                    Vote vote = voteDAO.findVoteByElectionIdAndDACUserId(e.getElectionId(), dacUserId);
                    if (vote == null) {
                        // Handle error case where user votes have not been created for the current election
                        createMissingUserVotes(e, dacUser);
                        vote = voteDAO.findVoteByElectionIdAndDACUserId(e.getElectionId(), dacUserId);
                    }
                    if (vote != null) {
                        PendingCase pendingCase = new PendingCase();
                        setGeneralFields(pendingCase, e, vote, vote.getIsReminderSent());
                        return pendingCase;
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull).
                collect(Collectors.toList());
        if (roleIds.contains(UserRoles.CHAIRPERSON.getRoleId())) {
            return orderPendingCasesForChairperson(pendingCases);
        }
        if (roleIds.contains(UserRoles.MEMBER.getRoleId())) {
            return orderPendingCasesForMember(pendingCases);
        }
        return pendingCases;
    }

    public List<PendingCase> describeDataRequestPendingCases(AuthUser authUser) throws NotFoundException {
        DACUser dacUser = userService.findUserByEmail(authUser.getName());
        Integer dacUserId = dacUser.getDacUserId();
        boolean isChair = dacService.isAuthUserChair(authUser);
        List<Election> unfilteredElections = isChair ?
                electionDAO.findLastElectionsByTypeAndFinalAccessVoteChairPerson(ElectionType.DATA_ACCESS.getValue(), false) :
                electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue());
        List<Election> elections = dacService.filterElectionsByDAC(unfilteredElections, authUser);
        List<PendingCase> pendingCases = new ArrayList<>();
        if (elections != null) {
            for (Election election : elections) {
                Vote accessVote = voteDAO.findVoteByElectionIdAndDACUserId(election.getElectionId(),
                        dacUserId);
                if (accessVote == null) {
                    continue;
                }
                Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(election.getElectionId());
                PendingCase pendingCase = new PendingCase();
                Boolean isReminderSent;
                if (Objects.nonNull(rpElectionId)) {
                    Election rpElection = electionDAO.findElectionWithFinalVoteById(rpElectionId);
                    Vote rpVote = voteDAO.findVoteByElectionIdAndDACUserId(rpElectionId, dacUserId);
                    isReminderSent = accessVote.getIsReminderSent() || (Objects.nonNull(rpVote) && rpVote.getIsReminderSent());
                    pendingCase.setRpElectionId(rpElectionId);
                    pendingCase.setAlreadyVoted(accessVote.getVote() != null && Objects.nonNull(rpVote) && rpVote.getVote() != null);
                    pendingCase.setElectionStatus(rpElection.getStatus().equals(ElectionStatus.FINAL.getValue()) && election.getStatus().equals(ElectionStatus.FINAL.getValue()) ? ElectionStatus.FINAL.getValue() : ElectionStatus.OPEN.getValue());
                    if (Objects.nonNull(rpVote)) {
                        pendingCase.setRpVoteId(rpVote.getVoteId());
                    }
                    pendingCase.setStatus(accessVote.getVote() == null || (Objects.nonNull(rpVote) && rpVote.getVote() == null) ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());
                } else {
                    isReminderSent = (accessVote.getIsReminderSent());
                    pendingCase.setAlreadyVoted(accessVote.getVote() != null);
                    pendingCase.setElectionStatus(election.getStatus().equals(ElectionStatus.FINAL.getValue()) ? ElectionStatus.FINAL.getValue() : ElectionStatus.OPEN.getValue());
                    pendingCase.setStatus(accessVote.getVote() == null ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());
                }
                setGeneralFields(pendingCase, election, accessVote, isReminderSent);
                setFinalVote(dacUserId, election, pendingCase);
                pendingCases.add(pendingCase);
            }
        }
        return pendingCases.stream().distinct().collect(Collectors.toList());
    }

    public List<DataOwnerCase> describeDataOwnerPendingCases(Integer dataOwnerId, AuthUser authUser) {
        List<Election> elections = dacService.filterElectionsByDAC(
                electionDAO.getElectionByTypeAndStatus(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue()),
                authUser
        );
        List<DataOwnerCase> pendingCases = new ArrayList<>();
        if (elections != null) {
            for (Election election : elections) {
                DataOwnerCase dataOwnerCase = new DataOwnerCase();
                List<Vote> dataOwnerVotes = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), dataOwnerId, VoteType.DATA_OWNER.getValue());
                if (CollectionUtils.isNotEmpty(dataOwnerVotes)) {
                    dataOwnerVotes.forEach(v -> {
                        DataAccessRequest dataAccessRequest = dataAccessRequestService.findByReferenceId(election.getReferenceId());
                        DataSet dataSet = dataSetDAO.findDataSetById(election.getDataSetId());
                        dataOwnerCase.setAlias(dataSet.getAlias());
                        dataOwnerCase.setDarCode(dataAccessRequest != null ? dataAccessRequest.getData().getDarCode() : null);
                        dataOwnerCase.setDataSetId(dataSet.getDataSetId());
                        dataOwnerCase.setDataSetName(dataSet.getName());
                        dataOwnerCase.setVoteId(v.getVoteId());
                        dataOwnerCase.setAlreadyVoted(v.getVote() != null);
                        dataOwnerCase.setReferenceId(election.getReferenceId());
                        dataOwnerCase.setHasConcerns(v.getHasConcerns());
                        pendingCases.add(dataOwnerCase);
                    });
                }
            }
        }
        return pendingCases;
    }

    private void setGeneralFields(PendingCase pendingCase, Election election, Vote vote, boolean isReminderSent) {
        List<Vote> votes = voteDAO.findDACVotesByElectionId(election.getElectionId());
        List<Vote> pendingVotes = voteDAO.findPendingVotesByElectionId(election.getElectionId());
        Dac dac = electionDAO.findDacForElection(election.getElectionId());
        if (dac != null) {
            dac = dacService.findById(dac.getDacId());
            pendingCase.setDac(dac);
        }
        pendingCase.setTotalVotes(votes.size());
        pendingCase.setVotesLogged(votes.size() - pendingVotes.size());
        pendingCase.setReferenceId(election.getReferenceId());
        if (election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())) {
            DataAccessRequest dataAccessRequest = dataAccessRequestService.findByReferenceId(election.getReferenceId());
            if (dataAccessRequest != null) {
                pendingCase.setFrontEndId(dataAccessRequest.getData().getDarCode());
                pendingCase.setProjectTitle(dataAccessRequest.getData().getProjectTitle());
            }
        } else {
            pendingCase.setFrontEndId(consentDAO.findConsentById(election.getReferenceId()).getName());
            pendingCase.setConsentGroupName(consentDAO.findConsentById(election.getReferenceId()).getGroupName());
        }
        pendingCase.setLogged(setLogged(pendingCase.getTotalVotes(), pendingCase.getVotesLogged()));
        pendingCase.setAlreadyVoted(pendingCase.getAlreadyVoted() == null ? vote.getVote() != null : pendingCase.getAlreadyVoted());
        pendingCase.setStatus(vote.getVote() == null ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());
        pendingCase.setVoteId(vote.getVoteId());
        pendingCase.setIsReminderSent(isReminderSent);
        pendingCase.setCreateDate(election.getCreateDate());
        pendingCase.setElectionStatus(pendingCase.getElectionStatus() == null ? election.getStatus() : pendingCase.getElectionStatus());
        pendingCase.setElectionId(election.getElectionId());
    }

    private String setLogged(Integer totalVotes, Integer loggedVotes) {
        return loggedVotes + "/" + totalVotes;
    }

    private List<PendingCase> orderPendingCasesForChairperson(List<PendingCase> cases) {
        HashMap<Integer, Queue<PendingCase>> pendingCases = new HashMap<>();
        List<PendingCase> chairFinalList = cases.stream().
                filter(p1 -> p1.getTotalVotes().equals(p1.getVotesLogged())).
                collect(Collectors.toList());
        List<PendingCase> remainingList = cases.stream().
                filter(p1 -> !p1.getTotalVotes().equals(p1.getVotesLogged())).
                collect(Collectors.toList());
        for (PendingCase p : remainingList) {
            Integer key = Integer.parseInt(String.valueOf(p.getVotesLogged()) + p.getTotalVotes());
            if (!pendingCases.containsKey(key)) {
                pendingCases.put(key, new PriorityQueue<>());
            }
            Queue<PendingCase> pq = pendingCases.get(key);
            pq.add(p);
        }
        return generateFinalList(chairFinalList, pendingCases);
    }

    private List<PendingCase> generateFinalList(List<PendingCase> readyToCollect, HashMap<Integer, Queue<PendingCase>> pendingCases) {
        List<PendingCase> prevList = orderedVotedCases(orderListDecreased(pendingCases.keySet()), pendingCases);
        readyToCollect.addAll(prevList.stream().filter(PendingCase::getAlreadyVoted).collect(Collectors.toList()));
        readyToCollect.addAll(prevList.stream().filter(p1 -> !p1.getAlreadyVoted()).collect(Collectors.toList()));
        return readyToCollect;
    }

    private List<Integer> orderListDecreased(Set<Integer> setOfKeys) {
        return setOfKeys.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    private List<PendingCase> orderedVotedCases(List<Integer> keys, HashMap<Integer, Queue<PendingCase>> pendingCases) {
        ArrayList<PendingCase> votedCases = new ArrayList<>();
        for (Integer key : keys) {
            votedCases.addAll(pendingCases.get(key));
        }
        return votedCases;
    }

    private List<PendingCase> orderPendingCasesForMember(List<PendingCase> cases) {
        List<PendingCase> memberCaseList = new ArrayList<>();
        memberCaseList.addAll(cases.stream().filter(PendingCase::getIsReminderSent).collect(Collectors.toList()));
        memberCaseList.addAll(cases.stream().filter(p1 -> (!p1.getAlreadyVoted()) && (!p1.getIsReminderSent())).collect(Collectors.toList()));
        memberCaseList.addAll(cases.stream().filter(PendingCase::getAlreadyVoted).collect(Collectors.toList()));
        return memberCaseList;
    }

    private void setFinalVote(Integer dacUserId, Election election, PendingCase pendingCase) {
        if (pendingCase.getAlreadyVoted()) {
            Vote chairPersonVote = voteDAO.findChairPersonVoteByElectionIdAndDACUserId(
                    election.getElectionId(), dacUserId);
            if (chairPersonVote != null) {
                pendingCase.setIsFinalVote(chairPersonVote.getVote() != null);
            }
        } else {
            pendingCase.setIsFinalVote(false);
        }
    }

    private void createMissingUserVotes(Election e, DACUser dacUser) {
        ElectionType type = ElectionType.getFromValue(e.getElectionType());
        boolean isManualReview = false;
        if (type.equals(ElectionType.DATA_ACCESS)) {
            DataAccessRequest dar = dataAccessRequestService.findByReferenceId(e.getReferenceId());
            isManualReview = DarUtil.requiresManualReview(dar);
        }
        if (type.equals(ElectionType.TRANSLATE_DUL)) {
            Consent c = consentDAO.findConsentById(e.getReferenceId());
            isManualReview = c.getRequiresManualReview();
        }
        logger.info(String.format(
                "Creating missing votes for user id '%s', election id '%s', reference id '%s' ",
                dacUser.getDacUserId(),
                e.getElectionId(),
                e.getReferenceId()
        ));
        voteService.createVotesForUser(dacUser, e, type, isManualReview);
    }

}
