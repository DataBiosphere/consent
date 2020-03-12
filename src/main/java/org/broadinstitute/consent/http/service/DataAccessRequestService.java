package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
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
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static java.util.stream.Collectors.toList;

public class DataAccessRequestService {

    private Logger logger = Logger.getLogger(DataAccessRequestService.class.getName());
    private ConsentDAO consentDAO;
    private DacDAO dacDAO;
    private DACUserDAO dacUserDAO;
    private DataSetDAO dataSetDAO;
    private ElectionDAO electionDAO;
    private MongoConsentDB mongo;
    private DacService dacService;
    private VoteDAO voteDAO;

    private static final String UN_REVIEWED = "un-reviewed";
    private static final String NEEDS_APPROVAL = "Needs Approval";
    private static final String APPROVED = "Approved";
    private static final String DENIED = "Denied";

    @Inject
    public DataAccessRequestService(ConsentDAO consentDAO, DacDAO dacDAO, DACUserDAO dacUserDAO, DataSetDAO dataSetDAO,
                                    ElectionDAO electionDAO, MongoConsentDB mongo, DacService dacService,
                                    VoteDAO voteDAO) {
        this.consentDAO = consentDAO;
        this.dacDAO = dacDAO;
        this.dacUserDAO = dacUserDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.mongo = mongo;
        this.dacService = dacService;
        this.voteDAO = voteDAO;
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
     * @param userId Optional UserId. If provided, filter list of DARs on this user.
     * @param authUser Required if no user id is provided. Instead, filter on what DACs the auth
     *                 user has access to.
     * @return List of DataAccessRequestManage objects
     */
    public List<DataAccessRequestManage> describeDataAccessRequestManage(Integer userId, AuthUser authUser) {
        BasicDBObject sort = new BasicDBObject("sortDate", -1);
        List<Document> filteredAccessList;
        if (userId == null) {
            FindIterable<Document> accessList =  mongo.getDataAccessRequestCollection().find().sort(sort);
            filteredAccessList = dacService.filterDarsByDAC(accessList.into(new ArrayList<>()), authUser);
        } else {
            filteredAccessList = mongo.getDataAccessRequestCollection().
                    find(new BasicDBObject(DarConstants.USER_ID, userId)).
                    sort(sort).into(new ArrayList<>());
        }
        List<DataAccessRequestManage> darManage = new ArrayList<>();
        List<String> accessRequestIds = filteredAccessList.
                stream().
                map(d -> d.get(DarConstants.ID).toString()).
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

    private Map<String, Election> createAccessRequestElectionMap(List<Election> elections) {
        Map<String, Election> electionMap = new HashMap<>();
        elections.forEach(election -> electionMap.put(election.getReferenceId(), election));
        return electionMap;
    }

    public List<Document> describeDataAccessRequests(AuthUser authUser) {
        List<Document> documents = mongo.getDataAccessRequestCollection().find().into(new ArrayList<>());
        return dacService.filterDarsByDAC(documents, authUser);
    }

    private List<DataAccessRequestManage> createAccessRequestManage(
            List<Document> documents,
            Map<String, Election> referenceIdElectionMap,
            AuthUser authUser) {
        DACUser user = dacUserDAO.findDACUserByEmail(authUser.getName());
        List<DataAccessRequestManage> requestsManage = new ArrayList<>();
        Map<ObjectId, List<Integer>> darDatasetMap = documents.stream().collect(Collectors.toMap(
                d -> d.get(DarConstants.ID, ObjectId.class),
                d -> DarUtil.getIntegerList(d, DarConstants.DATASET_ID)
        ));
        List<Integer> datasetIdsForDatasetsToApprove = documents.stream().
                map(d -> DarUtil.getIntegerList(d, DarConstants.DATASET_ID)).
                flatMap(List::stream).
                collect(toList());
        List<DataSet> dataSetsToApprove = dataSetDAO.
                findNeedsApprovalDataSetByDataSetId(datasetIdsForDatasetsToApprove);

        documents.forEach(dar -> {
            DataAccessRequestManage darManage = new DataAccessRequestManage();
            ObjectId id = dar.get(DarConstants.ID, ObjectId.class);
            List<Integer> darDatasetIds = darDatasetMap.get(id);
            if (darDatasetIds.size() > 1) {
                darManage.addError("DAR has more than one dataset association: " + ArrayUtils.toString(darDatasetIds));
            }
            if (darDatasetIds.size() == 1) {
                darManage.setDatasetId(darDatasetIds.get(0));
            }
            Election election = referenceIdElectionMap.get(id.toString());
            if (election != null) {
                darManage.setElectionId(election.getElectionId());
            }
            darManage.setCreateDate(new Timestamp((long) id.getTimestamp() * 1000));
            darManage.setRus(dar.getString(DarConstants.RUS));
            darManage.setProjectTitle(dar.getString(DarConstants.PROJECT_TITLE));
            darManage.setDataRequestId(id.toString());
            darManage.setFrontEndId(dar.get(DarConstants.DAR_CODE).toString());
            darManage.setSortDate(dar.getDate("sortDate"));
            darManage.setIsCanceled(dar.containsKey(DarConstants.STATUS) && dar.get(DarConstants.STATUS).equals(ElectionStatus.CANCELED.getValue()));
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
            darManage.setOwnerUser(getOwnerUser(dar.get(DarConstants.USER_ID)).orElse(null));
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
        List<Pair<Integer, Integer>> rpAccessElectionIdPairs = electionDAO.findRpAccessElectionIdPairs(electionIds);
        Map<Integer, List<Vote>> electionVoteMap = voteDAO.findVotesByElectionIds(electionIds).stream().
                collect(Collectors.groupingBy(Vote::getElectionId));
        Map<Integer, List<Vote>> electionPendingVoteMap = electionVoteMap.entrySet().stream().
                collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().filter(v -> v.getVote() == null).
                                collect(toList())
                ));
        List<String> referenceIds = new ArrayList<>(referenceIdElectionMap.keySet());
        Map<String, Consent> consentMap = consentDAO.findConsentsFromConsentsIDs(referenceIds).stream().
                collect(Collectors.toMap(Consent::getConsentId, Function.identity()));
        List<Vote> userVotes = voteDAO.findVotesByElectionIdsAndUser(electionIds, user.getDacUserId());
        Gson gson = new GsonBuilder().setDateFormat("MMM d, yyyy").create();
        return darManages.stream().
                map(d -> gson.fromJson(gson.toJson(d), DataAccessRequestManage.class)).
                peek(c -> {
                    if (c.getElectionId() != null) {
                        Optional<Election> electionOption = elections.stream().filter(e -> e.getElectionId().equals(c.getElectionId())).findFirst();
                        if (electionOption.isPresent()) {
                            Election election = electionOption.get();
                            // Only calculate votes for the cases for open elections:
                            if (election.getStatus().equalsIgnoreCase(ElectionStatus.OPEN.getValue())) {
                                List<Vote> electionVotes = electionVoteMap.get(election.getElectionId());
                                List<Vote> pendingVotes = electionPendingVoteMap.get(election.getElectionId());
                                Optional<Pair<Integer, Integer>> rpElectionIdOption =  rpAccessElectionIdPairs.stream().
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
        Gson gson = new GsonBuilder().setDateFormat("MMM d, yyyy").create();
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

    private List<Document> getUnReviewedDarsForUser(AuthUser authUser) {
        if (dacService.isAuthUserAdmin(authUser)) {
            return mongo.
                    getDataAccessRequestCollection().
                    find(ne(DarConstants.STATUS, ElectionStatus.CANCELED.getValue())).
                    into(new ArrayList<>());
        }
        List<Integer> dataSetIds = dataSetDAO.findDataSetsByAuthUserEmail(authUser.getName()).stream().
                map(DataSet::getDataSetId).
                collect(toList());
        return mongo.
                getDataAccessRequestCollection().
                find(and(
                        ne(DarConstants.STATUS, ElectionStatus.CANCELED.getValue()),
                        in(DarConstants.DATASET_ID, dataSetIds))).
                into(new ArrayList<>());
    }

}
