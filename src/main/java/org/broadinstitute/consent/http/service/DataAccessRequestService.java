package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("UnusedReturnValue")
public class DataAccessRequestService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ConsentDAO consentDAO;
    private final CounterService counterService;
    private final DacDAO dacDAO;
    private final DatasetDAO dataSetDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DarCollectionDAO darCollectionDAO;
    private final ElectionDAO electionDAO;
    private final MatchDAO matchDAO;
    private final UserDAO userDAO;
    private final VoteDAO voteDAO;
    private final InstitutionDAO institutionDAO;

    private final DacService dacService;
    private final DataAccessReportsParser dataAccessReportsParser;
    private static final String SUFFIX = "-A-";

    @Inject
    public DataAccessRequestService(CounterService counterService, DAOContainer container,
            DacService dacService) {
        this.consentDAO = container.getConsentDAO();
        this.counterService = counterService;
        this.dacDAO = container.getDacDAO();
        this.dataAccessRequestDAO = container.getDataAccessRequestDAO();
        this.darCollectionDAO = container.getDarCollectionDAO();
        this.dataSetDAO = container.getDatasetDAO();
        this.electionDAO = container.getElectionDAO();
        this.matchDAO = container.getMatchDAO();
        this.userDAO = container.getUserDAO();
        this.voteDAO = container.getVoteDAO();
        this.institutionDAO = container.getInstitutionDAO();
        this.dacService = dacService;
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
                map(DataAccessRequest::getReferenceId).
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
    public List<DataAccessRequestManage> describeDataAccessRequestManageV2(User user, UserRoles userRoles) {
        if (Objects.isNull(user)) {
            throw new IllegalArgumentException("User is required");
        }
        if (Objects.isNull(userRoles)) {
            throw new IllegalArgumentException("UserRoles is required");
        }
        switch (userRoles) {
            case SIGNINGOFFICIAL:
                if (Objects.nonNull(user.getInstitutionId())) {
                    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequestsForInstitution(user.getInstitutionId());
                    List<DataAccessRequest> openDars = filterOutCanceledDars(dars);
                    return createAccessRequestManageV2(openDars);
                } else {
                    throw new NotFoundException("Signing Official (user: " + user.getDisplayName() + ") "
                            + "is not associated with an Institution.");
                }
            case RESEARCHER:
                List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDarsByUserId(user.getUserId());
                return createAccessRequestManageV2(dars);
            default:
                //case for Admin, Chairperson, and Member
                List<DataAccessRequest> allDars = findAllDataAccessRequests();
                List<DataAccessRequest> filteredAccessList = dacService.filterDataAccessRequestsByDac(allDars, user);
                List<DataAccessRequest> openDarList = filterOutCanceledDars(filteredAccessList);
                openDarList.sort(sortTimeComparator());
                return createAccessRequestManageV2(openDarList);
        }
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

    public List<DataAccessRequest> findAllDataAccessRequests() {
        return dataAccessRequestDAO.findAllDataAccessRequests();
    }

    public List<DataAccessRequest> findAllDraftDataAccessRequests() {
        return dataAccessRequestDAO.findAllDraftDataAccessRequests();
    }

    public List<DataAccessRequest> findAllDraftDataAccessRequestsByUser(Integer userId) {
        return dataAccessRequestDAO.findAllDraftsByUserId(userId);
    }

    public List<DataAccessRequest> getDataAccessRequestsByReferenceIds(List<String> referenceIds) {
        return dataAccessRequestDAO.findByReferenceIds(referenceIds);
    }

    public void deleteByReferenceId(User user, String referenceId) throws NotAcceptableException {
        List<Election> elections = electionDAO.findElectionsByReferenceId(referenceId);
        if (!elections.isEmpty()) {
            // If the user is an admin, delete all votes and elections
            if (user.hasUserRole(UserRoles.ADMIN)) {
                voteDAO.deleteVotesByReferenceId(referenceId);
                List<Integer> electionIds = elections.stream().map(Election::getElectionId).collect(toList());
                electionDAO.deleteElectionsFromAccessRPs(electionIds);
                electionDAO.deleteElectionsByIds(electionIds);
            } else {
                String message = String.format("Unable to delete DAR: '%s', there are existing elections that reference it.", referenceId);
                logger.warn(message);
                throw new NotAcceptableException(message);
            }
        }
        matchDAO.deleteMatchesByPurposeId(referenceId);
        dataAccessRequestDAO.deleteByReferenceId(referenceId);
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
        syncDataAccessRequestDatasets(darData.getDatasetIds(), referencedId);
        return findByReferenceId(referencedId);
    }

    public DataAccessRequest insertDraftDataAccessRequest(User user, DataAccessRequest dar) {
        if (Objects.isNull(user) || Objects.isNull(dar) || Objects.isNull(dar.getReferenceId()) || Objects.isNull(dar.getData())) {
            throw new IllegalArgumentException("User and DataAccessRequest are required");
        }
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        dar.getData().setPartialDarCode(DataAccessRequestData.partialDarCodePrefix + sdf.format(now));
        dataAccessRequestDAO.insertDraftDataAccessRequest(
            dar.getReferenceId(),
            user.getUserId(),
            now,
            now,
            null,
            now,
            dar.getData()
        );

        syncDataAccessRequestDatasets(dar.getData().getDatasetIds(), dar.getReferenceId());

        return findByReferenceId(dar.getReferenceId());
    }

    /**
     * First delete any rows with the current reference id. This will allow us to keep (referenceId, dataset_id) unique
     * Takes in a list of datasetIds and a referenceId and adds them to the dar_dataset collection
     *
     * @param datasetIds List of Integers that represent the datasetIds
     * @param referenceId ReferenceId of the corresponding DAR
     */
    private void syncDataAccessRequestDatasets(List<Integer> datasetIds, String referenceId) {
        List<DarDataset> darDatasets = datasetIds.stream()
                .map(datasetId -> new DarDataset(referenceId, datasetId))
                .collect(Collectors.toList());
        dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(referenceId);

        if (!darDatasets.isEmpty()) {
            dataAccessRequestDAO.insertAllDarDatasets(darDatasets);
        }
    }

    /**
     * Create a new Draft DAR from the canceled DARs present in source DarCollection.
     *
     * @param user The User
     * @param sourceCollection The source DarCollection
     * @return New DataAccessRequest in draft status
     */
    public DataAccessRequest createDraftDarFromCanceledCollection(User user, DarCollection sourceCollection) {
        if (Objects.isNull(sourceCollection.getDars()) || sourceCollection.getDars().isEmpty()) {
            throw new IllegalArgumentException("Source Collection must contain at least a single DAR");
        }
        DataAccessRequest sourceDar = new ArrayList<>(sourceCollection.getDars().values()).get(0);
        DataAccessRequestData sourceData = sourceDar.getData();
        if (Objects.isNull(sourceData)) {
            throw new IllegalArgumentException("Source Collection must contain at least a single DAR with a populated data");
        }

        // Find all dataset ids for canceled DARs in the collection
        List<Integer> datasetIds = sourceCollection
                .getDars().values().stream()
                .map(DataAccessRequest::getData)
                .filter(d -> DarStatus.CANCELED.getValue().equalsIgnoreCase(d.getStatus()))
                .map(DataAccessRequestData::getDatasetIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (datasetIds.isEmpty()) {
            throw new IllegalArgumentException("Source Collection must contain references to at least a single canceled DAR's dataset");
        }

        List<String> canceledReferenceIds = sourceCollection
                .getDars().values().stream()
                .map(DataAccessRequest::getData)
                .filter(d -> DarStatus.CANCELED.getValue().equalsIgnoreCase(d.getStatus()))
                .map(DataAccessRequestData::getReferenceId)
                .collect(Collectors.toList());
        List<Integer> electionIds = electionDAO.getElectionIdsByReferenceIds(canceledReferenceIds);
        if (!electionIds.isEmpty()) {
            String errorMessage = "Found 'Open' elections for canceled DARs in collection id: " + sourceCollection.getDarCollectionId();
            logger.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        List<String> sourceReferenceIds = sourceCollection
                .getDars().values().stream()
                .map(DataAccessRequest::getReferenceId)
                .collect(Collectors.toList());
        dataAccessRequestDAO.archiveByReferenceIds(sourceReferenceIds);

        String referenceId = UUID.randomUUID().toString();
        Date now = new Date();
        // Clone the dar's data object and reset values that need to be updated for the clone
        DataAccessRequestData newData = new Gson().fromJson(sourceData.toString(), DataAccessRequestData.class);
        newData.setDarCode(null);
        newData.setStatus(null);
        newData.setReferenceId(referenceId);
        newData.setDatasetIds(datasetIds);
        newData.setCreateDate(now.getTime());
        newData.setSortDate(now.getTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        newData.setPartialDarCode(DataAccessRequestData.partialDarCodePrefix + sdf.format(now));
        dataAccessRequestDAO.insertDraftDataAccessRequest(
            referenceId,
            user.getUserId(),
            now,
            now,
            null,
            now,
            newData
        );
        syncDataAccessRequestDatasets(datasetIds, referenceId);

        return findByReferenceId(referenceId);
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
        List<Integer> electionIds = electionDAO.getElectionIdsByReferenceIds(List.of(referenceId));
        if (!electionIds.isEmpty()) {
            throw new UnsupportedOperationException("Cancelling this DAR is not allowed");
        }
        DataAccessRequestData darData = dar.getData();
        darData.setStatus(DarStatus.CANCELED.getValue());
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
        Collection<User> researchers = userIds.isEmpty() ? Collections.emptyList() : userDAO.findUsers(userIds);
        Map<Integer, User> researcherMap = researchers.stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

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
        return dars.stream().filter(dar -> !DarStatus.CANCELED.getValue().equals(dar.getData().getStatus())).collect(Collectors.toList());
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
            Integer collectionId = darCollectionDAO.insertDarCollection(darCodeSequence, user.getUserId(), now);
            for (int idx = 0; idx < datasets.size(); idx++) {
                String darCode = (datasets.size() == 1) ? darCodeSequence: darCodeSequence + SUFFIX + idx ;
                darData.setDatasetIds(Collections.singletonList(datasets.get(idx)));
                darData.setDarCode(darCode);
                if (idx == 0) {
                    DataAccessRequest alreadyExists = dataAccessRequestDAO.findByReferenceId(dataAccessRequest.getReferenceId());
                    if (Objects.nonNull(alreadyExists)) {
                        dataAccessRequestDAO.updateDraftForCollection(collectionId, dataAccessRequest.getReferenceId());
                        dataAccessRequestDAO.updateDataByReferenceIdVersion2(
                            dataAccessRequest.getReferenceId(),
                            user.getUserId(),
                            new Date(darData.getSortDate()),
                            now,
                            now,
                            darData);
                        newDARList.add(findByReferenceId(dataAccessRequest.getReferenceId()));
                        syncDataAccessRequestDatasets(List.of(datasets.get(idx)), dataAccessRequest.getReferenceId());
                    } else {
                        String referenceId = UUID.randomUUID().toString();
                        DataAccessRequest createdDar = insertSubmittedDataAccessRequest(user, referenceId, darData, collectionId, now);
                        newDARList.add(createdDar);
                        syncDataAccessRequestDatasets(List.of(datasets.get(idx)), referenceId);
                    }
                } else {
                    String referenceId = UUID.randomUUID().toString();
                    DataAccessRequest createdDar = insertSubmittedDataAccessRequest(user, referenceId, darData, collectionId, now);
                    newDARList.add(createdDar);
                    syncDataAccessRequestDatasets(List.of(datasets.get(idx)), referenceId);
                }
            }
        }
        return newDARList;
    }

    public DataAccessRequest insertSubmittedDataAccessRequest(User user, String referencedId, DataAccessRequestData darData, Integer collectionId, Date now) {
        dataAccessRequestDAO.insertDataAccessRequest(
            collectionId,
            referencedId,
            user.getUserId(),
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
            user.getUserId(),
            now,
            dar.getSubmissionDate(),
            now,
            dar.getData());
        if (Objects.nonNull(dar.getCollectionId())) {
            darCollectionDAO.updateDarCollection(dar.getCollectionId(), user.getUserId(), now);
        }
        // Update the dar_dataset collection
        syncDataAccessRequestDatasets(dar.getData().getDatasetIds(), dar.getReferenceId());

        return findByReferenceId(dar.getReferenceId());
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
                        String consentId = Objects.nonNull(datasetId) ? dataSetDAO.getAssociatedConsentIdByDatasetId(datasetId) : null;
                        Consent consent = Objects.nonNull(consentId) ? consentDAO.findConsentById(consentId) : null;
                        String profileName = user.getDisplayName();
                        if (Objects.isNull(user.getInstitutionId())) {
                            logger.warn("No institution found for creator (user: " + user.getDisplayName() + ", " + user.getUserId() + ") "
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
                    String consentId = Objects.nonNull(datasetId) ? dataSetDAO.getAssociatedConsentIdByDatasetId(datasetId) : null;
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

    public String getDatasetApprovedUsersContent(AuthUser authUser, Integer datasetId) {
        User requestingUser = userDAO.findUserByEmail(authUser.getEmail());
        if (Objects.isNull(requestingUser)) {
            throw new NotFoundException("User not found: " + authUser.getEmail());
        }
        StringBuilder builder = new StringBuilder();
        builder.append(dataAccessReportsParser.getDatasetApprovedUsersHeader(requestingUser));
        List<DataAccessRequest> darList = dataAccessRequestDAO.findAllDataAccessRequestsByDatasetId(Integer.toString(datasetId));
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
                    builder.append(dataAccessReportsParser.getDataSetApprovedUsersLine(requestingUser, email, name, institution, darCode, approvalDate));
                }
            }
        }
        return builder.toString();
    }

    /**
     * @param authUser AuthUser
     * @return List<DataAccessRequest>
     */
    private List<DataAccessRequest> getUnReviewedDarsForUser(AuthUser authUser) {
        List<DataAccessRequest> activeDars = dataAccessRequestDAO.findAllDataAccessRequests().stream().
                filter(d -> !DarStatus.CANCELED.getValue().equalsIgnoreCase(Objects.nonNull(d.getData()) ? d.getData().getStatus() : "")).
                collect(Collectors.toList());
        if (dacService.isAuthUserAdmin(authUser)) {
            return activeDars;
        }
        List<Integer> dataSetIds = dataSetDAO.findDatasetsByAuthUserEmail(authUser.getEmail()).stream().
                map(Dataset::getDataSetId).
                collect(Collectors.toList());
        return activeDars.stream().
                filter(d -> d.getData().getDatasetIds().stream().anyMatch(dataSetIds::contains)).
                collect(Collectors.toList());
    }

}
