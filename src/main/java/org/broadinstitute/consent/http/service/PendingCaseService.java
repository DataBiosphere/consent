package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteStatus;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DataOwnerCase;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

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

    private ConsentDAO consentDAO;
    private DACUserDAO dacUserDAO;
    private DataSetDAO dataSetDAO;
    private ElectionDAO electionDAO;
    private MongoConsentDB mongo;
    private UserRoleDAO userRoleDAO;
    private VoteDAO voteDAO;
    private DacService dacService;

    @Inject
    public PendingCaseService(ConsentDAO consentDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO,
                              ElectionDAO electionDAO, MongoConsentDB mongo,
                              UserRoleDAO userRoleDAO, VoteDAO voteDAO, DacService dacService) {
        this.consentDAO = consentDAO;
        this.dacUserDAO = dacUserDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.mongo = mongo;
        this.userRoleDAO = userRoleDAO;
        this.voteDAO = voteDAO;
        this.dacService = dacService;
    }

    public List<PendingCase> describeConsentPendingCases(AuthUser authUser) throws NotFoundException {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(authUser.getName());
        List<Integer> roleIds = userRoleDAO.findRolesByUserEmail(authUser.getName()).stream().
                map(UserRole::getRoleId).
                collect(Collectors.toList());
        Integer dacUserId = dacUser.getDacUserId();
        List<Election> elections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.TRANSLATE_DUL.getValue(), ElectionStatus.OPEN.getValue());
        List<PendingCase> pendingCases = dacService.filterElectionsByDAC(elections, authUser).
                stream().
                map(e -> {
                    Vote vote = voteDAO.findVoteByElectionIdAndDACUserId(e.getElectionId(), dacUserId);
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
        DACUser dacUser = dacUserDAO.findDACUserByEmail(authUser.getName());
        Integer dacUserId = dacUser.getDacUserId();
        boolean isChair = dacService.isAuthUserChair(authUser);
        List<Election> unfilteredElections = isChair ?
                electionDAO.findElectionsByTypeAndFinalAccessVoteChairPerson(ElectionType.DATA_ACCESS.getValue(), false) :
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
                    isReminderSent = accessVote.getIsReminderSent() || rpVote.getIsReminderSent();
                    pendingCase.setRpElectionId(rpElectionId);
                    pendingCase.setAlreadyVoted(accessVote.getVote() != null && rpVote.getVote() != null);
                    pendingCase.setElectionStatus(rpElection.getStatus().equals(ElectionStatus.FINAL.getValue()) && election.getStatus().equals(ElectionStatus.FINAL.getValue()) ? ElectionStatus.FINAL.getValue() : ElectionStatus.OPEN.getValue());                 // if it's already voted, we should collect vote or do the final election vote
                    pendingCase.setRpVoteId(rpVote.getVoteId());
                    pendingCase.setStatus(accessVote.getVote() == null || rpVote.getVote() == null ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());

                } else {
                    isReminderSent = (accessVote.getIsReminderSent());
                    pendingCase.setAlreadyVoted(accessVote.getVote() != null);
                    pendingCase.setElectionStatus(election.getStatus().equals(ElectionStatus.FINAL.getValue()) ? ElectionStatus.FINAL.getValue() : ElectionStatus.OPEN.getValue());                 // if it's already voted, we should collect vote or do the final election vote
                    pendingCase.setStatus(accessVote.getVote() == null ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());
                }
                setGeneralFields(pendingCase, election, accessVote, isReminderSent);
                setFinalVote(dacUserId, election, pendingCase);
                pendingCases.add(pendingCase);
            }
        }
        return pendingCases;
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
                if(CollectionUtils.isNotEmpty(dataOwnerVotes)){
                    dataOwnerVotes.forEach(v -> {
                        BasicDBObject query = new BasicDBObject().append(DarConstants.ID, new ObjectId(election.getReferenceId()));
                        FindIterable<Document> dataAccessRequest = mongo.getDataAccessRequestCollection().find(query);
                        DataSet dataSet = dataSetDAO.findDataSetById(election.getDataSetId());
                        dataOwnerCase.setAlias(dataSet.getAlias());
                        dataOwnerCase.setDarCode(dataAccessRequest != null ?  dataAccessRequest.first().get(DarConstants.DAR_CODE).toString() : null);
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
        pendingCase.setTotalVotes(votes.size());
        pendingCase.setVotesLogged(votes.size() - pendingVotes.size());
        pendingCase.setReferenceId(election.getReferenceId());
        if (election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())) {
            BasicDBObject query = new BasicDBObject().append(DarConstants.ID, new ObjectId(election.getReferenceId()));
            FindIterable<Document> dataAccessRequest = mongo.getDataAccessRequestCollection().find(query);
            if (dataAccessRequest.first() != null) {
                pendingCase.setFrontEndId(dataAccessRequest.first().get(DarConstants.DAR_CODE).toString());
                pendingCase.setProjectTitle(dataAccessRequest.first().get(DarConstants.PROJECT_TITLE).toString());
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

}
