package org.broadinstitute.consent.http.service;

import static java.util.stream.Collectors.toList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
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

@SuppressWarnings("UnusedReturnValue")
public class DataAccessRequestService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ConsentDAO consentDAO;
    private final CounterService counterService;
    private final DacDAO dacDAO;
    private final DatasetDAO dataSetDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final ElectionDAO electionDAO;
    private final MatchDAO matchDAO;
    private final UserDAO userDAO;
    private final VoteDAO voteDAO;
    private final InstitutionDAO institutionDAO;
    private final UserPropertyDAO userPropertyDAO;

    private final DacService dacService;
    private final UserService userService;
    private final DataAccessReportsParser dataAccessReportsParser;

    private static final Gson gson = new GsonBuilder().setDateFormat("MMM d, yyyy").create();
    private static final String UN_REVIEWED = "un-reviewed";
    private static final String NEEDS_APPROVAL = "Needs Approval";
    private static final String APPROVED = "Approved";
    private static final String DENIED = "Denied";
    private static final String SUFFIX = "-A-";

    @Inject
    public DataAccessRequestService(CounterService counterService, DAOContainer container,
            DacService dacService, UserService userService) {
        this.consentDAO = container.getConsentDAO();
        this.counterService = counterService;
        this.dacDAO = container.getDacDAO();
        this.dataAccessRequestDAO = container.getDataAccessRequestDAO();
        this.dataSetDAO = container.getDatasetDAO();
        this.electionDAO = container.getElectionDAO();
        this.matchDAO = container.getMatchDAO();
        this.userDAO = container.getUserDAO();
        this.voteDAO = container.getVoteDAO();
        this.institutionDAO = container.getInstitutionDAO();
        this.dacService = dacService;
        this.userService = userService;
        this.userPropertyDAO = container.getResearcherPropertyDAO();
        this.dataAccessReportsParser = new DataAccessReportsParser();
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
                map(d -> d.getReferenceId()).
                collect(toList());
        Integer unReviewedDarCount = 0;
        if (!unReviewedDarIds.isEmpty()) {
            List<String> electionReferenceIds = electionDAO.
                    findLastElectionsWithFinalVoteByReferenceIdsAndType(unReviewedDarIds, ElectionType.DATA_ACCESS.getValue()).
                    stream().
                    map(Election::getReferenceId).
                    collect(toList());
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
     * @param user Filter on what DARs the user has access to.
     * @return List of DataAccessRequestManage objects
     */
    public List<DataAccessRequestManage> describeDataAccessRequestManageV2(User user, String roleName) {
        if (Objects.nonNull(roleName) && Objects.nonNull(user)) {
            if (roleName.equalsIgnoreCase(UserRoles.SIGNINGOFFICIAL.getRoleName())) {
                if (Objects.nonNull(user.getInstitutionId())) {
                    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequestsForInstitution(user.getInstitutionId());
                    List<DataAccessRequest> openDars = filterOutCanceledDars(dars);
                    return createAccessRequestManageV2(openDars);
                } else {
                    throw new NotFoundException("Signing Official (user: " + user.getDisplayName() + ") "
                      + "is not associated with an Institution.");
                }
            }
        }
        //if there is no roleName then user is a member, chair, or admin
        List<DataAccessRequest> allDars = findAllDataAccessRequests();
        List<DataAccessRequest> filteredAccessList = dacService.filterDataAccessRequestsByDac(allDars, user);
        List<DataAccessRequest> openDarList = filterOutCanceledDars(filteredAccessList);
        openDarList.sort(sortTimeComparator());
        if (CollectionUtils.isNotEmpty(openDarList)) {
            return createAccessRequestManageV2(openDarList);
        }
        return Collections.emptyList();
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * Filter DataAccessRequestManage objects on user.
     *
     * @param userId   Optional UserId. If provided, filter list of DARs on this user.
     * @param authUser Required if no user id is provided. Instead, filter on what DACs the auth
     *                 user has access to.
     * @return List of DataAccessRequestManage objects
     */
    @Deprecated // Use describeDataAccessRequestManageV2
    public List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId, AuthUser authUser) {
        List<DataAccessRequest> filteredAccessList;
        List<DataAccessRequest> allDars = findAllDataAccessRequests();
        if (userId == null) {
            User user = userDAO.findUserByEmail(authUser.getName());
            filteredAccessList = dacService.filterDataAccessRequestsByDac(allDars, user);
        } else {
            filteredAccessList = allDars.stream().
                    filter(d -> d.getUserId() != null).
                    filter(d -> d.getUserId().equals(userId)).
                    collect(Collectors.toList());
        }
        filteredAccessList.sort(sortTimeComparator());
        List<DataAccessRequestManage> darManage = new ArrayList<>();
        List<String> accessRequestIds = filteredAccessList.
                stream().
                map(DataAccessRequest::getReferenceId).
                distinct().
                collect(toList());
        if (CollectionUtils.isNotEmpty(accessRequestIds)) {
            List<Election> electionList = electionDAO.
                    findLastElectionsWithFinalVoteByReferenceIdsAndType(accessRequestIds, ElectionType.DATA_ACCESS.getValue());
            Map<String, Election> electionAccessMap = createAccessRequestElectionMap(electionList);
            darManage.addAll(createAccessRequestManage(filteredAccessList, electionAccessMap, authUser));
        }
        return darManage;
    }

    /**
     * Compare DataAccessRequest sort time long values, descending order.
     *
     * @return Comparator
     */
    private Comparator<DataAccessRequest> sortTimeComparator() {
        return (a, b) -> {
            Long aTime = a.getData().getSortDate();
            Long bTime = b.getData().getSortDate();
            if (aTime == null) {
                aTime = new Date().getTime();
            }
            if (bTime == null) {
                bTime = new Date().getTime();
            }
            return bTime.compareTo(aTime);
        };
    }

    private Map<String, Election> createAccessRequestElectionMap(List<Election> elections) {
        Map<String, Election> electionMap = new HashMap<>();
        elections.forEach(election -> electionMap.put(election.getReferenceId(), election));
        return electionMap;
    }

    public List<DataAccessRequest> findAllDataAccessRequests() {
        return dataAccessRequestDAO.findAllDataAccessRequests();
    }

    public List<DataAccessRequest> findAllDraftDataAccessRequests() {
        return dataAccessRequestDAO.findAllDraftDataAccessRequests();
    }

    @Deprecated //instead use findAllDraftDataAccessRequestByUser
    public List<Document> findAllDraftDataAccessRequestDocumentsByUser(Integer userId) {
        return dataAccessRequestDAO.findAllDraftsByUserId(userId).stream().
                map(this::createDocumentFromDar).
                collect(Collectors.toList());
    }

    public List<DataAccessRequest> findAllDraftDataAccessRequestsByUser(Integer userId) {
        return dataAccessRequestDAO.findAllDraftsByUserId(userId);
    }

    /**
     *
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     * Replacement for MongoConsentDB.getDataAccessRequestCollection().find(ObjectId)
     *
     * @return DataAccessRequestData object as Document
     */
    @Deprecated //instead use findByReferenceId
    public Document getDataAccessRequestByReferenceIdAsDocument(String referenceId) {
        DataAccessRequest d = dataAccessRequestDAO.findByReferenceId(referenceId);
        if (d == null) {
            throw new NotFoundException("Unable to find Data Access Request by reference id: " + referenceId);
        }
        return createDocumentFromDar(d);
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-604
     *
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     *
     * @return DataAccessRequestData object as Document
     */
    @Deprecated //instead use getDataAccessRequestsByReferenceIds
    public List<Document> getDataAccessRequestsByReferenceIdsAsDocuments(List<String> referenceIds) {
        return getDataAccessRequestsByReferenceIds(referenceIds).
                stream().
                map(this::createDocumentFromDar).
                collect(Collectors.toList());
    }

    public List<DataAccessRequest> getDataAccessRequestsByReferenceIds(List<String> referenceIds) {
        return dataAccessRequestDAO.findByReferenceIds(referenceIds);
    }

    @Deprecated
    public Document createDocumentFromDar(DataAccessRequest d) {
        Document document = Document.parse(gson.toJson(d.getData()));
        document.put(DarConstants.DATA_ACCESS_REQUEST_ID, d.getId());
        document.put(DarConstants.REFERENCE_ID, d.getReferenceId());
        document.put(DarConstants.CREATE_DATE, d.getCreateDate());
        document.put(DarConstants.SORT_DATE, d.getSortDate());
        return document;
    }

    public void deleteByReferenceId(String referenceId) throws NotAcceptableException {
        List<Election> elections = electionDAO.findElectionsByReferenceId(referenceId);
        if (Objects.isNull(elections) || elections.isEmpty()) {
            matchDAO.deleteMatchesByPurposeId(referenceId);
            dataAccessRequestDAO.deleteByReferenceId(referenceId);
        } else {
            String message = String.format("Unable to delete DAR: '%s', there are existing elections that reference it.", referenceId);
            logger.warn(message);
            throw new NotAcceptableException(message);
        }
    }

    public DataAccessRequest findByReferenceId(String referencedId) {
        DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referencedId);
        if (Objects.isNull(dar)) {
            throw new NotFoundException("There does not exist a DAR with the given reference Id");
        }
        return dar;
    }

    @Deprecated // Use updateByReferenceIdVersion2
    public DataAccessRequest updateByReferenceId(String referencedId, DataAccessRequestData darData) {
        darData.setSortDate(new Date().getTime());
        dataAccessRequestDAO.updateDataByReferenceId(referencedId, darData);
        return findByReferenceId(referencedId);
    }

    public DataAccessRequest insertDraftDataAccessRequest(User user, DataAccessRequest dar) {
        if (Objects.isNull(user) || Objects.isNull(dar) || Objects.isNull(dar.getReferenceId()) || Objects.isNull(dar.getData())) {
            throw new IllegalArgumentException("User and DataAccessRequest are required");
        }
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        dar.getData().setPartialDarCode(DataAccessRequestData.partialDarCodePrefix + sdf.format(now));
        dataAccessRequestDAO.insertVersion2(
            dar.getReferenceId(),
            user.getDacUserId(),
            now,
            now,
            null,
            now,
            dar.getData()
        );
        dataAccessRequestDAO.updateDraftByReferenceId(dar.getReferenceId(), true);
        return findByReferenceId(dar.getReferenceId());
    }

    /**
     *
     * @param user User
     * @return List<DataAccessRequest>
     */
    public List<DataAccessRequest> getDataAccessRequestsByUserRole(User user) {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
        return dacService.filterDataAccessRequestsByDac(dars, user);
    }

    /**
     * Cancel a Data Access Request
     *
     * @param referenceId The DAR Reference Id
     * @return The updated DAR
     */
    public DataAccessRequest cancelDataAccessRequest(String referenceId) {
        DataAccessRequest dar = findByReferenceId(referenceId);
        if (Objects.isNull(dar)) {
            throw new NotFoundException("Unable to find Data Access Request with the provided id: " + referenceId);
        }
        List<Election> elections = electionDAO.findElectionsByReferenceId(referenceId);
        if (!elections.isEmpty()) {
            throw new UnsupportedOperationException("Cancelling this DAR is not allowed");
        }
        DataAccessRequestData darData = dar.getData();
        darData.setStatus(ElectionStatus.CANCELED.getValue());
        updateByReferenceId(referenceId, darData);
        return findByReferenceId(referenceId);
    }

    /**
     * Iterate over a list of DataAccessRequests to find relevant Election, Vote, and Dac
     * information for each one.
     *
     * @param dataAccessRequests List of DataAccessRequest
     * @return List of DataAccessRequestManage
     */
    private List<DataAccessRequestManage> createAccessRequestManageV2(List<DataAccessRequest> dataAccessRequests) {
        List<String> requestIds = dataAccessRequests.stream().map(DataAccessRequest::getReferenceId).collect(toList());
        // Batch call 1
        List<Election> allElections = requestIds.isEmpty() ? Collections.emptyList() : electionDAO.findLastElectionsByReferenceIdsAndType(requestIds, ElectionType.DATA_ACCESS.getValue());
        Map<String, Election> referenceIdToElectionMap = allElections
            .stream()
            .collect(Collectors.toMap(Election::getReferenceId, Function.identity()));
        List<Integer> electionIds = allElections.stream().map(Election::getElectionId).collect(toList());
        // Batch call 2
        List<Vote> allVotes = electionIds.isEmpty() ? Collections.emptyList() : voteDAO.findVotesByElectionIds(electionIds);
        Map<String, List<Vote>> referenceIdToVoteMap = allElections.stream()
            .collect(Collectors.toMap(
                Election::getReferenceId,
                e -> allVotes.stream()
                    .filter(v -> v.getElectionId().equals(e.getElectionId()))
                    .collect(toList())
            ));
        List<Integer> datasetIds = dataAccessRequests.stream()
            .map(DataAccessRequest::getData).collect(toList()).stream()
            .map(DataAccessRequestData::getDatasetIds).flatMap(List::stream).collect(toList());
        // Batch call 3
        Set<Dac> dacs = datasetIds.isEmpty() ? Collections.emptySet() : dacDAO.findDacsForDatasetIds(datasetIds);
        // Batch call 4
        List<Integer> userIds = dataAccessRequests.stream().map(DataAccessRequest::getUserId).collect(toList());
        Collection<User> researchers = userDAO.findUsers(userIds);
        Map<Integer, User> researcherMap = researchers.stream()
                .collect(Collectors.toMap(User::getDacUserId, Function.identity()));

        return dataAccessRequests.stream()
            .filter(Objects::nonNull)
            .map(dar -> {
                DataAccessRequestManage darManage = new DataAccessRequestManage();
                darManage.setResearcher(researcherMap.get(dar.getUserId()));
                darManage.setDar(dar);
                darManage.setElection(referenceIdToElectionMap.get(dar.getReferenceId()));
                darManage.setVotes(referenceIdToVoteMap.get(dar.getReferenceId()));
                dar.getData().getDatasetIds().stream()
                    .findFirst()
                    .flatMap(id -> dacs.stream()
                        .filter(dataset -> dataset.getDatasetIds().contains(id))
                        .findFirst())
                    .ifPresent(darManage::setDac);
                return darManage;
            })
            .collect(toList());
    }

    private List<DataAccessRequest> filterOutCanceledDars(List<DataAccessRequest> dars) {
        return dars.stream().filter(dar -> !ElectionStatus.CANCELED.getValue().equals(dar.getData().getStatus())).collect(Collectors.toList());
    }

    @Deprecated // Use createAccessRequestManageV2 instead
    private List<DataAccessRequestManage> createAccessRequestManage(
            List<DataAccessRequest> documents,
            Map<String, Election> referenceIdElectionMap,
            AuthUser authUser) {
        User user = userService.findUserByEmail(authUser.getName());
        List<DataAccessRequestManage> requestsManage = new ArrayList<>();
        List<Integer> datasetIdsForDatasetsToApprove = documents.stream().
                map(d -> d.getData().getDatasetIds()).
                flatMap(List::stream).
                collect(toList());
        List<DataSet> dataSetsToApprove = dataSetDAO.
                findNeedsApprovalDataSetByDataSetId(datasetIdsForDatasetsToApprove);

        // Sort documents by sort time, create time, then reversed.
        Comparator<DataAccessRequest> sortField = Comparator.comparing(d -> d.getData().getSortDate());
        Comparator<DataAccessRequest> createField = Comparator.comparing(d -> d.getData().getCreateDate());
        documents.sort(sortField.thenComparing(createField).reversed());
        documents.forEach(dar -> {
            DataAccessRequestManage darManage = new DataAccessRequestManage();
            String referenceId = dar.getReferenceId();
            List<Integer> darDatasetIds = dar.getData().getDatasetIds();
            if (darDatasetIds.size() > 1) {
                darManage.addError("DAR has more than one dataset association: " + ArrayUtils.toString(darDatasetIds));
            }
            if (darDatasetIds.size() == 1) {
                darManage.setDatasetId(darDatasetIds.get(0));
            }
            Election election = referenceIdElectionMap.get(referenceId);
            if (election != null) {
                darManage.setElectionId(election.getElectionId());
            }
            darManage.setCreateDate(dar.getData().getCreateDate());
            darManage.setRus(dar.getData().getRus());
            darManage.setProjectTitle(dar.getData().getProjectTitle());
            darManage.setReferenceId(referenceId);
            darManage.setDataRequestId(referenceId);
            darManage.setFrontEndId(dar.getData().getDarCode());
            darManage.setSortDate(dar.getData().getSortDate());
            darManage.setIsCanceled(dar.getData().getStatus() != null && dar.getData().getStatus().equals(ElectionStatus.CANCELED.getValue()));
            darManage.setNeedsApproval(CollectionUtils.isNotEmpty(dataSetsToApprove));
            darManage.setDataSetElectionResult(darManage.getNeedsApproval() ? NEEDS_APPROVAL : "");
            if (election != null && !CollectionUtils.isEmpty(electionDAO.getElectionByTypeStatusAndReferenceId(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue(), election.getReferenceId()))) {
                darManage.setElectionStatus(ElectionStatus.PENDING_APPROVAL.getValue());
            } else if (CollectionUtils.isNotEmpty(dataSetsToApprove) && election != null && election.getStatus().equals(ElectionStatus.CLOSED.getValue())) {
                List<String> referenceList = Collections.singletonList(election.getReferenceId());
                List<Election> datasetElections = electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(referenceList, ElectionType.DATA_SET.getValue());
                darManage.setDataSetElectionResult(consolidateDataSetElectionsResult(datasetElections));
            } else {
                darManage.setElectionStatus(UN_REVIEWED);
            }
            darManage.setOwnerUser(getOwnerUser(dar.getUserId()).orElse(null));
            if (darManage.getOwnerUser() == null) {
                logger.error("DAR: " + darManage.getFrontEndId() + " has an invalid owner");
            }
            requestsManage.add(darManage);
        });
        return populateElectionInformation(
                populateDacInformation(requestsManage),
                referenceIdElectionMap,
                user);
    }

    /**
     * Generate a list of DARs split by dataset id. Generate one DAR per dataset that is
     * being requested. In the case of a single dataset DAR, update the existing value.
     * In the case of multiple dataset DARs, update the first one and create new ones for each
     * additional dataset past the first.
     *
     * @param user The User
     * @param dataAccessRequest DataAccessRequest with populated DAR data
     * @return List of created DARs.
     */
    public List<DataAccessRequest> createDataAccessRequest(User user, DataAccessRequest dataAccessRequest) {
        if (Objects.isNull(user) || Objects.isNull(dataAccessRequest) || Objects.isNull(dataAccessRequest.getReferenceId()) || Objects.isNull(dataAccessRequest.getData())) {
            throw new IllegalArgumentException("User and DataAccessRequest are required");
        }
        Date now = new Date();
        long nowTime = now.getTime();
        List<DataAccessRequest> newDARList = new ArrayList<>();
        DataAccessRequestData darData = dataAccessRequest.getData();
        darData.setPartialDarCode(null);
        if (Objects.isNull(darData.getCreateDate())) {
            darData.setCreateDate(nowTime);
        }
        darData.setSortDate(nowTime);
        List<Integer> datasets = dataAccessRequest.getData().getDatasetIds();
        if (CollectionUtils.isNotEmpty(datasets)) {
            String darCodeSequence = "DAR-" + counterService.getNextDarSequence();
            for (int idx = 0; idx < datasets.size(); idx++) {
                String darCode = (datasets.size() == 1) ? darCodeSequence: darCodeSequence + SUFFIX + idx ;
                darData.setDatasetIds(Collections.singletonList(datasets.get(idx)));
                darData.setDarCode(darCode);
                if (idx == 0) {
                    DataAccessRequest alreadyExists = dataAccessRequestDAO.findByReferenceId(dataAccessRequest.getReferenceId());
                    if (Objects.nonNull(alreadyExists)) {
                        dataAccessRequestDAO.updateDraftByReferenceId(dataAccessRequest.getReferenceId(), false);
                        dataAccessRequestDAO.updateDataByReferenceIdVersion2(
                            dataAccessRequest.getReferenceId(),
                            user.getDacUserId(),
                            new Date(darData.getSortDate()),
                            now,
                            now,
                            darData);
                        newDARList.add(findByReferenceId(dataAccessRequest.getReferenceId()));
                    } else {
                        String referenceId = UUID.randomUUID().toString();
                        DataAccessRequest createdDar = insertSubmittedDataAccessRequest(user, referenceId, darData);
                        newDARList.add(createdDar);
                    }
                } else {
                    String referenceId = UUID.randomUUID().toString();
                    DataAccessRequest createdDar = insertSubmittedDataAccessRequest(user, referenceId, darData);
                    newDARList.add(createdDar);
                }
            }
        }
        return newDARList;
    }

    public DataAccessRequest insertSubmittedDataAccessRequest(User user, String referencedId, DataAccessRequestData darData) {
        Date now = new Date();
        dataAccessRequestDAO.insertVersion2(
            referencedId,
            user.getDacUserId(),
            new Date(darData.getCreateDate()),
            new Date(darData.getSortDate()),
            now,
            now,
            darData);
        return findByReferenceId(referencedId);
    }

    /**
     * Update an existing DataAccessRequest. Replaces DataAccessRequestData.
     *
     * @param user The User
     * @param dar The DataAccessRequest
     * @return The updated DataAccessRequest
     */
    public DataAccessRequest updateByReferenceIdVersion2(User user, DataAccessRequest dar) {
        Date now = new Date();
        dataAccessRequestDAO.updateDataByReferenceIdVersion2(dar.getReferenceId(),
            user.getDacUserId(),
            now,
            dar.getSubmissionDate(),
            now,
            dar.getData());
        return findByReferenceId(dar.getReferenceId());
    }

    public Document describeDataAccessRequestFieldsById(String id, List<String> fields) {
        Document dar = getDataAccessRequestByReferenceIdAsDocument(id);
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

    public List<DataAccessRequestManage> getDraftDataAccessRequestManage(Integer userId) {
        List<DataAccessRequest> accessList = userId == null
                ? dataAccessRequestDAO.findAllDraftDataAccessRequests()
                : dataAccessRequestDAO.findAllDraftsByUserId(userId);
        return createAccessRequestManageV2(accessList);
    }


    public File createApprovedDARDocument() throws IOException {
        List<Election> elections = electionDAO.findDataAccessClosedElectionsByFinalResult(true);
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        FileWriter darWriter = new FileWriter(file);
        dataAccessReportsParser.setApprovedDARHeader(darWriter);
        if (CollectionUtils.isNotEmpty(elections)) {
            for (Election election : elections) {
                DataAccessRequest dataAccessRequest = findByReferenceId(election.getReferenceId());
                User user = userDAO.findUserById(dataAccessRequest.getUserId());
                try {
                    if (Objects.nonNull(dataAccessRequest) && Objects.nonNull(dataAccessRequest.getData()) && Objects.nonNull(user)) {
                        Integer datasetId = !CollectionUtils.isEmpty(dataAccessRequest.getData().getDatasetIds()) ? dataAccessRequest.getData().getDatasetIds().get(0) : null;
                        String consentId = Objects.nonNull(datasetId) ? dataSetDAO.getAssociatedConsentIdByDataSetId(datasetId) : null;
                        Consent consent = Objects.nonNull(consentId) ? consentDAO.findConsentById(consentId) : null;
                        String profileName = user.getDisplayName();
                        if (Objects.isNull(user.getInstitutionId())) {
                            logger.warn("No institution found for creator (user: " + user.getDisplayName() + ", " + user.getDacUserId() + ") "
                              + "of this Data Access Request (DAR: " + dataAccessRequest.getReferenceId() + ")");
                        }
                        String institution = Objects.isNull(user.getInstitutionId()) ? "" : institutionDAO.findInstitutionById(user.getInstitutionId()).getName();
                        dataAccessReportsParser.addApprovedDARLine(darWriter, election, dataAccessRequest, profileName, institution, consent.getName(), consent.getTranslatedUseRestriction());
                    }
                } catch (Exception e) {
                    logger.error("Exception generating Approved DAR Document", e);
                }
            }
        }
        darWriter.flush();
        return file;
    }

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
                DataAccessRequest dar = findByReferenceId(election.getReferenceId());
                if (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) {
                    Integer datasetId = !CollectionUtils.isEmpty(dar.getData().getDatasetIds()) ? dar.getData().getDatasetIds().get(0) : null;
                    String consentId = Objects.nonNull(datasetId) ? dataSetDAO.getAssociatedConsentIdByDataSetId(datasetId) : null;
                    Consent consent = Objects.nonNull(consentId) ? consentDAO.findConsentById(consentId) : null;
                    if (Objects.nonNull(consent)) {
                        dataAccessReportsParser.addReviewedDARLine(darWriter, election, dar, consent.getName(), consent.getTranslatedUseRestriction());
                    } else {
                        dataAccessReportsParser.addReviewedDARLine(darWriter, election, dar, "", "");
                    }
                }
            }
        }
        darWriter.flush();
        return file;
    }

    public File createDataSetApprovedUsersDocument(Integer dataSetId) throws IOException {
        File file = File.createTempFile("DatasetApprovedUsers", ".tsv");
        FileWriter darWriter = new FileWriter(file);
        List<DataAccessRequest> darList = dataAccessRequestDAO.findAllDataAccessRequestsByDatasetId(Integer.toString(dataSetId));
        dataAccessReportsParser.setDataSetApprovedUsersHeader(darWriter);
        if (CollectionUtils.isNotEmpty(darList)){
            for(DataAccessRequest dar: darList){
                String referenceId = dar.getReferenceId();
                User researcher = userDAO.findUserById(dar.getUserId());
                Date approvalDate = electionDAO.findApprovalAccessElectionDate(referenceId);
                if (Objects.nonNull(approvalDate) && Objects.nonNull(researcher)) {
                    String email = researcher.getEmail();
                    String name = researcher.getDisplayName();
                    String institution = (Objects.isNull(researcher.getInstitutionId())) ? "" : institutionDAO.findInstitutionById(researcher.getInstitutionId()).getName();
                    String darCode = dar.getData().getDarCode();
                    dataAccessReportsParser.addDataSetApprovedUsersLine(darWriter, email, name, institution, darCode, approvalDate);
                }
            }
        }
        darWriter.flush();
        return file;
    }

    public DARModalDetailsDTO DARModalDetailsDTOBuilder(DataAccessRequest dataAccessRequest, User user, ElectionService electionService) {
        DARModalDetailsDTO darModalDetailsDTO = new DARModalDetailsDTO();
        List<DataSet> datasets = populateDatasets(dataAccessRequest);
        Optional<User> optionalUser = Optional.ofNullable(user);
        String status = optionalUser.isPresent() ? user.getStatus() : "";
        String rationale = optionalUser.isPresent() ? user.getRationale() : "";
        User researcher = userDAO.findUserById(dataAccessRequest.getUserId());
        Boolean hasProps = Objects.nonNull(researcher) && Objects.nonNull(researcher.getProperties());
        Optional<UserProperty> department = hasProps ? researcher.getProperties().stream().filter(
            (UserProperty prop) -> prop.getPropertyKey() == UserFields.DEPARTMENT.getValue())
            .findFirst()
          : Optional.empty();
        Optional<UserProperty> city = hasProps ? researcher.getProperties().stream().filter(
            (UserProperty prop) -> prop.getPropertyKey() == UserFields.CITY.getValue())
            .findFirst()
          : Optional.empty();
        Optional<UserProperty> country = hasProps ? researcher.getProperties().stream().filter(
            (UserProperty prop) -> prop.getPropertyKey() == UserFields.COUNTRY.getValue())
            .findFirst()
          : Optional.empty();
        return darModalDetailsDTO
                .setNeedDOApproval(electionService.darDatasetElectionStatus(dataAccessRequest.getReferenceId()))
                .setResearcherName(researcher.getDisplayName())
                .setStatus(status)
                .setRationale(rationale)
                .setUserId(dataAccessRequest.getUserId())
                .setDarCode(Objects.nonNull(dataAccessRequest.getData()) ? dataAccessRequest.getData().getDarCode() : "")
                .setPrincipalInvestigator(DarUtil.findPI(researcher))
                .setInstitutionName((researcher == null || researcher.getInstitutionId() == null) ?
                   ""
                   : institutionDAO.findInstitutionById(researcher.getInstitutionId()).getName())
                .setProjectTitle(dataAccessRequest.getData().getProjectTitle())
                .setDepartment((department.isPresent()) ? department.get().getPropertyValue() : "")
                .setCity((city.isPresent()) ? city.get().getPropertyValue() : "")
                .setCountry((country.isPresent()) ? country.get().getPropertyValue() : "")
                .setIsThereDiseases(false)
                .setIsTherePurposeStatements(false)
                .setResearchType(dataAccessRequest)
                .setDiseases(dataAccessRequest)
                .setPurposeStatements(dataAccessRequest)
                .setDatasets(datasets)
                .setRus(Objects.nonNull(dataAccessRequest.getData()) ? dataAccessRequest.getData().getRus() : "");
    }

    private List<DataSet> populateDatasets(DataAccessRequest dar) {
        List<Integer> datasetIds = Objects.nonNull(dar.getData()) ? dar.getData().getDatasetIds() : Collections.emptyList();
        if (!datasetIds.isEmpty()) {
            return dataSetDAO.findDataSetsByIdList(datasetIds);
        }
        return Collections.emptyList();
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

    /**
     * Return a cloned, immutable list of DataAccessRequestManage objects with election and vote information populated
     */
    private List<DataAccessRequestManage> populateElectionInformation(List<DataAccessRequestManage> darManages, Map<String, Election> referenceIdElectionMap, User user) {
        Collection<Election> elections = referenceIdElectionMap.values();
        List<Integer> electionIds = referenceIdElectionMap.values().stream().
                map(Election::getElectionId).collect(toList());
        List<Pair<Integer, Integer>> rpAccessElectionIdPairs = new ArrayList<>();
        Map<Integer, List<Vote>> electionVoteMap = new HashMap<>();
        List<Vote> userVotes = new ArrayList<>();
        if (!electionIds.isEmpty()) {
            rpAccessElectionIdPairs.addAll(electionDAO.findRpAccessElectionIdPairs(electionIds));
            electionVoteMap.putAll(voteDAO.findVotesByElectionIds(electionIds).stream().
                    collect(Collectors.groupingBy(Vote::getElectionId)));
            userVotes.addAll(voteDAO.findVotesByElectionIdsAndUser(electionIds, user.getDacUserId()));
        }
        Map<Integer, List<Vote>> electionPendingVoteMap = electionVoteMap.entrySet().stream().
                collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().filter(v -> v.getVote() == null).
                                collect(toList())
                ));
        List<String> referenceIds = new ArrayList<>(referenceIdElectionMap.keySet());
        Map<String, Consent> consentMap = new HashMap<>();
        if (!referenceIds.isEmpty()) {
            consentMap.putAll(consentDAO.findConsentsFromConsentsIDs(referenceIds).stream().
                    collect(Collectors.toMap(Consent::getConsentId, Function.identity())));
        }
        return darManages.stream().
                map(d -> gson.fromJson(gson.toJson(d), DataAccessRequestManage.class)).
                peek(c -> {
                    if (c.getElectionId() != null) {
                        Optional<Election> electionOption = elections.stream().filter(e -> e.getElectionId().equals(c.getElectionId())).findFirst();
                        if (electionOption.isPresent()) {
                            Election election = electionOption.get();
                            List<Vote> electionVotes = electionVoteMap.get(election.getElectionId());
                            List<Vote> pendingVotes = electionPendingVoteMap.get(election.getElectionId());
                            Optional<Pair<Integer, Integer>> rpElectionIdOption = rpAccessElectionIdPairs.stream().
                                    filter(p -> p.getRight().equals(election.getElectionId())).
                                    findFirst();
                            if (rpElectionIdOption.isPresent()) {
                                c.setRpElectionId(rpElectionIdOption.get().getKey());
                                Optional<Vote> rpVoteOption = userVotes.stream().filter(v -> v.getElectionId().equals(c.getRpElectionId())).findFirst();
                                rpVoteOption.ifPresent(vote -> c.setRpVoteId(vote.getVoteId()));
                            }
                            boolean isReminderSent = electionVotes.stream().
                                    anyMatch(Vote::getIsReminderSent);
                            boolean finalVote = electionVotes.stream().
                                    filter(v -> v.getVote() != null).
                                    filter(v -> v.getType() != null).
                                    filter(v -> v.getType().equalsIgnoreCase(VoteType.FINAL.getValue())).
                                    anyMatch(Vote::getVote);
                            Optional<Vote> userVoteOption = electionVotes.stream().
                                    filter(v -> v.getDacUserId().equals(user.getDacUserId())).
                                    findFirst();
                            c.setTotalVotes(electionVotes.size());
                            c.setVotesLogged(electionVotes.size() - pendingVotes.size());
                            c.setLogged(c.getVotesLogged() + "/" + c.getTotalVotes());
                            c.setReminderSent(isReminderSent);
                            c.setFinalVote(finalVote);
                            c.setElectionVote(election.getFinalVote());
                            if (userVoteOption.isPresent()) {
                                c.setVoteId(userVoteOption.get().getVoteId());
                                c.setAlreadyVoted(true);
                            }
                            c.setElectionStatus(election.getStatus());
                            c.setReferenceId(election.getReferenceId());
                            Consent consent = consentMap.get(election.getReferenceId());
                            if (consent != null) {
                                // See PendingCaseService ... we populate this from the consent's name, not group name
                                c.setConsentGroupName(consent.getName());
                            }
                        }
                    }
                }).
                collect(Collectors.collectingAndThen(toList(), Collections::unmodifiableList));
    }

    /**
     * Return a cloned, immutable list of DataAccessRequestManage objects with Dac and DacId information populated
     */
    private List<DataAccessRequestManage> populateDacInformation(List<DataAccessRequestManage> darManages) {
        Map<Integer, Integer> datasetDacIdPairs = dataSetDAO.findDatasetAndDacIds().
                stream().
                collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        List<Dac> dacList = dacDAO.findAll();
        return darManages.stream().
                map(d -> gson.fromJson(gson.toJson(d), DataAccessRequestManage.class)).
                peek(c -> {
                    if (datasetDacIdPairs.containsKey(c.getDatasetId())) {
                        Integer dacId = datasetDacIdPairs.get(c.getDatasetId());
                        c.setDacId(dacId);
                        Optional<Dac> dacOption = dacList.stream().filter(d -> d.getDacId().equals(dacId)).findFirst();
                        dacOption.ifPresent(c::setDac);
                    }
                }).collect(Collectors.collectingAndThen(toList(), Collections::unmodifiableList));
    }

    private Optional<User> getOwnerUser(Object dacUserId) {
        try {
            Integer userId = Integer.valueOf(dacUserId.toString());
            Set<User> users = userDAO.findUsersWithRoles(Collections.singletonList(userId));
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

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @param authUser AuthUser
     * @return List<Document>
     */
    private List<DataAccessRequest> getUnReviewedDarsForUser(AuthUser authUser) {
        List<DataAccessRequest> activeDars = dataAccessRequestDAO.findAllDataAccessRequests().stream().
                filter(d -> !ElectionStatus.CANCELED.getValue().equalsIgnoreCase(Objects.nonNull(d.getData()) ? d.getData().getStatus() : "")).
                collect(Collectors.toList());
        if (dacService.isAuthUserAdmin(authUser)) {
            return activeDars;
        }
        List<Integer> dataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());
        return activeDars.stream().
                filter(d -> d.getData().getDatasetIds().stream().anyMatch(dataSetIds::contains)).
                collect(Collectors.toList());
    }

}
