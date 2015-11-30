package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteStatus;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseElectionCaseAPI extends AbstractPendingCaseAPI {

    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private DACUserRoleDAO rolesDAO;
    private DACUserDAO userDAO;
    private ConsentDAO consentDAO;

    private final MongoConsentDB mongo;

    public static void initInstance(ElectionDAO electionDAO, VoteDAO voteDAO, DACUserDAO userDAO, DACUserRoleDAO rolesDAO, ConsentDAO consentDAO ,MongoConsentDB mongoDB) {
        PendingCaseAPIHolder.setInstance(new DatabaseElectionCaseAPI(electionDAO, voteDAO, userDAO, rolesDAO, consentDAO, mongoDB));

    }

    private DatabaseElectionCaseAPI(ElectionDAO electionDAO, VoteDAO voteDAO, DACUserDAO userDAO, DACUserRoleDAO rolesDAO, ConsentDAO consentDAO, MongoConsentDB mongoDB) {
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.userDAO = userDAO;
        this.rolesDAO = rolesDAO;
        this.consentDAO = consentDAO;
        this.mongo = mongoDB;
    }

    @Override
    public List<PendingCase> describeConsentPendingCases(Integer dacUserId) throws NotFoundException {
        String type = electionDAO.findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue());
        List<Election> elections = electionDAO.findElectionsByTypeAndStatus(type, ElectionStatus.OPEN.getValue());
        List<PendingCase> pendingCases = new ArrayList<>();
        if (elections != null) {
            for (Election election : elections) {
                Vote vote = voteDAO.findVoteByElectionIdAndDACUserId(election.getElectionId(),
                        dacUserId);
                if (vote == null) {
                    continue;
                }
                PendingCase pendingCase = setGeneralFields(election, vote, vote.isReminderSent());
                pendingCases.add(pendingCase);
            }
        }
        if (userDAO.findDACUserById(dacUserId) != null) {
            List<DACUserRole> roles = rolesDAO.findRolesByUserId(dacUserId);
            if (roles.contains(new DACUserRole(0, "Chairperson"))) {
                return orderPendingCasesForChairperson(pendingCases);
            } else {
                return orderPendingCasesForMember(pendingCases);
            }
        }
        return pendingCases;
    }



    @Override
    public List<PendingCase> describeDataRequestPendingCases(Integer dacUserId) throws NotFoundException {
        String type = electionDAO.findElectionTypeByType(ElectionType.DATA_ACCESS.getValue());
        List<Election> elections;
        elections = isChairPerson(dacUserId)  ?  electionDAO.findElectionsByTypeAndFinalAccessVoteChairPerson(type,false) : electionDAO.findElectionsByTypeAndStatus(type, ElectionStatus.OPEN.getValue());
        List<PendingCase> pendingCases = new ArrayList<>();
        if (elections != null) {
            for (Election election : elections) {
                Vote accessVote = voteDAO.findVoteByElectionIdAndDACUserId(election.getElectionId(),
                        dacUserId);
                if (accessVote == null) {
                    continue;
                }
                Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(election.getElectionId());
                Election rpElection = electionDAO.findElectionById(rpElectionId);
                Vote rpVote = voteDAO.findVoteByElectionIdAndDACUserId(rpElectionId, dacUserId);
                Boolean isReminderSent = (accessVote.isReminderSent() || rpVote.isReminderSent()) ? true : false;
                PendingCase pendingCase = setGeneralFields(election, accessVote, isReminderSent);
                pendingCase.setRpElectionId(rpElectionId);
                pendingCase.setAlreadyVoted(accessVote.getVote() != null && rpVote.getVote() != null);
                pendingCase.setElectionStatus(rpElection.getStatus().equals(ElectionStatus.FINAL.getValue()) && election.getStatus().equals(ElectionStatus.FINAL.getValue()) ? ElectionStatus.FINAL.getValue() : ElectionStatus.OPEN.getValue());                 // if it's already voted, we should collect vote or do the final election vote
                // it depends if the chairperson vote was done after collect votes
                setFinalVote(dacUserId, election, pendingCase);
                pendingCase.setStatus(accessVote.getVote() == null || rpVote.getVote() == null ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());
                pendingCase.setRpVoteId(rpVote.getVoteId());
                pendingCases.add(pendingCase);
            }
        }
        return pendingCases;
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

    private PendingCase setGeneralFields(Election election, Vote vote, boolean isReminderSent) {
        String type = electionDAO.findElectionTypeByType(ElectionType.DATA_ACCESS.getValue());
        PendingCase pendingCase = new PendingCase();
        List<Vote> votes = voteDAO.findDACVotesByElectionId(election.getElectionId());
        List<Vote> pendingVotes = voteDAO.findPendingDACVotesByElectionId(election.getElectionId());
        pendingCase.setTotalVotes(votes.size());
        pendingCase.setVotesLogged(votes.size() - pendingVotes.size());
        pendingCase.setReferenceId(election.getReferenceId());
        if (election.getElectionType().equals(type)) {
            BasicDBObject query = new BasicDBObject().append("_id", new ObjectId(election.getReferenceId()));
            FindIterable<Document> dataAccessRequest = mongo.getDataAccessRequestCollection().find(query);
            pendingCase.setFrontEndId(dataAccessRequest.first() != null ?  dataAccessRequest.first().get("dar_code").toString() : null);
        }else{
             pendingCase.setFrontEndId(consentDAO.findConsentById(election.getReferenceId()).getName());
        }
            pendingCase.setLogged(setLogged(pendingCase.getTotalVotes(), pendingCase.getVotesLogged()));
            pendingCase.setAlreadyVoted(vote.getVote() != null);
            pendingCase.setStatus(vote.getVote() == null ? VoteStatus.PENDING.getValue() : VoteStatus.EDITABLE.getValue());
            pendingCase.setVoteId(vote.getVoteId());
            pendingCase.setIsReminderSent(isReminderSent);
            pendingCase.setCreateDate(election.getCreateDate());
            pendingCase.setElectionStatus(election.getStatus());
            pendingCase.setElectionId(election.getElectionId());
            return pendingCase;
        }


    private String setLogged(Integer totalVotes, Integer loggedVotes) {
        StringBuilder logged = new StringBuilder();
        logged.append(loggedVotes)
                .append("/")
                .append(totalVotes);
        return logged.toString();
    }

    private List<PendingCase> orderPendingCasesForMember(List<PendingCase> cases) {
        List<PendingCase> memberCaseList = new ArrayList<>();
        memberCaseList.addAll(cases.stream().filter(p1 -> p1.getIsReminderSent() == true).collect(Collectors.toList()));
        memberCaseList.addAll(cases.stream().filter(p1 -> (p1.getAlreadyVoted() == false) && (p1.getIsReminderSent() == false)).collect(Collectors.toList()));
        memberCaseList.addAll(cases.stream().filter(p1 -> p1.getAlreadyVoted() == true).collect(Collectors.toList()));
        return memberCaseList;
    }

    private List<PendingCase> orderPendingCasesForChairperson(List<PendingCase> cases) {
        HashMap<Integer, Queue<PendingCase>> pendingCases = new HashMap<>();
        List<PendingCase> chairFinalList = cases.stream().filter(p1 -> p1.getTotalVotes() == p1.getVotesLogged()).collect(Collectors.toList());
        List<PendingCase> remainingList = cases.stream().filter(p1 -> p1.getTotalVotes() != p1.getVotesLogged()).collect(Collectors.toList());
        for (PendingCase p : remainingList) {
            Integer key = Integer.parseInt(String.valueOf(p.getVotesLogged()) + String.valueOf(p.getTotalVotes()));
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
        readyToCollect.addAll(prevList.stream().filter(p1 -> p1.getAlreadyVoted()).collect(Collectors.toList()));
        readyToCollect.addAll(prevList.stream().filter(p1 -> p1.getAlreadyVoted() == false).collect(Collectors.toList()));
        return readyToCollect;
    }

    private List<Integer> orderListDecreased(Set<Integer> setOfKeys) {
        return setOfKeys.stream().sorted((i1, i2) -> i2.compareTo(i1)).collect(Collectors.toList());
    }

    private List<PendingCase> orderedVotedCases(List<Integer> keys, HashMap<Integer, Queue<PendingCase>> pendingCases){
        ArrayList<PendingCase> votedCases = new ArrayList<>();
        for(Integer key: keys){
            votedCases.addAll(pendingCases.get(key));
        }
        return votedCases;
    }

    private boolean isChairPerson(Integer dacUserId) {
        boolean isCherperson = false;
        if (userDAO.checkChairpersonUser(dacUserId) != null) {
            isCherperson = true;
        }
        return isCherperson;
    }


}