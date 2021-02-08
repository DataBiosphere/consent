package org.broadinstitute.consent.http.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Use DataAccessRequestService
 * Implementation class for DatabaseDataAccessRequestAPI.
 */
@Deprecated
public class DatabaseDataAccessRequestAPI extends AbstractDataAccessRequestAPI {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UseRestrictionConverter converter;

    private final ElectionDAO electionDAO;

    private final ConsentDAO consentDAO;

    private final UserPropertyDAO userPropertyDAO;

    private final VoteDAO voteDAO;

    private final UserDAO userDAO;

    private final DataSetDAO dataSetDAO;

    private static final String PATH = "template/RequestApplication.pdf";

    private final DataAccessReportsParser dataAccessReportsParser;

    private final DataAccessRequestService dataAccessRequestService;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     */
    public static void initInstance(DataAccessRequestService dataAccessRequestService, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, UserDAO userDAO, DataSetDAO dataSetDAO, UserPropertyDAO userPropertyDAO) {
        DataAccessRequestAPIHolder.setInstance(new DatabaseDataAccessRequestAPI(dataAccessRequestService, converter, electionDAO, consentDAO, voteDAO, userDAO, dataSetDAO,
            userPropertyDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     */
    protected DatabaseDataAccessRequestAPI(DataAccessRequestService dataAccessRequestService, UseRestrictionConverter converter, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, UserDAO userDAO, DataSetDAO dataSetDAO, UserPropertyDAO userPropertyDAO) {
        this.dataAccessRequestService = dataAccessRequestService;
        this.converter = converter;
        this.electionDAO = electionDAO;
        this.consentDAO = consentDAO;
        this.voteDAO = voteDAO;
        this.userDAO = userDAO;
        this.dataSetDAO = dataSetDAO;
        this.dataAccessReportsParser = new DataAccessReportsParser();
        this.userPropertyDAO = userPropertyDAO;
    }

    @Override
    public Document describeDataAccessRequestById(String id) {
        return dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(id);
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

    @Override
    public UseRestriction createStructuredResearchPurpose(Document document) {
        DataUse dataUse = converter.parseDataUsePurpose(document.toJson());
        return converter.parseUseRestriction(dataUse);
    }

    @Override
    public List<Document> describeDraftDataAccessRequestManage(Integer userId) {
        List<Document> accessList = userId == null
                ? dataAccessRequestService.findAllDraftDataAccessRequestsAsDocuments()
                : dataAccessRequestService.findAllDraftDataAccessRequestDocumentsByUser(userId);
        List<Document> darManage = new ArrayList<>();
        List<String> accessRequestIds = getRequestIds(accessList);
        if (CollectionUtils.isNotEmpty(accessRequestIds)){
            for(Document doc: accessList){
                doc.append("dataRequestId", doc.get(DarConstants.REFERENCE_ID).toString());
                darManage.add(doc);
            }
        }
        return darManage;
    }

    @Override
    public List<User> getUserEmailAndCancelElection(String referenceId) {
        Election access = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(referenceId, ElectionType.DATA_ACCESS.getValue());
        Election rp = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(referenceId, ElectionType.RP.getValue());
        updateElection(access, rp);
        List<User> users = new ArrayList<>();
        if (access != null){
            List<Vote> votes = voteDAO.findDACVotesByElectionId(access.getElectionId());
            List<Integer> userIds = votes.stream().map(Vote::getDacUserId).collect(Collectors.toList());
            users.addAll(userDAO.findUsers(userIds));
        } else {
            users =  userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
        }
        return users;
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
    public byte[] createDARDocument(Document dar, Map<String, String> researcherProperties, User user, Boolean manualReview, String sDUR) throws IOException {
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
        String consentId = dataSetDAO.getAssociatedConsentIdByDataSetId(dataSetId.get(0));
        return consentDAO.findConsentById(consentId).getTranslatedUseRestriction();
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
                try {
                    if (dar != null) {
                        Integer datasetId = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID).get(0);
                        String consentId = dataSetDAO.getAssociatedConsentIdByDataSetId(datasetId);
                        Consent consent = consentDAO.findConsentById(consentId);
                        String profileName = userPropertyDAO.findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.PROFILE_NAME);
                        String institution = userPropertyDAO.findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.INSTITUTION);
                        dataAccessReportsParser.addApprovedDARLine(darWriter, election, dar, profileName, institution, consent.getName(), consent.getTranslatedUseRestriction());
                    }
                } catch (Exception e) {
                    logger.error("Exception generating Approved DAR Document", e);
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
                    Integer datasetId = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID).get(0);
                    String consentId = dataSetDAO.getAssociatedConsentIdByDataSetId(datasetId);
                    Consent consent = consentDAO.findConsentById(consentId);
                    dataAccessReportsParser.addReviewedDARLine(darWriter, election, dar, consent.getName(), consent.getTranslatedUseRestriction());
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
                    String email = userPropertyDAO
                        .findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.ACADEMIC_BUSINESS_EMAIL);
                    String name = userPropertyDAO
                        .findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.PROFILE_NAME);
                    String institution = userPropertyDAO
                        .findPropertyValueByPK(dar.getInteger(DarConstants.USER_ID), DarConstants.INSTITUTION);
                    String darCode = dar.getString(DarConstants.DAR_CODE);
                    dataAccessReportsParser.addDataSetApprovedUsersLine(darWriter, email, name, institution, darCode, approvalDate);
                }
            }
        }
        darWriter.flush();
        return file;
    }

    @Override
    public DARModalDetailsDTO DARModalDetailsDTOBuilder(Document dar, User user, ElectionAPI electionApi) {
        DARModalDetailsDTO darModalDetailsDTO = new DARModalDetailsDTO();
        List<DataSet> datasets = populateDatasets(dar);
        Optional<User> optionalUser = Optional.ofNullable(user);
        String status = optionalUser.isPresent() ? user.getStatus() : "";
        String rationale = optionalUser.isPresent() ? user.getRationale() : "";
        List<UserProperty> researcherProperties = optionalUser.isPresent() ?
                userPropertyDAO.findResearcherPropertiesByUser(user.getDacUserId()) :
                Collections.emptyList();
        return darModalDetailsDTO
            .setNeedDOApproval(electionApi.darDatasetElectionStatus((dar.getString(DarConstants.REFERENCE_ID))))
            .setResearcherName(user, dar.getString(DarConstants.INVESTIGATOR))
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

    private List<DataSet> populateDatasets(Document dar) {
        List<DataSet> datasets = new ArrayList<>();
        try {
            List<Integer> datasetIds = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
            if (!datasetIds.isEmpty()) {
                datasets.addAll(dataSetDAO.findDataSetsByIdList(datasetIds));
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
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

    private List<String> getRequestIds(List<Document> access) {
        List<String> accessIds = new ArrayList<>();
        if (access != null) {
            access.forEach(document ->
                accessIds.add(document.getString(DarConstants.REFERENCE_ID))
            );
        }
        return accessIds;
    }

    protected List<UserProperty> updateResearcherIdentification(Document dataAccessRequest) {
        Integer userId = dataAccessRequest.getInteger(DarConstants.USER_ID);
        String linkedIn = dataAccessRequest.getString(ResearcherFields.LINKEDIN_PROFILE.getValue());
        String orcId = dataAccessRequest.getString(ResearcherFields.ORCID.getValue());
        String researcherGate = dataAccessRequest.getString(ResearcherFields.RESEARCHER_GATE.getValue());
        List<UserProperty> rpList = new ArrayList<>();
        userPropertyDAO.deletePropertyByUser(Arrays.asList(ResearcherFields.LINKEDIN_PROFILE.getValue(), ResearcherFields.ORCID.getValue(), ResearcherFields.RESEARCHER_GATE.getValue()), userId);
        if (StringUtils.isNotEmpty(linkedIn)) {
          rpList.add(new UserProperty(userId, ResearcherFields.LINKEDIN_PROFILE.getValue(), linkedIn));
        }
        if (StringUtils.isNotEmpty(orcId)) {
          rpList.add(new UserProperty(userId, ResearcherFields.ORCID.getValue(), orcId));
        }
        if (StringUtils.isNotEmpty(researcherGate)) {
           rpList.add(new UserProperty(userId, ResearcherFields.RESEARCHER_GATE.getValue(), researcherGate));
        }
        if (CollectionUtils.isNotEmpty(rpList)) {
           userPropertyDAO.insertAll(rpList);
        }
        return rpList;
    }

}

