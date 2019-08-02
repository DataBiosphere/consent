package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.*;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.broadinstitute.consent.http.util.DatasetUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.ws.rs.NotFoundException;
import java.io.*;
import java.sql.Timestamp;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mongodb.client.model.Filters.*;

/**
 * Implementation class for DatabaseDataAccessRequestAPI.
 */
public class DatabaseDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private static Logger logger() {
        return Logger.getLogger("DatabaseDataAccessRequestAPI");
    }

    private MongoConsentDB mongo;

    private final UseRestrictionConverter converter;

    private static final String UN_REVIEWED = "un-reviewed";

    private final ElectionDAO electionDAO;

    private final ConsentDAO consentDAO;

    private  final ResearcherPropertyDAO  researcherPropertyDAO;

    private static final String DATA_SET_ID = "datasetId";

    private static final String SUFFIX = "-A-";

    private final VoteDAO voteDAO;

    private final DACUserDAO dacUserDAO;

    private final DataSetDAO dataSetDAO;

    private static final String NEEDS_APPROVAL = "Needs Approval";

    private static final String APPROVED = "Approved";

    private static final String DENIED = "Denied";

    private static final String PATH = "template/RequestApplication.pdf";

    private final DataAccessReportsParser dataAccessReportsParser;
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
    public static void initInstance(MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO, ResearcherPropertyDAO researcherPropertyDAO) {
        DataAccessRequestAPIHolder.setInstance(new DatabaseDataAccessRequestAPI(mongo, converter, electionDAO, consentDAO, voteDAO, dacUserDAO, dataSetDAO, researcherPropertyDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param mongo The Data Access Object used to read/write data.
     */
    protected DatabaseDataAccessRequestAPI(MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO, ResearcherPropertyDAO researcherPropertyDAO) {
        this.mongo = mongo;
        this.converter = converter;
        this.electionDAO = electionDAO;
        this.consentDAO = consentDAO;
        this.voteDAO = voteDAO;
        this.dacUserDAO = dacUserDAO;
        this.dataSetDAO = dataSetDAO;
        this.dataAccessReportsParser = new DataAccessReportsParser();
        this.researcherPropertyDAO = researcherPropertyDAO;
    }


    public void setMongoDBInstance(MongoConsentDB mongo) {
        this.mongo = mongo;
    }


    @Override
    public List<Document> createDataAccessRequest(Document dataAccessRequest) {
        List<Document> dataAccessList = new ArrayList<>();
        if (dataAccessRequest.containsKey(DarConstants.PARTIAL_DAR_CODE)){
            mongo.getPartialDataAccessRequestCollection().findOneAndDelete(new BasicDBObject(DarConstants.PARTIAL_DAR_CODE, dataAccessRequest.getString(DarConstants.PARTIAL_DAR_CODE)));
            dataAccessRequest.remove(DarConstants.ID);
            dataAccessRequest.remove(DarConstants.PARTIAL_DAR_CODE);
        }
        List<Integer> datasets =  dataAccessRequest.get(DATA_SET_ID, List.class);
        if (CollectionUtils.isNotEmpty(datasets)) {
            Set<ConsentDataSet> consentDataSets = consentDAO.getConsentIdAndDataSets(datasets);
            consentDataSets.forEach(consentDataSet -> {
                Document dataAccess = processDataSet(dataAccessRequest, consentDataSet);
                dataAccessList.add(dataAccess);
            });
        }
        dataAccessRequest.remove(DATA_SET_ID);
        insertDataAccess(dataAccessList);
        updateResearcherIdentification(dataAccessRequest);
        return dataAccessList;
    }


    @Override
    public Document describeDataAccessRequestById(String id) {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        return mongo.getDataAccessRequestCollection().find(query).first();
    }

    @Override
    public void deleteDataAccessRequestById(String id) {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);
    }


    @Override
    public Document describeDataAccessRequestFieldsById(String id, List<String> fields) {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        Document dar = mongo.getDataAccessRequestCollection().find(query).first();
        Document result = new Document();
        for (String field : fields) {
            if (field.equals(DarConstants.DATASET_ID)){
                List<String> dataSets = dar.get(field, List.class);
                result.append(field, dataSets);
            } else{
                String content = (String) dar.getOrDefault(field.replaceAll("\\s", ""), "Not found");
                result.append(field, content);
            }
        }
        return result;
    }

    /**
     * Find DARS related to the datasets sent as a parameter. Only dars with the use restriction
     * field present will be returned. DARs that require Manual Review wont be matched.
     * @param dataSetIds
     * @return A list of Data Access Requests.
     */
    @Override
    public List<Document> describeDataAccessWithDataSetIdAndRestriction(List<Integer> dataSetIds) {
        List<Document> response = new ArrayList<>();
        for (Integer datasetId : dataSetIds) {
            response.addAll(mongo.getDataAccessRequestCollection().find(and(eq(DarConstants.DATASET_ID, datasetId), eq(DarConstants.RESTRICTION, new BasicDBObject("$exists", true)))).into(new ArrayList<>()));
        }
        return response;
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
                : mongo.getDataAccessRequestCollection().find(new BasicDBObject(DarConstants.USER_ID, userId)).sort(new BasicDBObject("sortDate", -1));
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
    public List<String> describeDataAccessIdsForOwner(Integer userId) {
        List<String> referenceIds = new ArrayList<>();
        FindIterable<Document> accessList = mongo.getDataAccessRequestCollection().find(new BasicDBObject(DarConstants.USER_ID, userId)).sort(new BasicDBObject("sortDate", -1));
        for(Document doc: accessList){
            referenceIds.add(doc.get(DarConstants.ID).toString());
        }
        return referenceIds;
    }

    @Override
    public List<Document> describeDataAccessRequests() {
        return mongo.getDataAccessRequestCollection().find().into(new ArrayList<>());
    }

    @Override
    public Collection<String> getDatasetsInDARs(Collection<String> dataAccessRequestIds) {
        Collection<String> datasetIds = new HashSet<>();
        BasicDBObject projection = new BasicDBObject();
        projection.append(DarConstants.DATASET_ID,true);
        for (String darId : dataAccessRequestIds) {
            datasetIds.addAll((ArrayList) mongo.getDataAccessRequestCollection()
                    .find(eq(DarConstants.ID, new ObjectId(darId))).projection(projection).first().get(DarConstants.DATASET_ID));
        }
        return datasetIds;
    }


    @Override
    public UseRestriction createStructuredResearchPurpose(Document document) {
        DataUseDTO dto = converter.parseDataUsePurpose(document.toJson());
        return converter.parseUseRestriction(dto);
    }

    @Override
    public void deleteDataAccessRequest(Document document) {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, document.get(DarConstants.ID));
        mongo.getDataAccessRequestCollection().findOneAndDelete(query);
    }

    @Override
    public Document updateDataAccessRequest(Document dataAccessRequest, String id) {
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
    public Document describePartialDataAccessRequestById(String id) {
        BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(id));
        return mongo.getPartialDataAccessRequestCollection().find(query).first();
    }

    @Override
    public void deletePartialDataAccessRequestById(String id) {
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
                : mongo.getPartialDataAccessRequestCollection().find(new BasicDBObject(DarConstants.USER_ID, userId)).sort(new BasicDBObject("sortDate", -1));
        List<Document> darManage = new ArrayList<>();
        List<String> accessRequestIds = getRequestIds(accessList);
        if (CollectionUtils.isNotEmpty(accessRequestIds)){
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
        if (access != null){
            List<Vote> votes = voteDAO.findDACVotesByElectionId(access.getElectionId());
            List<Integer> userIds = votes.stream().map(Vote::getDacUserId).collect(Collectors.toList());
            dacUsers.addAll(dacUserDAO.findUsers(userIds));
        } else {
            dacUsers =  dacUserDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getValue(), true);
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

    @Override
    public boolean hasUseRestriction(String referenceId){
        return getField(referenceId, DarConstants.RESTRICTION) != null ? true : false;
    }

    @Override
    public List<UseRestrictionDTO> getInvalidDataAccessRequest() {
        List<Document> darList = new ArrayList<>();
        darList.addAll(mongo.getDataAccessRequestCollection().find(eq(DarConstants.VALID_RESTRICTION, false)).into(new ArrayList<>()));
        List<UseRestrictionDTO> invalidRestrictions = new ArrayList<>();
        darList.forEach(c->{
            invalidRestrictions.add(new UseRestrictionDTO(c.get(DarConstants.DAR_CODE, String.class),new Gson().toJson(c.get(DarConstants.RESTRICTION, Map.class))));
        });
        return invalidRestrictions;
    }

    @Override
    public void updateDARUseRestrictionValidation(List<String> darCodes, Boolean validUseRestriction){
        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append(DarConstants.VALID_RESTRICTION, validUseRestriction);
        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);

        mongo.getDataAccessRequestCollection().updateMany(in(DarConstants.DAR_CODE, darCodes), setQuery);
    }

    @Override
    public FindIterable<Document> findDARUseRestrictions(){
        return mongo.getDataAccessRequestCollection().find(ne(DarConstants.RESTRICTION, null)).projection(Projections.include(DarConstants.DAR_CODE, DarConstants.RESTRICTION));
    }

    private void updateElection(Election access, Election rp) {
        if (access != null) {
            access.setStatus(ElectionStatus.CANCELED.getValue());
            electionDAO.updateElectionStatus(new ArrayList<>(Collections.singletonList(access.getElectionId())), access.getStatus());
        }
        if (rp != null){
            rp.setStatus(ElectionStatus.CANCELED.getValue());
            electionDAO.updateElectionStatus(new ArrayList<>(Collections.singletonList(rp.getElectionId())), rp.getStatus());
        }
    }

    @Override
    public byte[] createDARDocument(Document dar, Map<String, String> researcherProperties, UserRole role, Boolean manualReview, String sDUR) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PDDocument darDOC = new PDDocument();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream is = classLoader.getResourceAsStream(PATH);
            darDOC = PDDocument.load(is);
            new DataAccessParser().fillDARForm(dar, researcherProperties, role, manualReview, darDOC.getDocumentCatalog().getAcroForm(), sDUR);
            darDOC.save(output);
            return output.toByteArray();
        } finally {
            output.close();
            darDOC.close();
        }

    }

    /**
     * Description: this method returns the correct structured Data Use Restriction for an election.
     * If there is no Access Election with the corresponding DAR reference Id, it will return Consent
     * Election's sDUR associated with its DatasetId.
     *
     * If there is a valid Access Election, we find the consent associated to it by the electionId
     * and try to get the sDUR from there.
     *
     * If there is no Access Election relationship to be found, we treat the reference id (a loose
     * reference to an ID) as a consent id and look up the Consent Election's sDUR.
     *
     * Finally, if for some reason there's no Election related to a given Consent we use DUR in
     * consents table, we look for the DUR on the consent itself.
     *
     * @param dar Mongo document correponding to a specific its reference Id
     * @return sDUR Structured Data Use Restriction
     */
    @Override
    public String getStructuredDURForPdf(Document dar) {
        List<Integer> dataSetId = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
        Election accessElection = electionDAO.findLastElectionByReferenceIdAndType(dar.get(DarConstants.ID).toString(), ElectionType.DATA_ACCESS.getValue());
        String sDUR;
        if (accessElection != null) {
            Integer electionId = electionDAO.getElectionConsentIdByDARElectionId(accessElection.getElectionId());
            Election dulElection = electionId != null ? electionDAO.findElectionById(electionId) : null;
            if (dulElection != null) {
                sDUR = dulElection.getTranslatedUseRestriction();
            } else {
                String consentId = dataSetDAO.getAssociatedConsentIdByDataSetId(dataSetId.get(0));
                dulElection = electionDAO.findDULApprovedElectionByReferenceId(consentId);
                sDUR = dulElection != null ? dulElection.getTranslatedUseRestriction() : consentDAO.findConsentById(consentId).getTranslatedUseRestriction();
            }
        } else {
            String consentId = dataSetDAO.getAssociatedConsentIdByDataSetId(dataSetId.get(0));
            sDUR = electionDAO.findDULApprovedElectionByReferenceId(consentId).getTranslatedUseRestriction();
        }
        return sDUR;
    }

    @Override
    public File createApprovedDARDocument() throws IOException {
        List<Election> elections = electionDAO.findDataAccessClosedElectionsByFinalResult(true);
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        FileWriter darWriter = new FileWriter(file);
        dataAccessReportsParser.setApprovedDARHeader(darWriter);
        if (CollectionUtils.isNotEmpty(elections)) {
            for (Election election : elections) {
                Document dar = describeDataAccessRequestById(election.getReferenceId());
                if (dar != null) {
                    String profileName = researcherPropertyDAO.findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.PROFILE_NAME);
                    String institution = researcherPropertyDAO.findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.INSTITUTION);
                    String consentName = consentDAO.findConsentNameFromDatasetID(dar.get(DarConstants.DATASET_ID, ArrayList.class).get(0).toString());
                    Election consentElection = getConsentElection(election.getElectionId(), dar);
                    dataAccessReportsParser.addApprovedDARLine(darWriter, election, dar, profileName, institution, consentName, consentElection.getTranslatedUseRestriction());
                }
            }
        }
        darWriter.flush();
        return file;
    }

    @Override
    public File createReviewedDARDocument() throws IOException {
        List<Election> approvedElections = electionDAO.findDataAccessClosedElectionsByFinalResult(true);
        List<Election> disaprovedElections = electionDAO.findDataAccessClosedElectionsByFinalResult(false);
        List<Election> elections = new ArrayList<>();
        elections.addAll(approvedElections);
        elections.addAll(disaprovedElections);
        File file = File.createTempFile("ReviewedDataAccessRequests", ".tsv");
        FileWriter darWriter = new FileWriter(file);
        dataAccessReportsParser.setReviewedDARHeader(darWriter);
        if (CollectionUtils.isNotEmpty(elections)) {
            for (Election election : elections) {
                Document dar = describeDataAccessRequestById(election.getReferenceId());
                if (dar != null) {
                    String consentName = consentDAO.findConsentNameFromDatasetID(dar.get(DarConstants.DATASET_ID, ArrayList.class).get(0).toString());
                    Election consentElection = getConsentElection(election.getElectionId(), dar);
                    dataAccessReportsParser.addReviewedDARLine(darWriter, election, dar, consentName, consentElection.getTranslatedUseRestriction());
                }
            }
        }
        darWriter.flush();
        return file;
    }


    @Override
    public File createDataSetApprovedUsersDocument(Integer dataSetId) throws IOException {
        File file = File.createTempFile("DatasetApprovedUsers", ".tsv");
        FileWriter darWriter = new FileWriter(file);
        List<Document> darList = describeDataAccessByDataSetId(dataSetId);
        dataAccessReportsParser.setDataSetApprovedUsersHeader(darWriter);
        if (CollectionUtils.isNotEmpty(darList)){
            for(Document dar: darList){
                Date approvalDate = electionDAO.findApprovalAccessElectionDate(dar.get(DarConstants.ID).toString());
                if (approvalDate != null) {
                    String email = researcherPropertyDAO.findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.ACADEMIC_BUSINESS_EMAIL);
                    String name = researcherPropertyDAO.findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.PROFILE_NAME);
                    String institution = researcherPropertyDAO.findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.INSTITUTION);
                    String darCode = dar.getString(DarConstants.DAR_CODE);
                    dataAccessReportsParser.addDataSetApprovedUsersLine(darWriter, email, name, institution, darCode, approvalDate);
                }
            }
        }
        darWriter.flush();
        return file;
    }

    @Override
    public DARModalDetailsDTO DARModalDetailsDTOBuilder(Document dar, DACUser dacUser, ElectionAPI electionApi, UserRole role) {
        DARModalDetailsDTO darModalDetailsDTO = new DARModalDetailsDTO();
        List<Document> datasetDetails = populateDatasetDetailDocuments(dar.get(DarConstants.DATASET_DETAIL));
        return darModalDetailsDTO
            .setNeedDOApproval(electionApi.darDatasetElectionStatus((dar.get(DarConstants.ID).toString())))
            .setResearcherName(dacUser, dar.getString(DarConstants.INVESTIGATOR))
            .setStatus(role.getStatus())
            .setRationale(role.getRationale())
            .setUserId(dar.getInteger(DarConstants.USER_ID))
            .setDarCode(dar.getString(DarConstants.DAR_CODE))
            .setPrincipalInvestigator(dar.getString(DarConstants.INVESTIGATOR))
            .setInstitutionName(dar.getString(DarConstants.INSTITUTION))
            .setProjectTitle(dar.getString(DarConstants.PROJECT_TITLE))
            .setDepartment(dar.getString(DarConstants.DEPARTMENT))
            .setCity(dar.getString(DarConstants.CITY))
            .setCountry(dar.getString(DarConstants.COUNTRY))
            .setNihUsername(dar.getString(DarConstants.NIH_USERNAME))
            .setHaveNihUsername(StringUtils.isNotEmpty(dar.getString(DarConstants.NIH_USERNAME)))
            .setIsThereDiseases(false)
            .setIsTherePurposeStatements(false)
            .setResearchType(dar)
            .setDiseases(dar)
            .setPurposeStatements(dar)
            .setDatasetDetail(datasetDetails);
    }

    /**
     * Dataset Detail fields have undergone transition and can have different keys in the mongo source
     *
     * "datasetDetail": [
     *     {
     *       "datasetId": "1",
     *       "name": "CMG_Manton",
     *       "objectId": "SC-25537"
     *     }
     *   ]
     *
     * OR something like this ...
     *
     *  "datasetDetail": [
     *     {
     *       "datasetId": "143",
     *       "name": "IDH-mutant astrocytoma - Suva"
     *     },
     *     {
     *       "datasetId": "142",
     *       "name": "oligodendroglioma scRNA-seq - Suva"
     *     }
     *   ]
     *
     *   For backwards compatibility, we need to recreate the "objectId" field if they are missing
     */
    @SuppressWarnings("unchecked")
    private List<Document> populateDatasetDetailDocuments(Object datasetDetails) {
        List<Document> newDetails = new ArrayList<>();
        try {
            ((ArrayList<Document>) datasetDetails).forEach(d -> {
                // If we already have an objectId, we don't need to do any additional work
                if (d.containsKey(DarConstants.OBJECT_ID)) {
                    newDetails.add(d);
                }
                // If we have do have a datasetId, we can look it up and populate the object id field
                else if (d.containsKey(DarConstants.DATASET_ID)) {
                    DataSet dataSet = dataSetDAO.findDataSetById(Integer.valueOf(d.getString(DarConstants.DATASET_ID)));
                    if (dataSet.getAlias() != null) {
                        String aliasString = DatasetUtil.parseAlias(dataSet.getAlias());
                        d.put(DarConstants.OBJECT_ID, aliasString);
                        newDetails.add(d);
                    }
                }
                // Last case - just add the doc and return.
                else {
                    newDetails.add(d);
                }
            });
        } catch (Exception e) {
            logger().warn(e);
        }
        return newDetails;
    }

    private List<Document> describeDataAccessByDataSetId(Integer dataSetId) {
        List<Document> response = new ArrayList<>();
        response.addAll(mongo.getDataAccessRequestCollection().find(eq(DarConstants.DATASET_ID, dataSetId.toString())).into(new ArrayList<>()));
        return response;
    }

    private void insertDataAccess(List<Document> dataAccessRequestList) {
        if (CollectionUtils.isNotEmpty(dataAccessRequestList)){
            String seq = mongo.getNextSequence(DarConstants.PARTIAL_DAR_CODE_COUNTER);
            if (dataAccessRequestList.size() > 1) {
                IntStream.range(0, dataAccessRequestList.size())
                        .forEach(idx -> {
                                    dataAccessRequestList.get(idx).append(DarConstants.DAR_CODE, "DAR-" + seq + SUFFIX + idx);
                                    dataAccessRequestList.get(idx).remove(DarConstants.ID);
                                    if (dataAccessRequestList.get(idx).get(DarConstants.PARTIAL_DAR_CODE) != null){
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

    private DACUser getOwnerUser(Integer dacUserId){
        List<DACUser> users = new ArrayList<>();
        users.addAll(dacUserDAO.findUsersWithRoles(new ArrayList<>(Collections.singletonList(dacUserId))));
        return users.get(0);
    }

    private List<DataAccessRequestManage> createAccessRequestManage(FindIterable<Document> documents, Map<String, Election> electionList) {
        List<DataAccessRequestManage> requestsManage = new ArrayList<>();
        documents.forEach((Block<Document>) dar -> {
            DataAccessRequestManage darManage = new DataAccessRequestManage();
            ObjectId id = dar.get(DarConstants.ID, ObjectId.class);
            List<Integer> dataSets =  DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
            List<DataSet> dataSetsToApprove = dataSetDAO.findNeedsApprovalDataSetByDataSetId(dataSets);
            Election election = electionList.get(id.toString());
            darManage.setCreateDate(new Timestamp((long) id.getTimestamp() * 1000));
            darManage.setRus(dar.getString(DarConstants.RUS));
            darManage.setProjectTitle(dar.getString(DarConstants.PROJECT_TITLE));
            darManage.setDataRequestId(id.toString());
            darManage.setFrontEndId(dar.get(DarConstants.DAR_CODE).toString());
            darManage.setSortDate(dar.getDate("sortDate"));
            darManage.setIsCanceled(dar.containsKey(DarConstants.STATUS) && dar.get(DarConstants.STATUS).equals(ElectionStatus.CANCELED.getValue()) ? true : false);
            darManage.setNeedsApproval(CollectionUtils.isNotEmpty(dataSetsToApprove) ? true : false);
            darManage.setDataSetElectionResult(darManage.getNeedsApproval() ? NEEDS_APPROVAL : "");
            darManage.setElectionStatus(election == null ? UN_REVIEWED : election.getStatus());
            darManage.setElectionId(election != null ? election.getElectionId() : null);
            darManage.setElectionVote(election != null ? election.getFinalVote() : null);
            if (election != null && !CollectionUtils.isEmpty(electionDAO.getElectionByTypeStatusAndReferenceId(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue(), election.getReferenceId()))) {
                darManage.setElectionStatus(ElectionStatus.PENDING_APPROVAL.getValue());
            }
            else if (CollectionUtils.isNotEmpty(dataSetsToApprove) && election != null && election.getStatus().equals(ElectionStatus.CLOSED.getValue())) {
                List<String> referenceList = Collections.singletonList(election.getReferenceId());
                List<Election> datasetElections = electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(referenceList, ElectionType.DATA_SET.getValue());
                darManage.setDataSetElectionResult(consolidateDataSetElectionsResult(datasetElections));
            }
            try{
                darManage.setOwnerUser(getOwnerUser(dar.getInteger(DarConstants.USER_ID)));
            }catch (Exception e){
                darManage.setOwnerUser(getOwnerUser(Integer.valueOf(dar.getString(DarConstants.USER_ID))));
            }
            requestsManage.add(darManage);
        });
        return requestsManage;
    }

    private String consolidateDataSetElectionsResult(List<Election> datasetElections) {
        if (CollectionUtils.isNotEmpty(datasetElections)) {
            for (Election election : datasetElections) {
                if (!election.getFinalAccessVote()) {
                    return DENIED;
                }
            }
            return APPROVED;
        }
        return NEEDS_APPROVAL;
    }

    private List getRequestIds(FindIterable<Document> access) {
        List<String> accessIds = new ArrayList<>();
        if (access != null) {
            access.forEach((Block<Document>) document ->
                accessIds.add(document.get(DarConstants.ID).toString())
            );
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
        List<Integer> datasetId = new ArrayList<>();
        Document dataAccess = new Document(dataAccessRequest);
        consentDataSet.getDataSets().forEach((k,v) -> {
            Document document = new Document();
            document.put(DATA_SET_ID,k);
            datasetId.add(Integer.valueOf(k));
            document.put("name", v);
            dataSetList.add(document);
            String objectId = dataSetDAO.findObjectIdByDataSetId(Integer.valueOf(k));
            if (StringUtils.isNotEmpty(objectId)) {
                document.put("objectId", objectId);
            }
        });
        dataAccess.put(DarConstants.DATASET_ID, datasetId);
        dataAccess.put(DarConstants.DATASET_DETAIL,dataSetList);
        return dataAccess;
    }

    protected List<ResearcherProperty> updateResearcherIdentification(Document dataAccessRequest) {
        Integer userId = dataAccessRequest.getInteger(DarConstants.USER_ID);
        String linkedIn = dataAccessRequest.getString(ResearcherFields.LINKEDIN_PROFILE.getValue());
        String orcId = dataAccessRequest.getString(ResearcherFields.ORCID.getValue());
        String researcherGate = dataAccessRequest.getString(ResearcherFields.RESEARCHER_GATE.getValue());
        String urlDAA = dataAccessRequest.getString(ResearcherFields.URL_DAA.getValue());
        String nameDAA = dataAccessRequest.getString(ResearcherFields.NAME_DAA.getValue());
        List<ResearcherProperty> rpList = new ArrayList<>();
        researcherPropertyDAO.deletePropertyByUser(Arrays.asList(ResearcherFields.LINKEDIN_PROFILE.getValue(), ResearcherFields.ORCID.getValue(), ResearcherFields.RESEARCHER_GATE.getValue(), ResearcherFields.NAME_DAA.getValue(), ResearcherFields.URL_DAA.getValue()), userId);
        if (StringUtils.isNotEmpty(linkedIn)) {
          rpList.add(new ResearcherProperty(userId, ResearcherFields.LINKEDIN_PROFILE.getValue(), linkedIn));
        }
        if (StringUtils.isNotEmpty(orcId)) {
          rpList.add(new ResearcherProperty(userId, ResearcherFields.ORCID.getValue(), orcId));
        }
        if (StringUtils.isNotEmpty(researcherGate)) {
           rpList.add(new ResearcherProperty(userId, ResearcherFields.RESEARCHER_GATE.getValue(), researcherGate));
        }
        if (StringUtils.isNotEmpty(nameDAA)) {
            rpList.add(new ResearcherProperty(userId, ResearcherFields.NAME_DAA.getValue(), nameDAA));
        }
        if (StringUtils.isNotEmpty(urlDAA)) {
            rpList.add(new ResearcherProperty(userId, ResearcherFields.URL_DAA.getValue(), urlDAA));
        }
        if (CollectionUtils.isNotEmpty(rpList)) {
           researcherPropertyDAO.insertAll(rpList);
        }
        return rpList;
    }

    private Election getConsentElection(Integer darElectionId, Document dar) {
        Integer electionId = electionDAO.getElectionConsentIdByDARElectionId(darElectionId);
        Election election = electionDAO.findElectionById(electionId);
        if (election == null) {
            List<Integer> datasetIds = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
            if (CollectionUtils.isNotEmpty(datasetIds)) {
                Consent consent = consentDAO.findConsentFromDatasetID(datasetIds.get(0));
                election = electionDAO.findDULApprovedElectionByReferenceId(consent.getConsentId());
            }
        }
        return election;
    }

}

