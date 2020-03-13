package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DataAccessRequestService {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private DACUserDAO dacUserDAO;
    private DataAccessRequestDAO dataAccessRequestDAO;
    private DataSetDAO dataSetDAO;
    private ElectionDAO electionDAO;
    private MongoConsentDB mongo;
    private DacService dacService;

    private static final String UN_REVIEWED = "un-reviewed";
    private static final String NEEDS_APPROVAL = "Needs Approval";
    private static final String APPROVED = "Approved";
    private static final String DENIED = "Denied";

    @Inject
    public DataAccessRequestService(DACUserDAO dacUserDAO, DataAccessRequestDAO dataAccessRequestDAO,
                                    DataSetDAO dataSetDAO, ElectionDAO electionDAO, MongoConsentDB mongo,
                                    DacService dacService) {
        this.dacUserDAO = dacUserDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.mongo = mongo;
        this.dacService = dacService;
    }

    /**
     * Get a count of data access requests that do not have an election
     *
     * @param authUser AuthUser
     * @return Count of user-accessible DARs with no election.
     */
    public Integer getTotalUnReviewedDars(AuthUser authUser) {
        List<String> unReviewedDarIds = getUnReviewedDarsForUser(authUser).
                stream().
                map(d -> d.get(DarConstants.ID).toString()).
                collect(Collectors.toList());
        Integer unReviewedDarCount = 0;
        if (!unReviewedDarIds.isEmpty()) {
            List<String> electionReferenceIds = electionDAO.
                    findLastElectionsWithFinalVoteByReferenceIdsAndType(unReviewedDarIds, ElectionType.DATA_ACCESS.getValue()).
                    stream().
                    map(Election::getReferenceId).
                    collect(Collectors.toList());
            for (String darId : unReviewedDarIds) {
                if (!electionReferenceIds.contains(darId)) {
                    ++unReviewedDarCount;
                }
            }
        }
        return unReviewedDarCount;
    }

    /**
     * Filter DataAccessRequestManage objects on user.
     *
     * @param userId Optional UserId. If provided, filter list of DARs on this user.
     * @param authUser Required if no user id is provided. Instead, filter on what DACs the auth
     *                 user has access to.
     * @return List of DataAccessRequestManage objects
     */
    public List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId, AuthUser authUser) {
        List<Document> filteredAccessList;
        List<Document> allDars = getAllDataAccessRequestsAsDocuments();
        if (userId == null) {
            filteredAccessList = dacService.filterDarsByDAC(allDars, authUser);
        } else {
            filteredAccessList = allDars.stream().
                    filter(d -> d.getInteger(DarConstants.USER_ID).equals(userId)).
                    collect(Collectors.toList());
        }
        filteredAccessList.sort((a, b) -> b.getInteger(DarConstants.SORT_DATE) - a.getInteger(DarConstants.SORT_DATE));
        List<DataAccessRequestManage> darManage = new ArrayList<>();
        List<String> accessRequestIds = filteredAccessList.
                stream().
                map(d -> d.get(DarConstants.ID).toString()).
                distinct().
                collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(accessRequestIds)) {
            List<Election> electionList = electionDAO.
                    findLastElectionsWithFinalVoteByReferenceIdsAndType(accessRequestIds, ElectionType.DATA_ACCESS.getValue());
            Map<String, Election> electionAccessMap = createAccessRequestElectionMap(electionList);
            darManage.addAll(createAccessRequestManage(filteredAccessList, electionAccessMap));
        }
        return darManage;
    }

    private Map<String, Election> createAccessRequestElectionMap(List<Election> elections) {
        Map<String, Election> electionMap = new HashMap<>();
        elections.forEach(election -> electionMap.put(election.getReferenceId(), election));
        return electionMap;
    }

    /**
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     */
    @SuppressWarnings("deprecation")
    public List<Document> getAllMongoDataAccessRequests() {
        return mongo.getDataAccessRequestCollection().find().into(new ArrayList<>());
    }

    /**
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     */
    public List<DataAccessRequest> getAllPostgresDataAccessRequests() {
        return dataAccessRequestDAO.findAll();
    }

    /**
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     * Replacement for MongoConsentDB.getDataAccessRequestCollection()
     * @return List of all DataAccessRequestData objects as Documents
     */
    public List<Document> getAllDataAccessRequestsAsDocuments() {
        Gson gson = new Gson();
        return getAllPostgresDataAccessRequests().stream().
                map(d -> {
                    Document document = Document.parse(gson.toJson(d.getData()));
                    document.put(DarConstants.DATA_ACCESS_REQUEST_ID, d.getId());
                    document.put(DarConstants.ID, d.getReferenceId());
                    document.put(DarConstants.REFERENCE_ID, d.getReferenceId());
                    return document;
                }).
                collect(Collectors.toList());
    }

    /**
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     * Replacement for MongoConsentDB.getDataAccessRequestCollection().find(ObjectId)
     * @return DataAccessRequestData object as Document
     */
    public Document getDataAccessRequestByReferenceIdAsDocument(String referenceId) {
        DataAccessRequest d = dataAccessRequestDAO.findByReferenceId(referenceId);
        if (d == null) {
            throw new NotFoundException("Unable to find Data Access Request by reference id: " + referenceId);
        }
        return createDocumentFromDar(d);
    }

    /**
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     * Replacement for MongoConsentDB.getDataAccessRequestCollection().find(ObjectId)
     * @return DataAccessRequestData object as Document
     */
    public List<Document> getDataAccessRequestsByReferenceIdsAsDocuments(List<String> referenceIds) {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findByReferenceIds(referenceIds);
        return dars.stream().map(this::createDocumentFromDar).collect(Collectors.toList());
    }

    private Document createDocumentFromDar(DataAccessRequest d) {
        Gson gson = new Gson();
        Document document = Document.parse(gson.toJson(d.getData()));
        document.put(DarConstants.DATA_ACCESS_REQUEST_ID, d.getId());
        document.put(DarConstants.ID, d.getReferenceId());
        document.put(DarConstants.REFERENCE_ID, d.getReferenceId());
        return document;
    }

    public void deleteByReferenceId(String referenceId) {
        dataAccessRequestDAO.deleteByReferenceId(referenceId);
    }

    public DataAccessRequest findByReferenceId(String referencedId) {
        return dataAccessRequestDAO.findByReferenceId(referencedId);
    }

    public DataAccessRequest updateByReferenceId(String referencedId, DataAccessRequestData darData) {
        dataAccessRequestDAO.updateDataByReferenceId(referencedId, darData.toString());
        return findByReferenceId(referencedId);
    }

    /**
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     */
    public Document updateDocumentByReferenceId(String id, Document document) {
        if (findByReferenceId(id) == null) {
            throw new NotFoundException("Data access for the specified id does not exist");
        }
        Gson gson = new Gson();
        DataAccessRequestData darData = gson.fromJson(document.toJson(), DataAccessRequestData.class);
        darData.setSortDate(new Date().getTime());
        updateByReferenceId(id, darData);
        return getDataAccessRequestByReferenceIdAsDocument(id);
    }

    public DataAccessRequest insertDataAccessRequest(String referencedId, DataAccessRequestData darData) {
        dataAccessRequestDAO.insert(referencedId, darData.toString());
        return findByReferenceId(referencedId);
    }

    public List<Document> describeDataAccessRequests(AuthUser authUser) {
        List<Document> documents = getAllDataAccessRequestsAsDocuments();
        return dacService.filterDarsByDAC(documents, authUser);
    }

    private List<DataAccessRequestManage> createAccessRequestManage(List<Document> documents, Map<String, Election> electionList) {
        List<DataAccessRequestManage> requestsManage = new ArrayList<>();
        Map<Integer, Integer> datasetDacPairs = dataSetDAO.findDatasetAndDacIds().
                stream().
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        documents.forEach(dar -> {
            DataAccessRequestManage darManage = new DataAccessRequestManage();
            ObjectId id = dar.get(DarConstants.ID, ObjectId.class);
            List<Integer> darDatasetIds = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
            List<DataSet> dataSetsToApprove = dataSetDAO.findNeedsApprovalDataSetByDataSetId(darDatasetIds);
            if (darDatasetIds.size() > 1) {
                darManage.addError("DAR has more than one dataset association: " + ArrayUtils.toString(darDatasetIds));
            }
            if (darDatasetIds.size() == 1) {
                darManage.setDatasetId(darDatasetIds.get(0));
            }
            if (datasetDacPairs.containsKey(darManage.getDatasetId())) {
                darManage.setDacId(datasetDacPairs.get(darManage.getDatasetId()));
            }
            Election election = electionList.get(id.toString());
            darManage.setCreateDate(new Timestamp((long) id.getTimestamp() * 1000));
            darManage.setRus(dar.getString(DarConstants.RUS));
            darManage.setProjectTitle(dar.getString(DarConstants.PROJECT_TITLE));
            darManage.setDataRequestId(id.toString());
            darManage.setFrontEndId(dar.get(DarConstants.DAR_CODE).toString());
            darManage.setSortDate(dar.getDate("sortDate"));
            darManage.setIsCanceled(dar.containsKey(DarConstants.STATUS) && dar.get(DarConstants.STATUS).equals(ElectionStatus.CANCELED.getValue()));
            darManage.setNeedsApproval(CollectionUtils.isNotEmpty(dataSetsToApprove));
            darManage.setDataSetElectionResult(darManage.getNeedsApproval() ? NEEDS_APPROVAL : "");
            darManage.setElectionStatus(election == null ? UN_REVIEWED : election.getStatus());
            darManage.setElectionId(election != null ? election.getElectionId() : null);
            darManage.setElectionVote(election != null ? election.getFinalVote() : null);
            if (election != null && !CollectionUtils.isEmpty(electionDAO.getElectionByTypeStatusAndReferenceId(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue(), election.getReferenceId()))) {
                darManage.setElectionStatus(ElectionStatus.PENDING_APPROVAL.getValue());
            } else if (CollectionUtils.isNotEmpty(dataSetsToApprove) && election != null && election.getStatus().equals(ElectionStatus.CLOSED.getValue())) {
                List<String> referenceList = Collections.singletonList(election.getReferenceId());
                List<Election> datasetElections = electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(referenceList, ElectionType.DATA_SET.getValue());
                darManage.setDataSetElectionResult(consolidateDataSetElectionsResult(datasetElections));
            }
            darManage.setOwnerUser(getOwnerUser(dar.get(DarConstants.USER_ID)).orElse(null));
            if (darManage.getOwnerUser() == null) {
                logger.error("DAR: " + darManage.getFrontEndId() + " has an invalid owner");
            }
            requestsManage.add(darManage);
        });
        return requestsManage;
    }

    private Optional<DACUser> getOwnerUser(Object dacUserId) {
        try {
            Integer userId = Integer.valueOf(dacUserId.toString());
            Set<DACUser> users = dacUserDAO.findUsersWithRoles(Collections.singletonList(userId));
            return users.stream().findFirst();
        } catch (Exception e) {
            logger.error("Unable to determine user for dacUserId: " + dacUserId.toString() + "; " + e.getMessage());
        }
        return Optional.empty();
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

    private List<Document> getUnReviewedDarsForUser(AuthUser authUser) {
        List<Document> activeDars = getAllDataAccessRequestsAsDocuments().stream().
                filter(d -> !d.getString(DarConstants.STATUS).equalsIgnoreCase(ElectionStatus.CANCELED.getValue())).
                collect(Collectors.toList());
        if (dacService.isAuthUserAdmin(authUser)) {
            return activeDars;
        }
        List<Integer> dataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());
        return activeDars.stream().
                filter(d -> !Collections.disjoint(DarUtil.getIntegerList(d, DarConstants.DATASET_ID), dataSetIds)).
                collect(Collectors.toList());
    }

}
