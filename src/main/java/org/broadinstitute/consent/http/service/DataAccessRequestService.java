package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.sql.Timestamp;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("UnusedReturnValue")
public class DataAccessRequestService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConsentDAO consentDAO;
    private final DacDAO dacDAO;
    private final DACUserDAO dacUserDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DataSetDAO dataSetDAO;
    private final ElectionDAO electionDAO;
    private final DacService dacService;
    private final UserService userService;
    private final VoteDAO voteDAO;
    private final MongoConsentDB mongo;

    private static final Gson gson = new GsonBuilder().setDateFormat("MMM d, yyyy").create();
    private static final String UN_REVIEWED = "un-reviewed";
    private static final String NEEDS_APPROVAL = "Needs Approval";
    private static final String APPROVED = "Approved";
    private static final String DENIED = "Denied";

    @Inject
    public DataAccessRequestService(ConsentDAO consentDAO, DataAccessRequestDAO dataAccessRequestDAO, DacDAO dacDAO,
                                    DACUserDAO dacUserDAO, DataSetDAO dataSetDAO, ElectionDAO electionDAO,
                                    DacService dacService, UserService userService, VoteDAO voteDAO,
                                    MongoConsentDB mongo) {
        this.consentDAO = consentDAO;
        this.dacDAO = dacDAO;
        this.dacUserDAO = dacUserDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.dacService = dacService;
        this.userService = userService;
        this.voteDAO = voteDAO;
        this.mongo = mongo;
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
                map(d -> d.getString(DarConstants.REFERENCE_ID)).
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
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * Filter DataAccessRequestManage objects on user.
     *
     * @param userId   Optional UserId. If provided, filter list of DARs on this user.
     * @param authUser Required if no user id is provided. Instead, filter on what DACs the auth
     *                 user has access to.
     * @return List of DataAccessRequestManage objects
     */
    public List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId, AuthUser authUser) {
        List<DataAccessRequest> filteredAccessList;
        List<DataAccessRequest> allDars = findAllDataAccessRequests();
        if (userId == null) {
            filteredAccessList = dacService.filterDataAccessRequestsByDac(allDars, authUser);
        } else {
            filteredAccessList = allDars.stream().
                    filter(d -> d.getData().getUserId() != null).
                    filter(d -> d.getData().getUserId().equals(userId)).
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

    public List<Document> findAllDraftDataAccessRequestsAsDocuments() {
        return dataAccessRequestDAO.findAllDraftDataAccessRequests().stream().
                map(this::createDocumentFromDar).
                collect(Collectors.toList());
    }

    public List<Document> findAllDraftDataAccessRequestDocumentsByUser(Integer userId) {
        return dataAccessRequestDAO.findAllDraftsByUserId(userId).stream().
                map(this::createDocumentFromDar).
                collect(Collectors.toList());
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-604
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     * Replacement for MongoConsentDB.getDataAccessRequestCollection()
     *
     * @return List of all DataAccessRequestData objects as Documents
     */
    public List<Document> getAllDataAccessRequestsAsDocuments() {
        return findAllDataAccessRequests().stream().
                map(this::createDocumentFromDar).
                collect(Collectors.toList());
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-604
     *
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     * Replacement for MongoConsentDB.getDataAccessRequestCollection().find(ObjectId)
     *
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
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-604
     *
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     *
     * @return DataAccessRequestData object as Document
     */
    public List<Document> getDataAccessRequestsByReferenceIdsAsDocuments(List<String> referenceIds) {
        return getDataAccessRequestsByReferenceIds(referenceIds).
                stream().
                map(this::createDocumentFromDar).
                collect(Collectors.toList());
    }

    public List<DataAccessRequest> getDataAccessRequestsByReferenceIds(List<String> referenceIds) {
        return dataAccessRequestDAO.findByReferenceIds(referenceIds);
    }

    private Document createDocumentFromDar(DataAccessRequest d) {
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
        darData.setSortDate(new Date().getTime());
        dataAccessRequestDAO.updateDataByReferenceId(referencedId, darData);
        return findByReferenceId(referencedId);
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-604
     *
     * Convenience method during transition away from `Document` and to `DataAccessRequest`
     */
    public Document updateDocumentByReferenceId(String referenceId, Document document) {
        if (findByReferenceId(referenceId) == null) {
            throw new NotFoundException("Data access for the specified id does not exist");
        }
        document.remove(DarConstants.ID);
        document.put(DarConstants.REFERENCE_ID, referenceId);
        String documentJson = gson.toJson(document);
        DataAccessRequestData darData = DataAccessRequestData.fromString(documentJson);
        updateByReferenceId(referenceId, darData);
        return getDataAccessRequestByReferenceIdAsDocument(referenceId);
    }

    public DataAccessRequest insertDataAccessRequest(String referencedId, DataAccessRequestData darData) {
        dataAccessRequestDAO.insert(referencedId, darData);
        return findByReferenceId(referencedId);
    }

    public DataAccessRequest insertDraftDataAccessRequest(String referencedId, DataAccessRequestData darData) {
        dataAccessRequestDAO.insertDraft(referencedId, darData);
        return findByReferenceId(referencedId);
    }

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-604
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @param authUser AuthUser
     * @return List<Document>
     */
    public List<Document> describeDataAccessRequests(AuthUser authUser) {
        List<Document> documents = getAllDataAccessRequestsAsDocuments();
        return dacService.filterDarsByDAC(documents, authUser);
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
        DataAccessRequestData darData = dar.getData();
        darData.setStatus(ElectionStatus.CANCELED.getValue());
        updateByReferenceId(referenceId, darData);
        return findByReferenceId(referenceId);
    }

    private List<DataAccessRequestManage> createAccessRequestManage(
            List<DataAccessRequest> documents,
            Map<String, Election> referenceIdElectionMap,
            AuthUser authUser) {
        DACUser user = userService.findUserByEmail(authUser.getName());
        List<DataAccessRequestManage> requestsManage = new ArrayList<>();
        List<Integer> datasetIdsForDatasetsToApprove = documents.stream().
                map(d -> d.getData().getDatasetId()).
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
            List<Integer> darDatasetIds = dar.getData().getDatasetId();
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
            darManage.setCreateDate(new Timestamp(dar.getData().getCreateDate()));
            darManage.setRus(dar.getData().getRus());
            darManage.setProjectTitle(dar.getData().getProjectTitle());
            darManage.setDataRequestId(referenceId);
            darManage.setFrontEndId(dar.getData().getDarCode());
            darManage.setSortDate(new Date(dar.getData().getSortDate()));
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
            darManage.setOwnerUser(getOwnerUser(dar.getData().getUserId()).orElse(null));
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
     * Return a cloned, immutable list of DataAccessRequestManage objects with election and vote information populated
     */
    private List<DataAccessRequestManage> populateElectionInformation(List<DataAccessRequestManage> darManages, Map<String, Election> referenceIdElectionMap, DACUser user) {
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

    /**
     * TODO: Cleanup with https://broadinstitute.atlassian.net/browse/DUOS-609
     *
     * @param authUser AuthUser
     * @return List<Document>
     */
    private List<Document> getUnReviewedDarsForUser(AuthUser authUser) {
        List<Document> activeDars = getAllDataAccessRequestsAsDocuments().stream().
                filter(d -> !ElectionStatus.CANCELED.getValue().equalsIgnoreCase(d.getString(DarConstants.STATUS))).
                collect(Collectors.toList());
        if (dacService.isAuthUserAdmin(authUser)) {
            return activeDars;
        }
        List<Integer> dataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).stream().
                map(DataSet::getDataSetId).
                collect(Collectors.toList());
        return activeDars.stream().
                filter(d -> DarUtil.getIntegerList(d, DarConstants.DATASET_ID).stream().anyMatch(dataSetIds::contains)).
                collect(Collectors.toList());
    }

    /**
     * TODO: Remove in follow-up work
     * Temporary Migration Service Call
     * @return List<Document> All partial DARs
     */
    public List<Document> getAllMongoPartialDataAccessRequests() {
        MongoCollection<Document> collection = mongo.getPartialDataAccessRequestCollection();
        return collection.find().into(new ArrayList<>());
    }

    /**
     * TODO: Remove in follow-up work
     * Temporary Migration Service Call
     * @return List<DataAccessRequest> All partial DARs
     */
    public List<DataAccessRequest> getAllPostgresDraftDataAccessRequests() {
        return dataAccessRequestDAO.findAllDraftDataAccessRequests();
    }

}
