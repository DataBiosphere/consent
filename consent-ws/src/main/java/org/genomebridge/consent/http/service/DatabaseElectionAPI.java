package org.genomebridge.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.genomebridge.consent.http.db.*;
import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.enumeration.DACUserRoles;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.enumeration.VoteType;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation class for ElectionAPI on top of ElectionDAO database support.
 */
public class DatabaseElectionAPI extends AbstractElectionAPI {

    private MailMessageDAO mailMessageDAO;
    private ElectionDAO electionDAO;
    private ConsentDAO consentDAO;
    private DataRequestDAO dataRequestDAO;
    private VoteDAO voteDAO;
    private DACUserDAO dacUserDAO;
    private MongoConsentDB mongo;


    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param dao The Data Access Object instance that the API should use to
     *            read/write data.
     */
    public static void initInstance(ElectionDAO dao, ConsentDAO consentDAO, DataRequestDAO dataRequestDAO, DACUserDAO dacUserDAO, MongoConsentDB mongo, VoteDAO voteDAO, MailMessageDAO mailMessageDAO) {
        ElectionAPIHolder.setInstance(new DatabaseElectionAPI(dao, consentDAO, dataRequestDAO, dacUserDAO, mongo, voteDAO, mailMessageDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseElectionAPI(ElectionDAO dao, ConsentDAO consentDAO, DataRequestDAO dataRequestDAO, DACUserDAO dacUserDAO, MongoConsentDB mongo, VoteDAO voteDAO, MailMessageDAO mailMessageDAO) {
        this.electionDAO = dao;
        this.consentDAO = consentDAO;
        this.dataRequestDAO = dataRequestDAO;
        this.dacUserDAO = dacUserDAO;
        this.mongo = mongo;
        this.voteDAO = voteDAO;
        this.mailMessageDAO = mailMessageDAO;
    }

    public void setMongoDBInstance(MongoConsentDB mongo) {
        this.mongo = mongo;
    }

    @Override
    public Election createElection(Election election, String referenceId, ElectionType electionType) throws
            IllegalArgumentException {
        validateAvailableUsers();
        validateReferenceId(referenceId, electionType);
        validateExistentElection(referenceId, electionType);
        validateStatus(election.getStatus());
        setGeneralFields(election, referenceId, electionType);
        Date createDate = new Date();
        Integer id = electionDAO.insertElection(election.getElectionType(), election.getStatus(),
                createDate, election.getReferenceId(), election.getFinalAccessVote());
        updateSortDate(referenceId, createDate);
        if (electionType.equals(ElectionType.RP)) {
            Election access = describeDataRequestElection(referenceId);
            electionDAO.insertAccessRP(access.getElectionId(), id);
        }
        return electionDAO.findElectionById(id);
    }


    @Override
    public Election updateElectionById(Election rec, Integer electionId) {
        validateStatus(rec.getStatus());
        if (rec.getStatus() == null) {
            rec.setStatus(ElectionStatus.OPEN.getValue());
        } else if (rec.getStatus().equals(ElectionStatus.CLOSED.getValue()) || rec.getStatus().equals(ElectionStatus.FINAL.getValue())) {
            rec.setFinalVoteDate(new Date());
        }
        updateFinalVote(rec, electionId);
        Election election = electionDAO.findElectionById(electionId);
        if (election == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        if (rec.getStatus().equals(ElectionStatus.CANCELED.getValue()) || rec.getStatus().equals(ElectionStatus.CLOSED.getValue())) {
            updateAccessElection(electionId, election.getElectionType(), rec.getStatus());
        }
        Date lastUpdate = new Date();
        electionDAO.updateElectionById(electionId, rec.getStatus(), lastUpdate);
        updateSortDate(electionDAO.findElectionById(electionId).getReferenceId(), lastUpdate);
        return electionDAO.findElectionById(electionId);
    }

    @Override
    public Election updateFinalAccessVoteDataRequestElection(Integer electionId) {
        if (electionDAO.findElectionById(electionId) == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        electionDAO.updateFinalAccessVote(electionId);
        return electionDAO.findElectionById(electionId);
    }

    @Override
    public Election describeConsentElection(String consentId) {
        if (consentDAO.checkConsentbyId(consentId) == null) {
            throw new NotFoundException("Invalid ConsentId");
        }
        Election election = electionDAO.getOpenElectionByReferenceIdAndType(consentId, ElectionType.TRANSLATE_DUL.getValue());
        if (election == null) {
            throw new NotFoundException("Election was not found");
        }
        return election;
    }

    @Override
    public List<Election> describeClosedElectionsByType(String type) {

        List<Election> elections;
        if (type.equals("1")) {
            elections = electionDAO.findRequestElectionsWithFinalVoteByStatus(ElectionStatus.CLOSED.getValue());
            List<String> referenceIds = elections.stream().map(election -> election.getReferenceId()).collect(Collectors.toList());
            ObjectId[] objarray = new ObjectId[referenceIds.size()];
            for (int i = 0; i < referenceIds.size(); i++)
                objarray[i] = new ObjectId(referenceIds.get(i));
            BasicDBObject in = new BasicDBObject("$in", objarray);
            BasicDBObject q = new BasicDBObject("_id", in);
            FindIterable<Document> dataAccessRequests =  mongo.getDataAccessRequestCollection().find(q);
            elections.forEach(election -> {
                MongoCursor<Document> itr = dataAccessRequests.iterator();
                try {
                    while (itr.hasNext()) {
                        Document next = itr.next();
                        if (next.get("_id").toString().equals(election.getReferenceId())) {
                            election.setReferenceId(next.get("dar_code").toString());
                        }
                    }
                } finally {
                    itr.close();
                }
            });
        }else {
            elections = electionDAO.findElectionsByTypeAndStatus(type, ElectionStatus.CLOSED.getValue());
            if(!elections.isEmpty()){
                List<String> consentIds = elections.stream().map(election -> election.getReferenceId()).collect(Collectors.toList());
                Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(consentIds);
                elections.forEach(election -> {
                    List<Consent> c = consents.stream().filter(cs -> cs.getConsentId().equals(election.getReferenceId())).
                            collect(Collectors.toList());
                    election.setReferenceId(c.get(0).getName());
                });
            }
        }

        if (elections == null) {
            throw new NotFoundException("Couldn't find any closed elections");
        }
        return elections;
    }

    @Override
    public void deleteElection(String referenceId, Integer id) {
        if (electionDAO.findElectionsByReferenceId(referenceId) == null) {
            throw new IllegalArgumentException("Does not exist an election for the specified id");
        }
        Election election = electionDAO.findElectionById(id);
        if (election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())) {
            Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(election.getElectionId());
            electionDAO.deleteAccessRP(id);
            electionDAO.deleteElectionById(rpElectionId);
        }
        electionDAO.deleteElectionById(id);
    }

    @Override
    public Election describeDataRequestElection(String requestId) {
        Election election = electionDAO.getOpenElectionByReferenceIdAndType(requestId, ElectionType.DATA_ACCESS.getValue());
        if (election == null) {
            election = electionDAO.getOpenElectionByReferenceIdAndType(requestId, ElectionType.RP.getValue());
        }
        if (election == null) {
            throw new NotFoundException();
        }
        return election;
    }

    @Override
    public List<Election> cancelOpenElectionAndReopen() {
        String electionTypeId = electionDAO.findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue());
        List<Election> openElections = electionDAO.findElectionsByTypeAndStatus(electionTypeId, ElectionStatus.OPEN.getValue());
        cancelOpenElection(electionTypeId);
        List<Integer> electionIds = openElections.stream().map(election -> election.getElectionId()).collect(Collectors.toList());
        List<String> consentIds = openElections.stream().map(election -> election.getReferenceId()).collect(Collectors.toList());
        Date sortDate = new Date();
        consentDAO.bulkUpdateConsentSortDate(consentIds, sortDate, sortDate);
        electionDAO.bulkUpdateElectionLastUpdate(electionIds, sortDate);
        return openElections(openElections);
    }

    @Override
    public Election describeElectionById(Integer electionId) {
        return electionDAO.findElectionById(electionId);
    }

    private void cancelOpenElection(String electionTypeId) {
        List<Integer> openElectionsIds = electionDAO.findElectionsIdByTypeAndStatus(electionTypeId, ElectionStatus.OPEN.getValue());
        if (openElectionsIds != null && openElectionsIds.size() > 0) {
            electionDAO.updateElectionStatus(openElectionsIds, ElectionStatus.CANCELED.getValue());
        }
    }

    @Override
    public Integer findRPElectionByElectionAccessId(Integer electionId) {
        return electionDAO.findRPElectionByElectionAccessId(electionId);
    }


    private List<Election> openElections(List<Election> openElections) {
        List<Election> elections = new ArrayList<>();
        for (Election existentElection : openElections) {
            Election election = new Election();
            election.setReferenceId(existentElection.getReferenceId());
            elections.add(createElection(election, election.getReferenceId(), ElectionType.TRANSLATE_DUL));
        }
        return elections;
    }

    private void setGeneralFields(Election election, String referenceId, ElectionType electionType) {
        election.setCreateDate(new Date());
        election.setReferenceId(referenceId);
        switch (electionType) {
            case TRANSLATE_DUL:
                election.setElectionType(electionDAO
                        .findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue()));
                break;
            case DATA_ACCESS:
                election.setElectionType(electionDAO
                        .findElectionTypeByType(ElectionType.DATA_ACCESS.getValue()));
                break;
            case RP:
                election.setElectionType(electionDAO
                        .findElectionTypeByType(ElectionType.RP.getValue()));
                break;
        }
        if (StringUtils.isEmpty(election.getStatus())) {
            election.setStatus(ElectionStatus.OPEN.getValue());
        }
    }

    private void validateReferenceId(String referenceId, ElectionType electionType) {
        if (electionType.equals(ElectionType.TRANSLATE_DUL)) {
            validateConsentId(referenceId);
        } else {
            validateDataRequestId(referenceId);
        }
    }

    private void validateDataRequestId(String dataRequest) {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(dataRequest));
        Document dar = mongo.getDataAccessRequestCollection().find(query).first();
        if (dataRequest != null && dar == null) {
            throw new NotFoundException("Invalid id: " + dataRequest);
        }
    }

    private void validateExistentElection(String referenceId, ElectionType type) {
        Election election = electionDAO.getOpenElectionByReferenceIdAndType(referenceId, type.getValue());
        if (election != null) {
            throw new IllegalArgumentException(
                    "An open election already exists for the specified id. Election id: "
                            + election.getElectionId());
        }
    }

    private void validateConsentId(String referenceId) {
        if (referenceId == null || consentDAO.checkConsentbyId(referenceId) == null) {
            throw new IllegalArgumentException("Invalid id: " + referenceId);
        }
    }

    private void validateStatus(String status) {
        if (StringUtils.isNotEmpty(status)) {
            if (ElectionStatus.getValue(status) == null) {
                throw new IllegalArgumentException(
                        "Invalid value. Valid status are: " + ElectionStatus.getValues());
            }
        }
    }

    private void validateAvailableUsers() {
        Set<DACUser> dacUsers = dacUserDAO.findDACUsersEnabledToVote();
        if (dacUsers != null && dacUsers.size() >= 4) {
            boolean existChairperson = false;
            for (DACUser user : dacUsers) {
                if (user.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(DACUserRoles.CHAIRPERSON.getValue()))) {
                    existChairperson = true;
                    break;
                }
            }
            if (!existChairperson) {
                throw new IllegalArgumentException("There has to be a Chairperson.");
            }
        } else {
            throw new IllegalArgumentException(
                    "There has to be a Chairperson and at least 4 Members cataloged in the system to create an election.");
        }
    }

