package org.genomebridge.consent.http.service;

import com.mongodb.BasicDBObject;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.DataRequestDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.enumeration.DACUserRoles;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.Election;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation class for ElectionAPI on top of ElectionDAO database support.
 */
public class DatabaseElectionAPI extends AbstractElectionAPI {

    private ElectionDAO electionDAO;
    private ConsentDAO consentDAO;
    private DataRequestDAO dataRequestDAO;
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
    public static void initInstance(ElectionDAO dao, ConsentDAO consentDAO, DataRequestDAO dataRequestDAO, DACUserDAO dacUserDAO, MongoConsentDB mongo) {
        ElectionAPIHolder.setInstance(new DatabaseElectionAPI(dao, consentDAO, dataRequestDAO, dacUserDAO, mongo));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseElectionAPI(ElectionDAO dao, ConsentDAO consentDAO, DataRequestDAO dataRequestDAO, DACUserDAO dacUserDAO, MongoConsentDB mongo) {
        this.electionDAO = dao;
        this.consentDAO = consentDAO;
        this.dataRequestDAO = dataRequestDAO;
        this.dacUserDAO = dacUserDAO;
        this.mongo = mongo;
    }

    public void setMongoDBInstance(MongoConsentDB mongo){
        this.mongo = mongo;
    }

    @Override
    public Election createElection(Election election, String referenceId, Boolean isConsent) throws
            IllegalArgumentException {
        validateAvailableUsers();
        validateReferenceId(referenceId, isConsent);
        validateExistentElection(referenceId);
        validateStatus(election.getStatus());
        setGeneralFields(election, referenceId, isConsent);
        Date createDate = new Date();
        Integer id = electionDAO.insertElection(election.getElectionType(),
                election.getFinalVote(), election.getFinalRationale(), election.getStatus(),
                createDate, election.getReferenceId());
        consentDAO.updateConsentSortDate(referenceId, createDate);
        return electionDAO.findElectionById(id);
    }

    @Override
    public Election updateElectionById(Election rec, Integer electionId) {
        validateStatus(rec.getStatus());
        if (rec.getStatus() == null) {
            rec.setStatus(ElectionStatus.OPEN.getValue());
        } else if(rec.getStatus().equals(ElectionStatus.CLOSED.getValue())){
            rec.setFinalVoteDate(new Date());
        }
        if (electionDAO.findElectionById(electionId) == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        Date lastUpdate = new Date();
        electionDAO.updateElectionById(electionId, rec.getFinalVote(), rec.getFinalVoteDate(), rec.getFinalRationale(), rec.getStatus(), lastUpdate);
        consentDAO.updateConsentSortDate(electionDAO.findElectionById(electionId).getReferenceId(), lastUpdate);
        return electionDAO.findElectionById(electionId);
    }


    @Override
    public Election updateFinalAccessVoteDataRequestElection(Integer electionId){
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
        Election election = electionDAO.getOpenElectionByReferenceId(consentId);
        if (election == null) {
            throw new NotFoundException("Election was not found");
        }
        return election;
    }

    @Override
    public List<Election> describeClosedElectionsByType(String type) {
        List<Election> elections = type.equals("2") ? electionDAO.findElectionsByTypeAndStatus(type, ElectionStatus.CLOSED.getValue())
                                                    : electionDAO.findRequestElectionsWithFinalVoteByStatus(ElectionStatus.CLOSED.getValue());
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
        consentDAO.updateConsentSortDate(referenceId, new Date());
        electionDAO.deleteElectionById(id);
    }

    @Override
    public Election describeDataRequestElection(String requestId) {
        Election election = electionDAO.getOpenElectionByReferenceId(requestId);
        if (election == null) {
            throw new NotFoundException("Election was not found");
        }
        return election;
    }

    @Override
    public List<Election> cancelOpenElectionAndReopen(){
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
    public Election describeElectionById(Integer electionId){
        return electionDAO.findElectionById(electionId);
    }

    private void cancelOpenElection(String electionTypeId){
        List<Integer> openElectionsIds = electionDAO.findElectionsIdByTypeAndStatus(electionTypeId, ElectionStatus.OPEN.getValue());
        if(openElectionsIds != null && openElectionsIds.size() > 0){
            electionDAO.updateElectionStatus(openElectionsIds, ElectionStatus.CANCELED.getValue());
        }
    }

    private List<Election> openElections(List<Election> openElections){
        List<Election> elections = new ArrayList<>();
        for(Election existentElection : openElections){
            Election election = new Election();
            election.setReferenceId(existentElection.getReferenceId());
            elections.add(createElection(election, election.getReferenceId(), true));
        }
        return elections;
    }

    private void setGeneralFields(Election election, String referenceId, Boolean isConsent) {
        election.setCreateDate(new Date());
        election.setReferenceId(referenceId);
        if (isConsent) {
            election.setElectionType(electionDAO
                    .findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue()));
        } else {
            election.setElectionType(electionDAO
                    .findElectionTypeByType(ElectionType.DATA_ACCESS.getValue()));
        }
        if (StringUtils.isEmpty(election.getStatus())) {
            election.setStatus(ElectionStatus.OPEN.getValue());
        }
    }

    private void validateReferenceId(String referenceId, Boolean isConsent) {
        if (isConsent) {
            validateConsentId(referenceId);
        } else {
            validateDataRequestId(referenceId);
        }
    }

    private void validateDataRequestId(String dataRequest) {
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(dataRequest));
        Document dar = mongo.getDataAccessRequestCollection().find(query).first();
        if (dataRequest != null && dar == null ) {
            throw new NotFoundException("Invalid id: " + dataRequest);
        }
    }

    private void validateExistentElection(String referenceId) {
        Election election = electionDAO.getOpenElectionByReferenceId(referenceId);
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
        if(dacUsers != null && dacUsers.size() >= 4){
            boolean existChairperson = false;
            for(DACUser user : dacUsers) {
                if(user.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(DACUserRoles.CHAIRPERSON.getValue()))){
                    existChairperson = true;
                    break;
                }
            }
            if(!existChairperson){
                throw new IllegalArgumentException("There has to be a Chairperson.");
            }
        }else{
            throw new IllegalArgumentException(
                    "There has to be a Chairperson and at least 4 Members cataloged in the system to create an election.");
        }
    }
}
