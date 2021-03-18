package org.broadinstitute.consent.http.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.darsummary.DARModalDetailsDTO;
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

    private final ElectionDAO electionDAO;

    private final ConsentDAO consentDAO;

    private final UserPropertyDAO userPropertyDAO;

    private final VoteDAO voteDAO;

    private final UserDAO userDAO;

    private final DataSetDAO dataSetDAO;

    private final DataAccessReportsParser dataAccessReportsParser;

    private final DataAccessRequestService dataAccessRequestService;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     */
    public static void initInstance(DataAccessRequestService dataAccessRequestService, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, UserDAO userDAO, DataSetDAO dataSetDAO, UserPropertyDAO userPropertyDAO) {
        DataAccessRequestAPIHolder.setInstance(new DatabaseDataAccessRequestAPI(dataAccessRequestService, electionDAO, consentDAO, voteDAO, userDAO, dataSetDAO,
            userPropertyDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     */
    protected DatabaseDataAccessRequestAPI(DataAccessRequestService dataAccessRequestService, ElectionDAO electionDAO, ConsentDAO consentDAO, VoteDAO voteDAO, UserDAO userDAO, DataSetDAO dataSetDAO, UserPropertyDAO userPropertyDAO) {
        this.dataAccessRequestService = dataAccessRequestService;
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
        return dataAccessRequestService.findAllDataAccessRequests().stream().
            filter(d -> !Collections.disjoint(dataSetIds, d.getData().getDatasetIds())).
            map(dataAccessRequestService::createDocumentFromDar).
            collect(Collectors.toList());
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
    public File createApprovedDARDocument() throws IOException {
        List<Election> elections = electionDAO.findDataAccessClosedElectionsByFinalResult(true);
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        FileWriter darWriter = new FileWriter(file);
        dataAccessReportsParser.setApprovedDARHeader(darWriter);
        if (CollectionUtils.isNotEmpty(elections)) {
            for (Election election : elections) {
                Document dar = describeDataAccessRequestById(election.getReferenceId());
                DataAccessRequest dataAccessRequest = dataAccessRequestService.findByReferenceId(election.getReferenceId());
                try {
                    if (dar != null) {
                        Integer datasetId = dataAccessRequest.getData().getDatasetIds().get(0);
                        String consentId = dataSetDAO.getAssociatedConsentIdByDataSetId(datasetId);
                        Consent consent = consentDAO.findConsentById(consentId);
                        String profileName = userPropertyDAO.findPropertyValueByPK(dataAccessRequest.getUserId(), DarConstants.PROFILE_NAME);
                        String institution = userPropertyDAO.findPropertyValueByPK(dataAccessRequest.getUserId(), DarConstants.INSTITUTION);
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
                String referenceId = dar.getString(DarConstants.REFERENCE_ID);
                DataAccessRequest dataAccessRequest = dataAccessRequestService.findByReferenceId(referenceId);
                Date approvalDate = electionDAO.findApprovalAccessElectionDate(referenceId);
                if (approvalDate != null) {
                    String email = userPropertyDAO
                        .findPropertyValueByPK(dataAccessRequest.getUserId(), DarConstants.ACADEMIC_BUSINESS_EMAIL);
                    String name = userPropertyDAO
                        .findPropertyValueByPK(dataAccessRequest.getUserId(), DarConstants.PROFILE_NAME);
                    String institution = userPropertyDAO
                        .findPropertyValueByPK(dataAccessRequest.getUserId(), DarConstants.INSTITUTION);
                    String darCode = dataAccessRequest.getData().getDarCode();
                    dataAccessReportsParser.addDataSetApprovedUsersLine(darWriter, email, name, institution, darCode, approvalDate);
                }
            }
        }
        darWriter.flush();
        return file;
    }

    @Override
    public DARModalDetailsDTO DARModalDetailsDTOBuilder(Document dar, User user, ElectionAPI electionApi) {
        DataAccessRequest dataAccessRequest = dataAccessRequestService.findByReferenceId(dar.getString(DarConstants.REFERENCE_ID));
        DARModalDetailsDTO darModalDetailsDTO = new DARModalDetailsDTO();
        List<DataSet> datasets = populateDatasets(dar);
        Optional<User> optionalUser = Optional.ofNullable(user);
        String status = optionalUser.isPresent() ? user.getStatus() : "";
        String rationale = optionalUser.isPresent() ? user.getRationale() : "";
        List<UserProperty> researcherProperties = optionalUser.isPresent() ?
                userPropertyDAO.findResearcherPropertiesByUser(user.getDacUserId()) :
                Collections.emptyList();
        return darModalDetailsDTO
            .setNeedDOApproval(electionApi.darDatasetElectionStatus(dataAccessRequest.getReferenceId()))
            .setResearcherName(user, dataAccessRequest.getData().getInvestigator())
            .setStatus(status)
            .setRationale(rationale)
            .setUserId(dataAccessRequest.getUserId())
            .setDarCode(dataAccessRequest.getData().getDarCode())
            .setPrincipalInvestigator(dar.getString(DarConstants.INVESTIGATOR))
            .setInstitutionName(dar.getString(DarConstants.INSTITUTION))
            .setProjectTitle(dataAccessRequest.getData().getProjectTitle())
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

}
