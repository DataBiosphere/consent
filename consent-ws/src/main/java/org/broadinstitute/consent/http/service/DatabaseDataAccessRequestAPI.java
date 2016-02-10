package org.broadinstitute.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;


/**
 * Implementation class for DatabaseDataAccessRequestAPI.
 */
public class DatabaseDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private MongoConsentDB mongo;

    private final UseRestrictionConverter converter;

    private final String UN_REVIEWED = "un-reviewed";

    private final ElectionDAO electionDAO;

    private final ConsentDAO consentDAO;

    private final String DATA_SET_ID = "datasetId";

    private final String SUFFIX = "-A-";

    private final VoteDAO voteDAO;

    private final DACUserDAO dacUserDAO;

    private final DataSetDAO dataSetDAO;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param mongo     The Data Access Object instance that the API should use to
     *                  read/write data.
     * @param converter
     */
    public static void initInstance(MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO) {
        DataAccessRequestAPIHolder.setInstance(new DatabaseDataAccessRequestAPI(mongo, converter, electionDAO, consentDAO, voteDAO, dacUserDAO, dataSetDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param mongo The Data Access Object used to read/write data.
     */
    private DatabaseDataAccessRequestAPI(MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO) {
        this.mongo = mongo;
        this.converter = converter;
        this.electionDAO = electionDAO;
        this.consentDAO = consentDAO;
        this.voteDAO = voteDAO;
        this.dacUserDAO = dacUserDAO;
        this.dataSetDAO = dataSetDAO;
    }


    public void setMongoDBInstance(MongoConsentDB mongo) {
        this.mongo = mongo;
    }


    @Override
    public List<Document> createDataAccessRequest(Document dataAccessRequest) throws MongoException {
        List<Document> dataAccessList = new ArrayList<>();
        if(dataAccessRequest.containsKey(DarConstants.PARTIAL_DAR_CODE)){
            mongo.getPartialDataAccessRequestCollection().findOneAndDelete(new BasicDBObject(DarConstants.PARTIAL_DAR_CODE, dataAccessRequest.getString(DarConstants.PARTIAL_DAR_CODE)));
            dataAccessRequest.remove(DarConstants.ID);
            dataAccessRequest.remove(DarConstants.PARTIAL_DAR_CODE);
        }
        List<String> dataSets =  dataAccessRequest.get(DATA_SET_ID, List.class);
        dataAccessRequest.remove(DATA_SET_ID);
        if (CollectionUtils.isNotEmpty(dataSets)) {
            Set<ConsentDataSet> consentDataSets = consentDAO.getConsentIdAndDataSets(dataSets);
            consentDataSets.forEach((consentDataSet) -> {
                Document dataAccess = processDataSet(dataAccessRequest, consentDataSet);
                dataAccessList.add(dataAccess);
            });
        }
        insertDataAccess(dataAccessList);
        return dataAccessList;
    }


    @Override
    public Document describeDataAccessRequestById(String id) throws NotFoundException {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        return mongo.getDataAccessRequestCollection().find(query).first();
    }

    @Override
    public void deleteDataAccessRequestById(String id) {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);
    }


    @Override
    public Document describeDataAccessRequestFieldsById(String id, List<String> fields) throws NotFoundException {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        Document dar = mongo.getDataAccessRequestCollection().find(query).first();
        Document result = new Document();
        for (String field : fields) {
            if(field.equals(DarConstants.DATASET_ID)){
                List<String> dataSets = dar.get(field, List.class);
                result.append(field, dataSets);
            }else{
                String content = (String) dar.getOrDefault(field.replaceAll("\\s", ""), "Not found");
                result.append(field, content);
            }
        }
        return result;
    }

    @Override
    public List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds) {
        List<Document> response = new ArrayList<>();
        for (String datasetId : dataSetIds) {
            response.addAll(mongo.getDataAccessRequestCollection().find(eq(DarConstants.DATASET_ID, datasetId)).into(new ArrayList<>()));
        }
        return response;
    }

    @Override
    public List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId) {
        FindIterable<Document> accessList = userId == null ? mongo.getDataAccessRequestCollection().find().sort(new BasicDBObject("sortDate", -1))
                : mongo.getDataAccessRequestCollection().find(new BasicDBObject("userId", userId)).sort(new BasicDBObject("sortDate", -1));
        List<DataAccessRequestManage> darManage = new ArrayList<>();
        List<String> accessRequestIds = getRequestIds(accessList);
        if (CollectionUtils.isNotEmpty(accessRequestIds)) {
            List<Election> electionList = new ArrayList<>();
            electionList.addAll(electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(accessRequestIds, ElectionType.DATA_ACCESS.getValue()));
            HashMap electionAccessMap = createAccessRequestElectionMap(electionList);
            darManage.addAll(createAccessRequestManage(accessList, electionAccessMap));
        }
        return darManage;
    }

    @Override
    public List<Document> describeDataAccessRequests() {
        return mongo.getDataAccessRequestCollection().find().into(new ArrayList<>());
    }


    @Override
    public UseRestriction createStructuredResearchPurpose(Document document) {
        return converter.parseJsonFormulary(document.toJson());
    }

    @Override
    public void deleteDataAccessRequest(Document document) {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, document.get(DarConstants.ID));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);
    }

    @Override
    public Document updateDataAccessRequest(Document dataAccessRequest, String id) throws MongoException {
        BasicDBObject query = new BasicDBObject(DarConstants.DAR_CODE, id);
        dataAccessRequest.remove(DarConstants.ID);
        dataAccessRequest.put("sortDate", new Date());
        if (mongo.getDataAccessRequestCollection().findOneAndReplace(query, dataAccessRequest) == null) {
            throw new NotFoundException("Data access for the specified id does not exist");
        }
        return mongo.getDataAccessRequestCollection().find(query).first();
    }

    @Override
    public Integer getTotalUnReviewedDAR() {
        FindIterable<Document> accessList = mongo.getDataAccessRequestCollection().find(ne(DarConstants.STATUS,ElectionStatus.CANCELED.getValue()));
        Integer unReviewedDAR = 0;
        List<String> accessRequestIds = getRequestIds(accessList);
        if (CollectionUtils.isNotEmpty(accessRequestIds)) {
            List<Election> electionList = new ArrayList<>();
            electionList.addAll(electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(accessRequestIds, ElectionType.DATA_ACCESS.getValue()));
            HashMap<String, Election> electionAccessMap = createAccessRequestElectionMap(electionList);
            for (Document dar : accessList) {
                ObjectId id = dar.get(DarConstants.ID, ObjectId.class);
                Election election = electionAccessMap.get(id.toString());
                if (election == null) ++unReviewedDAR;
            }
        }
        return unReviewedDAR;
    }

    // Partial Data Access Request Methods below
    @Override
    public List<Document> describePartialDataAccessRequests() {
        return mongo.getPartialDataAccessRequestCollection().find().into(new ArrayList<>());
    }

    @Override
    public Document describePartialDataAccessRequestById(String id) throws NotFoundException {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        return mongo.getPartialDataAccessRequestCollection().find(query).first();
    }

    @Override
    public void deletePartialDataAccessRequestById(String id) throws IllegalArgumentException {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        mongo.getPartialDataAccessRequestCollection().findOneAndDelete(query);
    }

    @Override
    public Document updatePartialDataAccessRequest(Document partialDar) {
        BasicDBObject query = new BasicDBObject(DarConstants.PARTIAL_DAR_CODE, partialDar.get(DarConstants.PARTIAL_DAR_CODE));
        partialDar.remove(DarConstants.ID);
        partialDar.put("sortDate", new Date());
        if (mongo.getPartialDataAccessRequestCollection().findOneAndReplace(query, partialDar) == null) {
            throw new NotFoundException("Partial Data access for the specified id does not exist");
        }
        return mongo.getPartialDataAccessRequestCollection().find(query).first();
    }

    @Override
    public Document createPartialDataAccessRequest(Document partialDar){
        String seq = mongo.getNextSequence(DarConstants.PARTIAL_DAR_CODE_COUNTER);
        partialDar.put("createDate", new Date());
        partialDar.append(DarConstants.PARTIAL_DAR_CODE, "temp_DAR" + seq);
        mongo.getPartialDataAccessRequestCollection().insertOne(partialDar);
        return partialDar;
    }

    @Override
    public List<Document> describePartialDataAccessRequestManage(Integer userId) {
        FindIterable<Document> accessList = userId == null ? mongo.getPartialDataAccessRequestCollection().find().sort(new BasicDBObject("sortDate", -1))
                : mongo.getPartialDataAccessRequestCollection().find(new BasicDBObject("userId", userId)).sort(new BasicDBObject("sortDate", -1));
        List<Document> darManage = new ArrayList<>();
        List<String> accessRequestIds = getRequestIds(accessList);
        if(CollectionUtils.isNotEmpty(accessRequestIds)){
            for(Document doc: accessList){
                doc.append("dataRequestId", doc.get(DarConstants.ID).toString());
                darManage.add(doc);
            }
        }
        return darManage;
    }

    @Override
    public Document cancelDataAccessRequest(String referenceId){
        Document dar = describeDataAccessRequestById(referenceId);
        dar.append(DarConstants.STATUS, ElectionStatus.CANCELED.getValue());
        BasicDBObject query = new BasicDBObject(DarConstants.DAR_CODE, dar.get(DarConstants.DAR_CODE));
        dar = mongo.getDataAccessRequestCollection().findOneAndReplace(query, dar);
        return dar;
    }

    @Override
    public List<DACUser> getUserEmailAndCancelElection(String referenceId) {
        Election access = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(referenceId, ElectionType.DATA_ACCESS.getValue());
        Election rp = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(referenceId, ElectionType.RP.getValue());
        updateElection(access, rp);
        List<DACUser> dacUsers = new ArrayList<>();
        if(access != null){
            List<Vote> votes = voteDAO.findDACVotesByElectionId(access.getElectionId());
            List<Integer> userIds = votes.stream().map(Vote::getDacUserId).collect(Collectors.toList());
            dacUsers.addAll(dacUserDAO.findUsers(userIds));
        } else {
            dacUsers =  dacUserDAO.describeUsersByRoleAndEmailPreference(DACUserRoles.ADMIN.getValue(), true);
        }
        return dacUsers;
    }

    @Override
    public Object getField(String requestId , String field){
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(requestId));
        BasicDBObject projection = new BasicDBObject();
        projection.append(field,true);
        Document dar = mongo.getDataAccessRequestCollection().find(query).projection(projection).first();
        return dar != null ? dar.get(field) : null;
    }

    private void updateElection(Election access, Election rp) {
        if(access != null) {
            access.setStatus(ElectionStatus.CANCELED.getValue());
            electionDAO.updateElectionStatus(new ArrayList<>(Arrays.asList(access.getElectionId())), access.getStatus());
        }
        if(rp != null){
            rp.setStatus(ElectionStatus.CANCELED.getValue());
            electionDAO.updateElectionStatus(new ArrayList<>(Arrays.asList(rp.getElectionId())), rp.getStatus());
        }
    }


    private void insertDataAccess(List<Document> dataAccessRequestList) {
        if(CollectionUtils.isNotEmpty(dataAccessRequestList)){
            String seq = mongo.getNextSequence(DarConstants.PARTIAL_DAR_CODE_COUNTER);
            if (dataAccessRequestList.size() > 1) {
                IntStream.range(0, dataAccessRequestList.size())
                        .forEach(idx -> {
                                    dataAccessRequestList.get(idx).append(DarConstants.DAR_CODE, "DAR-" + seq + SUFFIX + idx);
                                    dataAccessRequestList.get(idx).remove(DarConstants.ID);
                                    if(dataAccessRequestList.get(idx).get(DarConstants.PARTIAL_DAR_CODE) != null){
                                        BasicDBObject query = new BasicDBObject(DarConstants.PARTIAL_DAR_CODE, dataAccessRequestList.get(idx).get(DarConstants.PARTIAL_DAR_CODE));
                                        mongo.getPartialDataAccessRequestCollection().findOneAndDelete(query);
                                        dataAccessRequestList.get(idx).remove(DarConstants.PARTIAL_DAR_CODE);
                                    }
                                }

                        );
                mongo.getDataAccessRequestCollection().insertMany(dataAccessRequestList);
            }else{
                dataAccessRequestList.get(0).append(DarConstants.DAR_CODE, "DAR-" + seq);
                mongo.getDataAccessRequestCollection().insertMany(dataAccessRequestList);
            }
        }
    }

    private List<DataAccessRequestManage> createAccessRequestManage(FindIterable<Document> documents, Map<String, Election> electionList) {
        List<DataAccessRequestManage> requestsManage = new ArrayList<>();
        documents.forEach((Block<Document>) dar -> {
            DataAccessRequestManage darManage = new DataAccessRequestManage();
            ObjectId id = dar.get(DarConstants.ID, ObjectId.class);
            List<String> dataSets = dar.get(DarConstants.DATASET_ID, List.class);
            List<DataSet> dataSetsToApprove = dataSetDAO.findNeedsApprovalDataSetByObjectId(dataSets);
            Election election = electionList.get(id.toString());
            darManage.setCreateDate(new Timestamp((long) id.getTimestamp() * 1000));
            darManage.setRus(dar.getString(DarConstants.RUS));
            darManage.setProjectTitle(dar.getString(DarConstants.PROJECT_TITLE));
            darManage.setDataRequestId(id.toString());
            darManage.setFrontEndId(dar.get(DarConstants.DAR_CODE).toString());
            darManage.setSortDate(dar.getDate("sortDate"));
            darManage.setIsCanceled(dar.containsKey(DarConstants.STATUS) && dar.get(DarConstants.STATUS).equals(ElectionStatus.CANCELED.getValue()) ? true : false);
            darManage.setNeedsApproval(CollectionUtils.isNotEmpty(dataSetsToApprove) ? true : false);
            if (election == null) {
                darManage.setElectionStatus(UN_REVIEWED);
            }
            else {
                if(!CollectionUtils.isEmpty(electionDAO.getElectionByTypeStatusAndReferenceId(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue(), election.getReferenceId()))){
                    darManage.setElectionStatus(ElectionStatus.PENDING_APPROVAL.getValue());
                }else{
                    darManage.setElectionId(election.getElectionId());
                    darManage.setElectionStatus(election.getStatus());
                    darManage.setElectionVote(election.getFinalVote());
                }
            }

            requestsManage.add(darManage);
        });
        return requestsManage;
    }

    private List getRequestIds(FindIterable<Document> access) {
        List<String> accessIds = new ArrayList<>();
        if (access != null) {
            access.forEach((Block<Document>) document -> {
                accessIds.add(document.get(DarConstants.ID).toString());
            });
        }
        return accessIds;
    }

    private HashMap createAccessRequestElectionMap(List<Election> elections) {
        HashMap electionMap = new HashMap<>();
        elections.forEach(election -> {
            electionMap.put(election.getReferenceId(), election);
        });
        return electionMap;
    }


    private Document processDataSet(Document dataAccessRequest, ConsentDataSet consentDataSet) {
        List<Document> dataSetList = new ArrayList<>();
        List<String> datasetId = new ArrayList<>();
        Document dataAccess = new Document(dataAccessRequest);
        consentDataSet.getDataSets().forEach((k,v) -> {
            Document document = new Document();
            document.put(DATA_SET_ID,k);
            datasetId.add(k);
            document.put("name", v);
            dataSetList.add(document);
        });
        dataAccess.put(DarConstants.DATASET_ID, datasetId);
        dataAccess.put(DarConstants.DATASET_DETAIL,dataSetList);
        return dataAccess;
    }




}