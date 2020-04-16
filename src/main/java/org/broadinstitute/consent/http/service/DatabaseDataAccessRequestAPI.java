package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentDataSet;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.ws.rs.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @deprecated Use DataAccessRequestService
 * Implementation class for DatabaseDataAccessRequestAPI.
 */
@Deprecated
public class DatabaseDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private static Logger logger() {
        return Logger.getLogger("DatabaseDataAccessRequestAPI");
    }

    private MongoConsentDB mongo;

    private final UseRestrictionConverter converter;

    private final ElectionDAO electionDAO;

    private final ConsentDAO consentDAO;

    private  final ResearcherPropertyDAO  researcherPropertyDAO;

    private static final String DATA_SET_ID = "datasetId";

    private static final String SUFFIX = "-A-";

    private final VoteDAO voteDAO;

    private final DACUserDAO dacUserDAO;

    private final DataSetDAO dataSetDAO;

    private static final String PATH = "template/RequestApplication.pdf";

    private final DataAccessReportsParser dataAccessReportsParser;

    private final DataAccessRequestService dataAccessRequestService;

    private final CounterService counterService;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     */
    public static void initInstance(CounterService counterService, DataAccessRequestService dataAccessRequestService, MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO, ResearcherPropertyDAO researcherPropertyDAO) {
        DataAccessRequestAPIHolder.setInstance(new DatabaseDataAccessRequestAPI(counterService, dataAccessRequestService, mongo, converter, electionDAO, consentDAO, voteDAO, dacUserDAO, dataSetDAO, researcherPropertyDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param mongo The Data Access Object used to read/write data.
     */
    protected DatabaseDataAccessRequestAPI(CounterService counterService, DataAccessRequestService dataAccessRequestService, MongoConsentDB mongo, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO, ResearcherPropertyDAO researcherPropertyDAO) {
        this.counterService = counterService;
        this.dataAccessRequestService = dataAccessRequestService;
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

    @Override
    public List<Document> createDataAccessRequest(Document dataAccessRequest) {
        List<Document> dataAccessList = new ArrayList<>();
        if (dataAccessRequest.containsKey(DarConstants.PARTIAL_DAR_CODE)){
            mongo.getPartialDataAccessRequestCollection().findOneAndDelete(new BasicDBObject(DarConstants.PARTIAL_DAR_CODE, dataAccessRequest.getString(DarConstants.PARTIAL_DAR_CODE)));
            dataAccessRequest.remove(DarConstants.ID);
            dataAccessRequest.remove(DarConstants.PARTIAL_DAR_CODE);
        }
        List<Integer> datasets =  DarUtil.getIntegerList(dataAccessRequest, DarConstants.DATASET_ID);
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
        return dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(id);
    }

    @Override
    public void deleteDataAccessRequestById(String id) {
        dataAccessRequestService.deleteByReferenceId(id);
    }


    @Override
    public Document describeDataAccessRequestFieldsById(String id, List<String> fields) {
        Document dar = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(id);
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
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * Find DARS related to the datasets sent as a parameter. Only dars with the use restriction
     * field present will be returned. DARs that require Manual Review wont be matched.
     * @param dataSetIds
     * @return A list of Data Access Requests.
     */
    @Override
    public List<Document> describeDataAccessWithDataSetIdAndRestriction(List<Integer> dataSetIds) {
        return dataAccessRequestService.getAllDataAccessRequestsAsDocuments().stream().
                filter(d -> !Collections.disjoint(dataSetIds, DarUtil.getIntegerList(d, DarConstants.DATASET_ID))).
                filter(d -> d.get(DarConstants.RESTRICTION) != null).
                collect(Collectors.toList());
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @param dataSetIds List<String>
     * @return List<Document>
     */
    @Override
    public List<Document> describeDataAccessWithDataSetId(List<String> dataSetIds) {
        return dataAccessRequestService.getAllDataAccessRequestsAsDocuments().stream().
                filter(d -> !Collections.disjoint(dataSetIds, DarUtil.getIntegerList(d, DarConstants.DATASET_ID))).
                collect(Collectors.toList());
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @param userId User id
     * @return List<String>
     */
    @Override
    public List<String> describeDataAccessIdsForOwner(Integer userId) {
        return dataAccessRequestService.getAllDataAccessRequestsAsDocuments().stream().
                filter(d -> d.getInteger(DarConstants.USER_ID).equals(userId)).
                map(d -> d.getString(DarConstants.REFERENCE_ID)).
                collect(Collectors.toList());
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @return List<Document>
     */
    @Override
    public List<Document> describeDataAccessRequests() {
        return  dataAccessRequestService.getAllDataAccessRequestsAsDocuments();
    }

    @Override
    public Collection<String> getDatasetsInDARs(Collection<String> dataAccessRequestIds) {
        return dataAccessRequestService.getDataAccessRequestsByReferenceIds(new ArrayList<>(dataAccessRequestIds)).
                stream().
                map(DataAccessRequest::getData).filter(Objects::nonNull).
                map(DataAccessRequestData::getDatasetDetail).filter(Objects::nonNull).
                flatMap(List::stream).filter(Objects::nonNull).
                map(DatasetDetailEntry::getDatasetId).filter(Objects::nonNull).
                distinct().
                collect(Collectors.toList());
    }


    @Override
    public UseRestriction createStructuredResearchPurpose(Document document) {
        DataUse dataUse = converter.parseDataUsePurpose(document.toJson());
        return converter.parseUseRestriction(dataUse);
    }

    @Override
    public void deleteDataAccessRequest(Document document) {
        dataAccessRequestService.deleteByReferenceId(document.getString(DarConstants.REFERENCE_ID));
    }

    @Override
    public Document updateDataAccessRequest(Document dataAccessRequest, String id) {
        if (dataAccessRequestService.findByReferenceId(id) == null) {
            throw new NotFoundException("Data access for the specified id does not exist");
        }
        Gson gson = new Gson();
        DataAccessRequestData darData = gson.fromJson(dataAccessRequest.toJson(), DataAccessRequestData.class);
        darData.setSortDate(new Date().getTime());
        dataAccessRequestService.updateByReferenceId(id, darData);
        return dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(id);
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
        partialDar.put(DarConstants.SORT_DATE, new Date().getTime());
        if (mongo.getPartialDataAccessRequestCollection().findOneAndReplace(query, partialDar) == null) {
            throw new NotFoundException("Partial Data access for the specified id does not exist");
        }
        return mongo.getPartialDataAccessRequestCollection().find(query).first();
    }

    @Override
    public Document createPartialDataAccessRequest(Document partialDar){
        String seq = counterService.getNextDarSequence();
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
    public Document cancelDataAccessRequest(String referenceId) {
        DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
        DataAccessRequestData darData = dar.getData();
        darData.setStatus(ElectionStatus.CANCELED.getValue());
        dataAccessRequestService.updateByReferenceId(referenceId, darData);
        return dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(referenceId);
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
            dacUsers =  dacUserDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
        }
        return dacUsers;
    }

    @Override
    public Object getField(String requestId , String field){
        Document dar = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(requestId);
        return dar != null ? dar.get(field) : null;
    }

    @Override
    public boolean hasUseRestriction(String referenceId){
        return getField(referenceId, DarConstants.RESTRICTION) != null ? true : false;
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @return List<UseRestrictionDTO>
     */
    @Override
    public List<UseRestrictionDTO> getInvalidDataAccessRequest() {
        List<Document> darList = dataAccessRequestService.getAllDataAccessRequestsAsDocuments().stream().
                filter(d -> !d.getBoolean(DarConstants.VALID_RESTRICTION)).
                collect(Collectors.toList());
        List<UseRestrictionDTO> invalidRestrictions = new ArrayList<>();
        darList.forEach(c->{
            invalidRestrictions.add(new UseRestrictionDTO(c.get(DarConstants.DAR_CODE, String.class),new Gson().toJson(c.get(DarConstants.RESTRICTION, Map.class))));
        });
        return invalidRestrictions;
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
    public byte[] createDARDocument(Document dar, Map<String, String> researcherProperties, DACUser user, Boolean manualReview, String sDUR) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PDDocument darDOC = new PDDocument();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream is = classLoader.getResourceAsStream(PATH);
            darDOC = PDDocument.load(is);
            new DataAccessParser().fillDARForm(dar, researcherProperties, user, manualReview, darDOC.getDocumentCatalog().getAcroForm(), sDUR);
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
        Election accessElection = electionDAO.findLastElectionByReferenceIdAndType(dar.getString(DarConstants.REFERENCE_ID), ElectionType.DATA_ACCESS.getValue());
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
                    String consentName = consentDAO.findConsentNameFromDatasetID(DarUtil.getIntegerList(dar, DarConstants.DATASET_ID).get(0));
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
                    String consentName = consentDAO.findConsentNameFromDatasetID(DarUtil.getIntegerList(dar, DarConstants.DATASET_ID).get(0));
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
                Date approvalDate = electionDAO.findApprovalAccessElectionDate(dar.getString(DarConstants.REFERENCE_ID));
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
    public DARModalDetailsDTO DARModalDetailsDTOBuilder(Document dar, DACUser dacUser, ElectionAPI electionApi) {
        DARModalDetailsDTO darModalDetailsDTO = new DARModalDetailsDTO();
        List<DataSet> datasets = populateDatasets(dar.get(DarConstants.DATASET_DETAIL));
        Optional<DACUser> optionalUser = Optional.ofNullable(dacUser);
        String status = optionalUser.isPresent() ? dacUser.getStatus() : "";
        String rationale = optionalUser.isPresent() ? dacUser.getRationale() : "";
        List<ResearcherProperty> researcherProperties = optionalUser.isPresent() ?
                researcherPropertyDAO.findResearcherPropertiesByUser(dacUser.getDacUserId()) :
                Collections.emptyList();
        return darModalDetailsDTO
            .setNeedDOApproval(electionApi.darDatasetElectionStatus((dar.getString(DarConstants.REFERENCE_ID))))
            .setResearcherName(dacUser, dar.getString(DarConstants.INVESTIGATOR))
            .setStatus(status)
            .setRationale(rationale)
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
            .setDatasets(datasets)
            .setResearcherProperties(researcherProperties)
            .setRus(dar.getString(DarConstants.RUS));
    }

    @SuppressWarnings("unchecked")
    private List<DataSet> populateDatasets(Object datasetDetails) {
        List<DataSet> datasets = new ArrayList<>();
        try {
            List<Integer> datasetIds = ((ArrayList<Document>) datasetDetails).
                    stream().
                    filter(d -> d.containsKey(DarConstants.DATASET_ID)).
                    map(d -> d.getString(DarConstants.DATASET_ID)).
                    map(Integer::valueOf).
                    collect(Collectors.toList());
            datasets.addAll(dataSetDAO.findDataSetsByIdList(datasetIds));
        } catch (Exception e) {
            logger().warn(e);
        }
        return datasets;
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @param dataSetId Dataset Id
     * @return List<Document>
     */
    private List<Document> describeDataAccessByDataSetId(Integer dataSetId) {
        return dataAccessRequestService.getAllDataAccessRequestsAsDocuments().stream().
                filter(d -> DarUtil.getIntegerList(d, DarConstants.DATASET_ID).contains(dataSetId)).
                collect(Collectors.toList());
    }

    private void insertDataAccess(List<Document> dataAccessRequestList) {
        if (CollectionUtils.isNotEmpty(dataAccessRequestList)) {
            String seq = counterService.getNextDarSequence();
            if (dataAccessRequestList.size() > 1) {
                IntStream.range(0, dataAccessRequestList.size())
                        .forEach(idx -> {
                                    dataAccessRequestList.get(idx).append(DarConstants.DAR_CODE, "DAR-" + seq + SUFFIX + idx);
                                    dataAccessRequestList.get(idx).remove(DarConstants.ID);
                                    if (dataAccessRequestList.get(idx).get(DarConstants.PARTIAL_DAR_CODE) != null) {
                                        BasicDBObject query = new BasicDBObject(DarConstants.PARTIAL_DAR_CODE, dataAccessRequestList.get(idx).get(DarConstants.PARTIAL_DAR_CODE));
                                        mongo.getPartialDataAccessRequestCollection().findOneAndDelete(query);
                                        dataAccessRequestList.get(idx).remove(DarConstants.PARTIAL_DAR_CODE);
                                    }
                                }

                        );
            } else {
                dataAccessRequestList.get(0).append(DarConstants.DAR_CODE, "DAR-" + seq);
            }
            Gson gson = new Gson();
            dataAccessRequestList.forEach(d -> {
                String referenceId = d.getString(DarConstants.REFERENCE_ID);
                if (referenceId == null) {
                    referenceId = UUID.randomUUID().toString();
                }
                DataAccessRequestData darData = DataAccessRequestData.fromString(gson.toJson(d));
                darData.setReferenceId(referenceId);
                d.put(DarConstants.REFERENCE_ID, referenceId);
                if (darData.getCreateDate() == null) {
                    darData.setCreateDate(new Date().getTime());
                }
                dataAccessRequestService.insertDataAccessRequest(referenceId, darData);
            });
        }
    }

    private List getRequestIds(FindIterable<Document> access) {
        List<String> accessIds = new ArrayList<>();
        if (access != null) {
            access.forEach((Block<Document>) document ->
                accessIds.add(document.getString(DarConstants.REFERENCE_ID))
            );
        }
        return accessIds;
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

