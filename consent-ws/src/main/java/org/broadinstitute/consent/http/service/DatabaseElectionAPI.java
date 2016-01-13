package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation class for ElectionAPI on top of ElectionDAO database support.
 */
public class DatabaseElectionAPI extends AbstractElectionAPI {

    private MailMessageDAO mailMessageDAO;
    private ElectionDAO electionDAO;
    private ConsentDAO consentDAO;
    private VoteDAO voteDAO;
    private DACUserDAO dacUserDAO;
    private MongoConsentDB mongo;
    private DataSetDAO dataSetDAO;
    private final String DUL_NOT_APROVED = "The Data Use Limitation Election related to this Dataset has not been approved yet.";
    private final String INACTIVE_DS = "Election was not created. The following DataSets are disabled : ";
    private EmailNotifierAPI emailNotifierAPI;
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
    public static void initInstance(ElectionDAO dao, ConsentDAO consentDAO, DACUserDAO dacUserDAO, MongoConsentDB mongo, VoteDAO voteDAO, MailMessageDAO mailMessageDAO, DataSetDAO dataSetDAO) {
        ElectionAPIHolder.setInstance(new DatabaseElectionAPI(dao, consentDAO, dacUserDAO, mongo, voteDAO, mailMessageDAO, dataSetDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseElectionAPI(ElectionDAO dao, ConsentDAO consentDAO, DACUserDAO dacUserDAO, MongoConsentDB mongo, VoteDAO voteDAO, MailMessageDAO mailMessageDAO, DataSetDAO dataSetDAO) {
        this.electionDAO = dao;
        this.consentDAO = consentDAO;
        this.dacUserDAO = dacUserDAO;
        this.mongo = mongo;
        this.voteDAO = voteDAO;
        this.mailMessageDAO = mailMessageDAO;
        this.dataSetDAO = dataSetDAO;
        this.emailNotifierAPI = AbstractEmailNotifierAPI.getInstance();
    }

    public void setMongoDBInstance(MongoConsentDB mongo) {
        this.mongo = mongo;
    }

    @Override
    public Election createElection(Election election, String referenceId, ElectionType electionType) throws Exception {
        validateElectionIsValid(referenceId, electionType);
        validateAvailableUsers(electionType);
        validateReferenceId(referenceId, electionType);
        validateExistentElection(referenceId, electionType);
        validateStatus(election.getStatus());
        setGeneralFields(election, referenceId, electionType);
        Date createDate = new Date();
        Integer id = electionDAO.insertElection(election.getElectionType(), election.getStatus(),
                createDate, election.getReferenceId(), election.getFinalAccessVote() , Objects.toString(election.getUseRestriction(), "") , election.getTranslatedUseRestriction());
        updateSortDate(referenceId, createDate);
        if(electionType.equals(ElectionType.RP)) {
            Election access = describeDataRequestElection(referenceId);
            electionDAO.insertAccessRP(access.getElectionId(), id);
        }
        return electionDAO.findElectionWithFinalVoteById(id);
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
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if (election == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        if (rec.getStatus().equals(ElectionStatus.CANCELED.getValue()) || rec.getStatus().equals(ElectionStatus.CLOSED.getValue())) {
            updateAccessElection(electionId, election.getElectionType(), rec.getStatus());
        }
        Date lastUpdate = new Date();
        electionDAO.updateElectionById(electionId, rec.getStatus(), lastUpdate);
        updateSortDate(electionDAO.findElectionWithFinalVoteById(electionId).getReferenceId(), lastUpdate);
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    @Override
    public Election updateFinalAccessVoteDataRequestElection(Integer electionId) {
        if (electionDAO.findElectionWithFinalVoteById(electionId) == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        electionDAO.updateFinalAccessVote(electionId);
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    @Override
    public Election describeConsentElection(String consentId) {
        if (consentDAO.checkConsentbyId(consentId) == null) {
            throw new NotFoundException("Invalid ConsentId");
        }
        Election election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(consentId, ElectionType.TRANSLATE_DUL.getValue());
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
            BasicDBObject q = new BasicDBObject(DarConstants.ID, in);
            FindIterable<Document> dataAccessRequests =  mongo.getDataAccessRequestCollection().find(q);
            elections.forEach(election -> {
                MongoCursor<Document> itr = dataAccessRequests.iterator();
                try {
                    while (itr.hasNext()) {
                        Document next = itr.next();
                        if (next.get(DarConstants.ID).toString().equals(election.getReferenceId())) {
                            election.setReferenceId(next.get(DarConstants.DAR_CODE).toString());
                        }
                    }
                } finally {
                    itr.close();
                }
            });
        }else {
            elections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(type, ElectionStatus.CLOSED.getValue());
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
        if (electionDAO.
                findElectionsWithFinalVoteByReferenceId(referenceId) == null) {
            throw new IllegalArgumentException("Does not exist an election for the specified id");
        }
        Election election = electionDAO.findElectionWithFinalVoteById(id);
        if (election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())) {
            Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(election.getElectionId());
            electionDAO.deleteAccessRP(id);
            electionDAO.deleteElectionById(rpElectionId);
        }
        electionDAO.deleteElectionById(id);
    }

    @Override
    public Election describeDataRequestElection(String requestId) {
        Election election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(requestId, ElectionType.DATA_ACCESS.getValue());
        if (election == null) {
            election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(requestId, ElectionType.RP.getValue());
        }
        if (election == null) {
            throw new NotFoundException();
        }
        return election;
    }

    @Override
    public Election describeElectionById(Integer electionId) {
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    @Override
    public List<Election> cancelOpenElectionAndReopen() throws Exception{
        String electionTypeId = electionDAO.findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue());
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(electionTypeId, ElectionStatus.OPEN.getValue());
        cancelOpenElection(electionTypeId);
        List<Integer> electionIds = openElections.stream().map(election -> election.getElectionId()).collect(Collectors.toList());
        List<String> consentIds = openElections.stream().map(election -> election.getReferenceId()).collect(Collectors.toList());
        Date sortDate = new Date();
        if(CollectionUtils.isNotEmpty(electionIds)){
            consentDAO.bulkUpdateConsentSortDate(consentIds, sortDate, sortDate);
            electionDAO.bulkUpdateElectionLastUpdate(electionIds, sortDate);
        }
        return openElections(openElections);
    }

    @Override
    public Integer findRPElectionByElectionAccessId(Integer electionId) {
        return electionDAO.findRPElectionByElectionAccessId(electionId);
    }

    /*
     This is true if the last remaining vote is the chairperson vote, or everyone has already voted.
  */
    @Override
    public boolean validateCollectEmailCondition(Vote vote){
        List<Vote> votes = voteDAO.findPendingVotesByElectionId(vote.getElectionId());
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
        Election e = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
        Integer rpElectionId, darElectionId;
        if(e.getElectionType().equals(ElectionType.RP.getValue())){
            rpElectionId = e.getElectionId();
            darElectionId = electionDAO.findAccessElectionByElectionRPId(rpElectionId);
        } else {
            darElectionId = e.getElectionId();
            rpElectionId = electionDAO.findRPElectionByElectionAccessId(darElectionId);
        }
        List<Vote> rpElectionVotes = voteDAO.findPendingVotesByElectionId(rpElectionId);
        List<Vote> darVotes = voteDAO.findPendingVotesByElectionId(darElectionId);
        DACUser chairperson = dacUserDAO.findChairpersonUser();
        Integer exists = mailMessageDAO.existsCollectDAREmail(darElectionId, rpElectionId);
        if((exists == null)){
            if(((darVotes.size()==0) && (rpElectionVotes.size() == 0) && (!vote.getDacUserId().equals(chairperson.getDacUserId())))){
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

    @Override
    public void closeDataOwnerApprovalElection(Integer electionId){
        Election election = electionDAO.findElectionById(electionId);
        List<Vote> dataOwnersVote = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), VoteType.DATA_OWNER.getValue());
        List<Vote> rejectedVotes = dataOwnersVote.stream().filter(dov -> (dov.getVote() != null && !dov.getVote()) || (dov.getHasConcerns() != null && dov.getHasConcerns())).collect(Collectors.toList());
        election.setFinalAccessVote(CollectionUtils.isEmpty(rejectedVotes) ? true : false);
        election.setStatus(ElectionStatus.CLOSED.getValue());
        electionDAO.updateElectionById(electionId, election.getStatus(), new Date(), election.getFinalAccessVote());
    }

    @Override
    public boolean checkDataOwnerToCloseElection(Integer electionId){
        Boolean closeElection = false;
        Election election = electionDAO.findElectionById(electionId);
        if(electionDAO.findElectionTypeByElectionTypeId(election.getElectionType()).equals(ElectionType.DATA_SET.getValue())) {
            List<Vote> pendingVotes = voteDAO.findDataOwnerPendingVotesByElectionId(electionId, VoteType.DATA_OWNER.getValue());
            closeElection = CollectionUtils.isEmpty(pendingVotes) ? true : false;
        }
        return closeElection;
    }

    @Override
    public List<Election> createDataSetElections(String referenceId, Map<DACUser, List<DataSet>> dataOwnerDataSet){
        String electionType = electionDAO.findElectionTypeByType(ElectionType.DATA_SET.getValue());
        List<Integer> electionsIds = new ArrayList<>();
        dataOwnerDataSet.forEach((user,dataSets) -> {
            dataSets.stream().forEach(dataSet -> {
                if(electionDAO.getOpenElectionByReferenceIdAndDataSet(referenceId, dataSet.getDataSetId()) == null) {
                    Integer electionId = electionDAO.insertElection(electionType, ElectionStatus.OPEN.getValue(), new Date(), referenceId, dataSet.getDataSetId());
                    electionsIds.add(electionId);
                }
            });
        });
        return CollectionUtils.isEmpty(electionsIds) ? null : electionDAO.findElectionsByIds(electionsIds);
    }

    private void validateElectionIsValid(String referenceId, ElectionType electionType) throws Exception{
        if(electionType.equals(ElectionType.DATA_ACCESS)){
            Document dar = describeDataAccessRequestById(referenceId);
            if(dar == null){
                throw new NotFoundException();
            }
            List<DataSet> dataSets = verifyDisableDataSets(dar, referenceId);
            Consent consent = consentDAO.findConsentFromDatasetID(dataSets.get(0).getObjectId());
            Election consentElection = electionDAO.findLastElectionByReferenceIdAndStatus(consent.getConsentId(), "Closed");
            if((consentElection == null)){
                throw new IllegalArgumentException(DUL_NOT_APROVED);
            } else {
                Integer openElections = electionDAO.verifyOpenElectionsForReferenceId(consent.getConsentId());
                Vote vote = voteDAO.findVoteByElectionIdAndType(consentElection.getElectionId(), VoteType.CHAIRPERSON.getValue());
                if((openElections != 0) || (!vote.getVote())) {
                    throw new IllegalArgumentException(DUL_NOT_APROVED);
                }
            }
        }
    }

    private List<DataSet> verifyDisableDataSets(Document dar, String referenceId) throws  Exception{
        List<String> dataSets = dar.get(DarConstants.DATASET_ID, List.class);
        List<DataSet> dataSetList = dataSetDAO.searchDataSetsByObjectIdList(dataSets);
        List<String> disabledDataSets = dataSetList.stream().filter(ds -> !ds.getActive()).map(DataSet::getObjectId).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(disabledDataSets)) {
            boolean createElection = disabledDataSets.size() == dataSetList.size() ? false : true;
            DACUser dacUser = dacUserDAO.findDACUserById(dar.getInteger("userId"));
            if(!createElection){
                emailNotifierAPI.sendDisabledDatasetsMessage(dacUser, disabledDataSets, dar.getString(DarConstants.DAR_CODE));
                throw new IllegalArgumentException(INACTIVE_DS + disabledDataSets.toString());
            }else{
                updateDataAccessRequest(dataSetList, dar, dar.getString(DarConstants.DAR_CODE));
                emailNotifierAPI.sendDisabledDatasetsMessage(dacUser, disabledDataSets, dar.getString(DarConstants.DAR_CODE));
            }
        }
        return dataSetList;
    }

    private void updateDataAccessRequest(List<DataSet> dataSets, Document dar, String id){
        List<Document> dataSetList = new ArrayList<>();
        List<String> dataSetId = new ArrayList<>();
        List<DataSet> activeDataSets = dataSets.stream().filter(ds -> ds.getActive()).collect(Collectors.toList());
        activeDataSets.forEach((dataSet) -> {
            Document document = new Document();
            document.put(DarConstants.DATASET_ID, dataSet.getObjectId());
            dataSetId.add(dataSet.getObjectId());
            document.put("name", dataSet.getName());
            dataSetList.add(document);
        });
        dar.put(DarConstants.DATASET_ID,dataSetId);
        dar.put(DarConstants.DATASET_DETAIL,dataSetList);
        BasicDBObject query = new BasicDBObject(DarConstants.DAR_CODE, id);
        mongo.getDataAccessRequestCollection().findOneAndReplace(query, dar);
    }

    private void cancelOpenElection(String electionTypeId){
        List<Integer> openElectionsIds = electionDAO.findElectionsIdByTypeAndStatus(electionTypeId, ElectionStatus.OPEN.getValue());
        if (openElectionsIds != null && openElectionsIds.size() > 0) {
            electionDAO.updateElectionStatus(openElectionsIds, ElectionStatus.CANCELED.getValue());
        }
    }

    private Document describeDataAccessRequestById(String id){
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        return mongo.getDataAccessRequestCollection().find(query).first();
    }

    private List<Election> openElections(List<Election> openElections) throws Exception{
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
        BasicDBObject query;
        Document dar;
        switch (electionType) {
            case TRANSLATE_DUL:
                election.setElectionType(electionDAO
                        .findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue()));
                Consent consent = consentDAO.findConsentById(referenceId);
                election.setTranslatedUseRestriction(consent.getTranslatedUseRestriction());
                election.setUseRestriction(consent.getUseRestriction());
                break;
            case DATA_ACCESS:
                election.setElectionType(electionDAO
                        .findElectionTypeByType(ElectionType.DATA_ACCESS.getValue()));
                query = new BasicDBObject(DarConstants.ID, new ObjectId(referenceId));
                dar = mongo.getDataAccessRequestCollection().find(query).first();
                election.setTranslatedUseRestriction(dar.getString("translated_restriction"));
                try {
                    String restriction  =  new Gson().toJson(dar.get(DarConstants.RESTRICTION, Map.class));
                    election.setUseRestriction((UseRestriction.parse(restriction)));
                } catch (IOException e) {
                    election.setUseRestriction(null);
                }
                break;
            case RP:
                election.setElectionType(electionDAO
                        .findElectionTypeByType(ElectionType.RP.getValue()));
                query = new BasicDBObject(DarConstants.ID, new ObjectId(referenceId));
                dar = mongo.getDataAccessRequestCollection().find(query).first();
                election.setTranslatedUseRestriction(dar.getString("translated_restriction"));
                try {
                    String restriction = new Gson().toJson(dar.get(DarConstants.RESTRICTION, Map.class));
                    election.setUseRestriction((UseRestriction.parse(restriction)));

                } catch (IOException e) {
                    election.setUseRestriction(null);
                }
                break;
            case DATA_SET:
                election.setElectionType(electionDAO
                        .findElectionTypeByType(ElectionType.DATA_SET.getValue()));
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
        Document dar = describeDataAccessRequestById(dataRequest);
        if (dataRequest != null && dar == null ) {
            throw new NotFoundException("Invalid id: " + dataRequest);
        }
    }

    private void validateExistentElection(String referenceId, ElectionType type) {
        Election election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(referenceId, type.getValue());
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

    private void validateAvailableUsers(ElectionType electionType) {
        if(!electionType.equals(ElectionType.DATA_SET)){
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

    }


    private void updateSortDate(String referenceId, Date createDate){
        if(consentDAO.checkConsentbyId(referenceId) != null){
            consentDAO.updateConsentSortDate(referenceId, createDate);
        } else {
            BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(referenceId));
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
            voteDAO.updateVote(vote.getVote(), vote.getRationale(), vote.getUpdateDate(), vote.getVoteId(), vote.isReminderSent(), vote.getElectionId(), vote.getCreateDate(), vote.getHasConcerns());
        }
    }
}