    /*
        This is true if the last remaining vote is the chairperson vote, or everyone has already voted.
     */
    @Override
    public boolean validateCollectEmailCondition(Vote vote){
        List<Vote> votes = voteDAO.findPendingDACVotesByElectionId(vote.getElectionId());
        DACUser chairperson = dacUserDAO.findChairpersonUser();
        if((votes.size() == 0) &&(vote.getDacUserId() != chairperson.getDacUserId())){
            return true;
        } else if((votes.size() == 1)) {
            Vote chairVote = voteDAO.findVoteByElectionIdAndDACUserId(vote.getElectionId(), chairperson.getDacUserId());
            if(chairVote.getCreateDate() == null){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean validateCollectDAREmailCondition(Vote vote){
        Election e = electionDAO.findElectionById(vote.getElectionId());
        Integer rpElectionId, darElectionId;
        if(e.getElectionType().equals(ElectionType.RP.getValue())){
            rpElectionId = e.getElectionId();
            darElectionId = electionDAO.findAccessElectionByElectionRPId(rpElectionId);
        } else {
            darElectionId = e.getElectionId();
            rpElectionId = electionDAO.findRPElectionByElectionAccessId(darElectionId);
        }
        List<Vote> rpElectionVotes = voteDAO.findPendingDACVotesByElectionId(rpElectionId);
        List<Vote> darVotes = voteDAO.findPendingDACVotesByElectionId(darElectionId);
        DACUser chairperson = dacUserDAO.findChairpersonUser();
        if((mailMessageDAO.existsCollectDAREmail(darElectionId, rpElectionId) == null)){
            if(((darVotes.size()==0) && (rpElectionVotes.size() == 0) && (vote.getDacUserId() != chairperson.getDacUserId()))){
                return true;
            } else {
                Vote rpChairVote = voteDAO.findVoteByElectionIdAndDACUserId(rpElectionId, chairperson.getDacUserId());
                Vote darChairVote = voteDAO.findVoteByElectionIdAndDACUserId(vote.getElectionId(), chairperson.getDacUserId());
                if ((((rpElectionVotes.size() == 1) && (CollectionUtils.isEmpty(darVotes))))) {
                    if (rpChairVote.getCreateDate() == null) {
                        return true;
                    }
                } else {
                    if ((((darVotes.size() == 1) && (CollectionUtils.isEmpty(rpElectionVotes))))) {
                        if (darChairVote.getCreateDate() == null) {
                            return true;
                        }
                    } else {
                        if ((((darVotes.size() == 1) && (rpElectionVotes.size() == 1)))) {
                            if ((darChairVote.getCreateDate() == null) && (rpChairVote.getCreateDate() == null)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    private void updateSortDate(String referenceId, Date createDate){
        if(consentDAO.checkConsentbyId(referenceId) != null){
            consentDAO.updateConsentSortDate(referenceId, createDate);
        } else {
            BasicDBObject query = new BasicDBObject("_id", new ObjectId(referenceId));
            Document dar = mongo.getDataAccessRequestCollection().find(query).first();
            dar.put("sortDate", createDate);
            mongo.getDataAccessRequestCollection().findOneAndReplace(query, dar);
        }
    }

    private void updateAccessElection(Integer electionId, String type, String status) {
        List<Integer> ids = new ArrayList<>();
        if (type.equals(ElectionType.DATA_ACCESS.getValue())) {
            ids.add(electionDAO.findRPElectionByElectionAccessId(electionId));
        } else if (type.equals(ElectionType.RP.getValue())) {
            ids.add(electionDAO.findAccessElectionByElectionRPId(electionId));
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            electionDAO.updateElectionStatus(ids, status);
        }
    }


    private void updateFinalVote(Election rec, Integer electionId) {
        if (rec.getFinalVote() != null) {
            Vote vote = voteDAO.findVoteByElectionIdAndType(electionId, VoteType.CHAIRPERSON.getValue());
            vote.setVote(rec.getFinalVote());
            vote.setCreateDate(rec.getFinalVoteDate());
            vote.setRationale(rec.getFinalRationale());
            voteDAO.updateVote(vote.getVote(), vote.getRationale(), vote.getUpdateDate(), vote.getVoteId(), vote.isReminderSent(), vote.getElectionId(), vote.getCreateDate());
        }
    }
}
