package org.broadinstitute.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteStatus;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsentService {

    private ConsentDAO consentDAO;
    private DacDAO dacDAO;
    private DACUserDAO dacUserDAO;
    private DataSetDAO dataSetDAO;
    private ElectionDAO electionDAO;
    private MongoConsentDB mongo;
    private UserRoleDAO userRoleDAO;
    private VoteDAO voteDAO;

    @Inject
    public ConsentService(ConsentDAO consentDAO, DacDAO dacDAO, DACUserDAO dacUserDAO,
                          DataSetDAO dataSetDAO, ElectionDAO electionDAO, MongoConsentDB mongo,
                          UserRoleDAO userRoleDAO, VoteDAO voteDAO) {
        this.consentDAO = consentDAO;
        this.dacDAO = dacDAO;
        this.dacUserDAO = dacUserDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.mongo = mongo;
        this.userRoleDAO = userRoleDAO;
        this.voteDAO = voteDAO;
    }

    @SuppressWarnings("unchecked")
    public List<ConsentManage> describeConsentManage(AuthUser authUser) {
        List<ConsentManage> consentManageList = new ArrayList<>();
        consentManageList.addAll(collectUnreviewedConsents(consentDAO.findUnreviewedConsents()));
        consentManageList.addAll(consentDAO.findConsentManageByStatus(ElectionStatus.OPEN.getValue()));
        consentManageList.addAll(consentDAO.findConsentManageByStatus(ElectionStatus.CANCELED.getValue()));
        List<ConsentManage> closedElections = consentDAO.findConsentManageByStatus(ElectionStatus.CLOSED.getValue());
        closedElections.forEach(consentManage -> {
            Boolean vote = voteDAO.findChairPersonVoteByElectionId(consentManage.getElectionId());
            consentManage.setVote(vote != null && vote ? "Approved" : "Denied");
        });
        consentManageList.addAll(closedElections);
        consentManageList.sort((c1, c2) -> c2.getSortDate().compareTo(c1.getSortDate()));
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue());
        if (!openElections.isEmpty()) {
            List<String> referenceIds = openElections.stream().map(Election::getReferenceId).collect(Collectors.toList());
            ObjectId[] objarray = new ObjectId[referenceIds.size()];
            for (int i = 0; i < referenceIds.size(); i++)
                objarray[i] = new ObjectId(referenceIds.get(i));
            BasicDBObject in = new BasicDBObject("$in", objarray);
            BasicDBObject q = new BasicDBObject(DarConstants.ID, in);
            FindIterable<Document> dataAccessRequests = mongo.getDataAccessRequestCollection().find(q);
            List<String> datasetNames = new ArrayList<>();
            dataAccessRequests.forEach((Block<Document>) dar -> {
                List<String> dataSets = dar.get(DarConstants.DATASET_ID, List.class);
                datasetNames.addAll(dataSets);
            });
            List<String> objectIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(datasetNames)) {
                objectIds = consentDAO.getAssociationsConsentIdfromDataSetIds(datasetNames);
            }

            for (ConsentManage consentManage : consentManageList) {
                if (objectIds.stream().anyMatch(cm -> cm.equals(consentManage.getConsentId()))) {
                    consentManage.setEditable(false);
                } else {
                    consentManage.setEditable(true);
                }
            }
        }
        return filterConsentManageByDAC(consentManageList, authUser);
    }

    public Integer getUnReviewedConsents(AuthUser authUser) {
        Collection<Consent> consents = consentDAO.findUnreviewedConsents();
        return filterConsentsByDAC(consents, authUser).size();
    }

    public List<PendingCase> describeConsentPendingCases(AuthUser authUser) throws NotFoundException {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(authUser.getName());
        List<Integer> roleIds = userRoleDAO.findRolesByUserEmail(authUser.getName()).stream().
                map(UserRole::getRoleId).
                collect(Collectors.toList());
        Integer dacUserId = dacUser.getDacUserId();
        List<Election> elections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.TRANSLATE_DUL.getValue(), ElectionStatus.OPEN.getValue());
        List<PendingCase> pendingCases = filterElectionsByDAC(elections, authUser).
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

    private List<Election> filterElectionsByDAC(List<Election> elections, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return elections;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> dataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).
                stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());
        return elections.stream().
                filter(e -> dataSetIds.contains(e.getDataSetId())).
                collect(Collectors.toList());
    }

    private List<ConsentManage> filterConsentManageByDAC(List<ConsentManage> consentManages, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return consentManages;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consentManages.stream().
                filter(c -> c.getDacId() != null).
                filter(c -> dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    private List<ConsentManage> collectUnreviewedConsents(List<Consent> consents) {
        String UNREVIEWED = "un-reviewed"; // TODO: Should this be a real `ElectionStatus` enum? See what that could impact elsewhere.
        List<ConsentManage> consentManageList = consents.stream().map(ConsentManage::new).collect(Collectors.toList());
        consentManageList.forEach(c -> c.setElectionStatus(UNREVIEWED));
        consentManageList.forEach(c -> c.setEditable(true));
        return consentManageList;
    }

    private Collection<Consent> filterConsentsByDAC(Collection<Consent> consents, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return consents;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consents.stream().
                filter(c -> c.getDacId() != null).
                filter(c -> dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    private boolean isAuthUserAdmin(AuthUser authUser) {
        return userRoleDAO.findRolesByUserEmail(authUser.getName()).stream().
                anyMatch(ur -> ur.getRoleId().equals(UserRoles.ADMIN.getRoleId()));
    }

    private List<Integer> getDacIdsForUser(AuthUser authUser) {
        return dacDAO.findDacsForEmail(authUser.getName())
                .stream()
                .map(Dac::getDacId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
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

}
