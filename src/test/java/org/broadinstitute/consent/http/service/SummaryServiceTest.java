package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Association;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestSummaryDetail;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SummaryServiceTest {

    @Mock
    private VoteDAO voteDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private DatasetDAO datasetDAO;
    @Mock
    private MatchDAO matchDAO;
    @Mock
    private DarCollectionDAO darCollectionDAO;
    @Mock
    private DataAccessRequestService dataAccessRequestService;

    private SummaryService summaryService;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        summaryService = Mockito.spy(new SummaryService(dataAccessRequestService, voteDAO, electionDAO, userDAO, consentDAO,
                datasetDAO, matchDAO, darCollectionDAO));
    }

    private void initService() {
        summaryService = new SummaryService(dataAccessRequestService, voteDAO, electionDAO, userDAO, consentDAO,
                datasetDAO, matchDAO, darCollectionDAO);
    }

    // Test that empty data will not throw errors
    @Test
    public void testListDataAccessRequestSummaryDetails_case1() {
        initService();
        List<DataAccessRequestSummaryDetail> details = summaryService.listDataAccessRequestSummaryDetails();
        assertTrue(details.isEmpty());
    }

    // Test that minimal data will produce minimal results.
    // This unfortunately requires quite a bit of setup.
    @Test
    public void testListDataAccessRequestSummaryDetails_case2() {
        User voteUser = new User();
        voteUser.setUserId(5);
        voteUser.setDisplayName("Vote User Name");
        User darUser = new User();
        darUser.setUserId(10);
        darUser.setDisplayName("DAR User Name");
        List<Election> accessElections = List.of(createElection(ElectionType.DATA_ACCESS.getValue()));
        List<Election> rpElections = List.of(createElectionWithReferenceId(ElectionType.RP.getValue(), accessElections.get(0).getReferenceId()));
        List<Election> consentElections = List.of(createElection(ElectionType.TRANSLATE_DUL.getValue()));
        List<Integer> accessElectionIds = accessElections.stream().map(Election::getElectionId).collect(Collectors.toList());
        List<Integer> rpElectionIds = rpElections.stream().map(Election::getElectionId).collect(Collectors.toList());
        List<Integer> consentElectionIds = consentElections.stream().map(Election::getElectionId).collect(Collectors.toList());
        List<DataAccessRequest> dars = List.of(createDAR(accessElections.get(0).getReferenceId(), darUser.getUserId()));
        List<Association> associations = List.of(createAssociation(dars.get(0).getDatasetIds().get(0), consentElections.get(0).getReferenceId()));
        List<String> associatedConsentIds = List.of(consentElections.get(0).getReferenceId());
        List<Vote> accessVotes = createVotes(accessElections.get(0).getElectionId(), voteUser.getUserId());
        List<Vote> rpVotes = createVotes(rpElections.get(0).getElectionId(), voteUser.getUserId());
        List<Vote> consentVotes = createVotes(consentElections.get(0).getElectionId(), voteUser.getUserId());
        List<Match> matchList = List.of(createMatch(associatedConsentIds.get(0), dars.get(0).getReferenceId()));
        List<String> referenceIds = List.of(accessElections.get(0).getReferenceId());
        List<Integer> datasetIds = dars.get(0).getDatasetIds();
        DarCollection collection = new DarCollection();
        collection.setDarCode("DAR-" + RandomUtils.nextInt(100, 200));

        when(electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.CLOSED.getValue())).thenReturn(accessElections);
        when(electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.RP.getValue(), ElectionStatus.CLOSED.getValue())).thenReturn(rpElections);
        when(dataAccessRequestService.getDataAccessRequestsByReferenceIds(anyList())).thenReturn(dars);
        when(datasetDAO.getAssociationsForDatasetIdList(datasetIds)).thenReturn(associations);
        when(electionDAO.findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(associatedConsentIds, ElectionStatus.CLOSED.getValue())).thenReturn(consentElections);
        when(voteDAO.findVotesByElectionIds(accessElectionIds)).thenReturn(accessVotes);
        when(voteDAO.findVotesByElectionIds(rpElectionIds)).thenReturn(rpVotes);
        when(voteDAO.findVotesByElectionIds(consentElectionIds)).thenReturn(consentVotes);
        when(matchDAO.findMatchesForPurposeIds(referenceIds)).thenReturn(matchList);
        when(userDAO.findUsers(List.of(voteUser.getUserId()))).thenReturn(List.of(voteUser));
        when(userDAO.findUsers(List.of(darUser.getUserId()))).thenReturn(List.of(darUser));
        when(darCollectionDAO.findDARCollectionByCollectionId(anyInt())).thenReturn(collection);

        initService();
        List<DataAccessRequestSummaryDetail> details = summaryService.listDataAccessRequestSummaryDetails();
        assertFalse(details.isEmpty());
        // Should be able to print without errors.
        try {
            String headers = details.get(0).headers();
            assertFalse(headers.isBlank());
            String val = details.get(0).toString();
            assertFalse(val.isBlank());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    // In this tests we won't validate the resulting file, we will just validate the methods being called for each response given by the mocks is accurate.

    @Test
    public void testDescribeDataRequestSummaryCases() throws Exception {
        String electionType = ElectionType.DATA_ACCESS.getValue();
        summaryService.describeDataRequestSummaryCases(electionType);
        Mockito.verify(summaryService).getAccessSummaryCases(electionType);
        electionType = ElectionType.RP.getValue();
        summaryService.describeDataRequestSummaryCases(electionType);
        Mockito.verify(summaryService).getSummaryCases(electionType);
    }

    @Test
    public void testDescribeMatchSummaryCases() throws Exception {
        when(matchDAO.countMatchesByResult(Boolean.TRUE)).thenReturn(2);
        when(matchDAO.countMatchesByResult(Boolean.FALSE)).thenReturn(2);

        when(electionDAO.findLastElectionsWithFinalVoteByType(ElectionType.DATA_ACCESS.getValue())).thenReturn(electionsList(ElectionType.DATA_ACCESS.getValue(), "Open"));
        List<Summary> matchSummaryList = summaryService.describeMatchSummaryCases();
        assertTrue("The list should have two elements: ", matchSummaryList.size() == 2);
        assertTrue("The list for matches should have two positive cases: ", matchSummaryList.get(0).getReviewedPositiveCases().equals(2));
        assertTrue("The list for matches should have two negative cases: ", matchSummaryList.get(0).getReviewedNegativeCases().equals(2));
        assertTrue("The list for matches should have no pending cases: ", matchSummaryList.get(0).getPendingCases().equals(0));

        assertTrue("The list for closed positive cases should be zero: ", matchSummaryList.get(1).getReviewedPositiveCases().equals(0));
        assertTrue("The list for closed negative cases should be zero: ", matchSummaryList.get(1).getReviewedNegativeCases().equals(0));
        assertTrue("The list for pending cases should be zero: ", matchSummaryList.get(1).getPendingCases().equals(0));

        //This modifies the result for closed cases
        when(electionDAO.findLastElectionsWithFinalVoteByType(ElectionType.DATA_ACCESS.getValue())).thenReturn(ListUtils.union(electionsList(ElectionType.DATA_ACCESS.getValue(), "Open"), electionsList(ElectionType.DATA_ACCESS.getValue(), "Closed")));
        when(voteDAO.findVotesByElectionIds(notNull())).thenReturn(randomVotesList(123, VoteType.AGREEMENT.getValue()));

        matchSummaryList = summaryService.describeMatchSummaryCases();
        assertTrue("The list should have two elements: ", matchSummaryList.size() == 2);
        assertTrue("The list for matches should have two positive cases: ", matchSummaryList.get(0).getReviewedPositiveCases().equals(2));
        assertTrue("The list for matches should have two negative cases: ", matchSummaryList.get(0).getReviewedNegativeCases().equals(2));
        assertTrue("The list for matches should have no pending cases: ", matchSummaryList.get(0).getPendingCases().equals(0));

        assertTrue("The list for closed positive cases should be two: ", matchSummaryList.get(1).getReviewedPositiveCases().equals(2));
        assertTrue("The list for closed negative cases should be three: ", matchSummaryList.get(1).getReviewedNegativeCases().equals(3));
    }

    /**
     * Private methods for mocking
     **/

    private List<Election> electionsList(String electionType, String status) {
        Election e1 = new Election(1, electionType, status, new Date(), "571fd0ca5ce51d1b148715f3", new Date(), false, 1);
        Election e2 = new Election(2, electionType, status, new Date(), "572fd0ca5ce51d1b148715f3", new Date(), false, 2);
        Election e3 = new Election(3, electionType, status, new Date(), "573fd0ca5ce51d1b148715f3", new Date(), false, 3);
        Election e4 = new Election(4, electionType, status, new Date(), "574fd0ca5ce51d1b148715f3", new Date(), false, 4);
        return Arrays.asList(e1, e2, e3, e4);
    }

    private List<Vote> randomVotesList(Integer userId, String voteType) {
        Vote v1 = new Vote(1, false, userId, new Date(), new Date(), 1, "", voteType, false, false);
        Vote v2 = new Vote(2, false, userId, new Date(), new Date(), 2, "", voteType, false, false);
        Vote v3 = new Vote(3, false, userId, new Date(), new Date(), 3, "", voteType, false, false);
        Vote v4 = new Vote(4, true, userId, new Date(), new Date(), 4, "", voteType, false, false);
        Vote v5 = new Vote(5, true, userId, new Date(), new Date(), 5, "", voteType, false, false);
        Vote nul = new Vote(6, null, userId, new Date(), new Date(), 6, "", voteType, false, false);
        return Arrays.asList(v1, v2, v3, v4, v5, nul);
    }

    private Match createMatch(String consentId, String referenceId) {
        Match m = new Match();
        m.setConsent(consentId);
        m.setPurpose(referenceId);
        m.setMatch(true);
        return m;
    }

    private Association createAssociation(Integer datasetId, String consentId) {
        Association a = new Association();
        a.setAssociationId(RandomUtils.nextInt(1, 100));
        a.setAssociationType(AssociationType.SAMPLE_SET.getValue());
        a.setDataSetId(datasetId);
        a.setConsentId(consentId);
        return a;
    }

    private DataAccessRequest createDAR(String referenceId, Integer userId) {
        DataAccessRequestData data = new DataAccessRequestData();
        data.setReferenceId(referenceId);
        data.setDarCode("DAR-" + RandomUtils.nextInt(100, 200));
        data.setProjectTitle("Project-TEST");
        DataAccessRequest dar = new DataAccessRequest();
        dar.addDatasetId(1);
        dar.setReferenceId(referenceId);
        dar.setUserId(userId);
        dar.setData(data);
        dar.setSortDate(new Timestamp(new Date().getTime()));
        return dar;
    }

    private Election createElection(String electionType) {
        ElectionType type = ElectionType.getFromValue(electionType);
        Election e = new Election();
        e.setReferenceId(UUID.randomUUID().toString());
        e.setElectionType(type.getValue());
        e.setElectionId(type.ordinal());
        e.setCreateDate(new Date());
        e.setLastUpdate(new Date());
        e.setFinalVote(true);
        e.setFinalVoteDate(new Date());
        e.setFinalAccessVote(true);
        return e;
    }

    private Election createElectionWithReferenceId(String electionType, String referenceId) {
        Election e = createElection(electionType);
        e.setReferenceId(referenceId);
        return e;
    }

    private List<Vote> createVotes(Integer electionId, Integer userId) {
        return Arrays.stream(VoteType.values()).map(t -> {
                    Vote v = new Vote();
                    v.setVote(true);
                    v.setType(t.getValue());
                    v.setElectionId(electionId);
                    v.setCreateDate(new Date());
                    v.setUpdateDate(new Date());
                    v.setUserId(userId);
                    return v;
                }
        ).collect(Collectors.toUnmodifiableList());
    }

}