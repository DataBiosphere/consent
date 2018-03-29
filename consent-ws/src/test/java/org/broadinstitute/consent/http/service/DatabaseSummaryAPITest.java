package org.broadinstitute.consent.http.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class DatabaseSummaryAPITest {

    @Mock
    private VoteDAO voteDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private DACUserDAO dacUserDAO;
    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private DataSetDAO dataSetDAO;
    @Mock
    private MatchDAO matchDAO;
    @Mock
    private MongoConsentDB mongoDB;

    DatabaseSummaryAPI databaseSummaryAPI;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        databaseSummaryAPI = Mockito.spy(new DatabaseSummaryAPI(voteDAO, electionDAO, dacUserDAO, consentDAO, dataSetDAO, matchDAO, mongoDB));
    }

    // In this tests we won't validate the resulting file, we will just validate the methods being called for each response given by the mocks is accurate.

    @Test
    public void testDescribeDataRequestSummaryCases() throws Exception {
        String electionType = ElectionType.DATA_ACCESS.getValue();
        databaseSummaryAPI.describeDataRequestSummaryCases(electionType);
        Mockito.verify(databaseSummaryAPI).getAccessSummaryCases(electionType);
        electionType = ElectionType.RP.getValue();
        databaseSummaryAPI.describeDataRequestSummaryCases(electionType);
        Mockito.verify(databaseSummaryAPI).getSummaryCases(electionType);
    }

    @Test
    public void testDescribeMatchSummaryCases() throws Exception {
        when(matchDAO.countMatchesByResult(Boolean.TRUE)).thenReturn(2);
        when(matchDAO.countMatchesByResult(Boolean.FALSE)).thenReturn(2);

        when(electionDAO.findLastElectionsWithFinalVoteByType(ElectionType.DATA_ACCESS.getValue())).thenReturn(electionsList(ElectionType.DATA_ACCESS.getValue(), "Open"));
        List<Summary> matchSummaryList = databaseSummaryAPI.describeMatchSummaryCases();
        assertTrue("The list should have two elements: ", matchSummaryList.size() == 2);
        assertTrue("The list for matches should have two positive cases: ", matchSummaryList.get(0).getReviewedPositiveCases().equals(2));
        assertTrue("The list for matches should have two negative cases: ", matchSummaryList.get(0).getReviewedNegativeCases().equals(2));
        assertTrue("The list for matches should have no pending cases: ", matchSummaryList.get(0).getPendingCases().equals(0));

        assertTrue("The list for closed positive cases should be zero: ", matchSummaryList.get(1).getReviewedPositiveCases().equals(0));
        assertTrue("The list for closed negative cases should be zero: ", matchSummaryList.get(1).getReviewedNegativeCases().equals(0));
        assertTrue("The list for pending cases should be zero: ", matchSummaryList.get(1).getPendingCases().equals(0));

        //This modifies the result for closed cases
        when(electionDAO.findLastElectionsWithFinalVoteByType(ElectionType.DATA_ACCESS.getValue())).thenReturn(ListUtils.union(electionsList(ElectionType.DATA_ACCESS.getValue(), "Open"), electionsList(ElectionType.DATA_ACCESS.getValue(), "Closed")));
        when(voteDAO.findVotesByElectionIds(anyObject())).thenReturn(randomVotesList(123, VoteType.AGREEMENT.getValue()));

        matchSummaryList = databaseSummaryAPI.describeMatchSummaryCases();
        assertTrue("The list should have two elements: ", matchSummaryList.size() == 2);
        assertTrue("The list for matches should have two positive cases: ", matchSummaryList.get(0).getReviewedPositiveCases().equals(2));
        assertTrue("The list for matches should have two negative cases: ", matchSummaryList.get(0).getReviewedNegativeCases().equals(2));
        assertTrue("The list for matches should have no pending cases: ", matchSummaryList.get(0).getPendingCases().equals(0));

        assertTrue("The list for closed positive cases should be two: ", matchSummaryList.get(1).getReviewedPositiveCases().equals(2));
        assertTrue("The list for closed negative cases should be three: ", matchSummaryList.get(1).getReviewedNegativeCases().equals(3));
    }

    @Test(expected = IllegalStateException.class )
    public void testSingletonCollectorException() throws Exception {
        List<String> strings = Arrays.asList("One item", "Another item");
        strings.stream().filter(s -> s.length() > 0).collect(DatabaseSummaryAPI.singletonCollector());
    }

    @Test
    public void testSingletonCollector() throws Exception {
        List<String> strings = Arrays.asList("One item");
        String string = strings.stream().filter(s -> s.length() > 0).collect(DatabaseSummaryAPI.singletonCollector());
        assertTrue("The returned element equals the only element in the list ", string.equals("One item"));

        strings = new ArrayList<>();
        string = strings.stream().filter(s -> s.length() > 0).collect(DatabaseSummaryAPI.singletonCollector());
        assertTrue("There are no elements, so the returned string should be null ", Objects.isNull(string));
    }

    @Test
    public void testFormatTimeToDate() throws Exception {
        Calendar myCalendar = new GregorianCalendar(2016, 2, 11);
        String getAsString = databaseSummaryAPI.formatTimeToDate(myCalendar.getTimeInMillis());
        assertTrue(getAsString + " is the same date string for March 3, 2016 ", getAsString.equals("3/11/2016"));
    }

    @Test
    public void testDelimiterCheck(){
        String testString = "\"Samples\" Restricted for use with \"cancer\" [DOID_162(CC)]\bFuture use \"\"\\\" for methods" +
                " research (analytic/software/technology development) is prohibited [NMDS]\bNotes:\bFuture use as a" +
                " control set for\"' diseases other\' than\" those specified is not prohibited\n\"";
        int count = StringUtils.countMatches(databaseSummaryAPI.delimiterCheck(testString), "\"");
        assertThat(count, is(2));
    }

    /** Private methods for mocking **/

    private List<Election> electionsList(String electionType, String status){
        Election e1 = new Election(1, electionType, status, new Date(), "571fd0ca5ce51d1b148715f3", new Date(), false, 1);
        Election e2 = new Election(2, electionType, status, new Date(), "572fd0ca5ce51d1b148715f3", new Date(), false, 2);
        Election e3 = new Election(3, electionType, status, new Date(), "573fd0ca5ce51d1b148715f3", new Date(), false, 3);
        Election e4 = new Election(4, electionType, status, new Date(), "574fd0ca5ce51d1b148715f3", new Date(), false, 4);
        return Arrays.asList(e1, e2, e3, e4);
    }

    private List<Vote> randomVotesList(Integer dacUserId, String voteType){
        Vote v1 = new Vote(1, false, dacUserId, new Date(), new Date(), 1, "", voteType, false, false);
        Vote v2 = new Vote(2, false, dacUserId, new Date(), new Date(), 2, "", voteType, false, false);
        Vote v3 = new Vote(3, false, dacUserId, new Date(), new Date(), 3, "", voteType, false, false);
        Vote v4 = new Vote(4, true, dacUserId, new Date(), new Date(), 4, "", voteType, false, false);
        Vote v5 = new Vote(5, true, dacUserId, new Date(), new Date(), 5, "", voteType, false, false);
        Vote nul = new Vote(6, null, dacUserId, new Date(), new Date(), 6, "", voteType, false, false);
        return Arrays.asList(v1, v2, v3, v4, v5, nul);
    }

}