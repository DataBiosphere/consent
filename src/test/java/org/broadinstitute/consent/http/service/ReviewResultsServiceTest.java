package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentBuilder;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.grammar.And;
import org.broadinstitute.consent.http.models.grammar.Named;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ReviewResultsServiceTest {

    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private ConsentDAO consentDAO;

    private ReviewResultsService service;

    private final Election sampleElection = new Election();
    private final DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    private final Consent consent = new ConsentBuilder().
            setRequiresManualReview(false).
            setUseRestriction(new And(new Named("DOID:1"), new Named("DOID:2"))).
            setDataUse(dataUse).
            setName("Consent 1").
            build();

    private void initService() {
        service = new ReviewResultsService(electionDAO, voteDAO, consentDAO);
    }

    @Before
    public void setUp(){
        openMocks(this);
        when(voteDAO.findVoteByTypeAndElectionId(anyInt(), anyString())).thenReturn(randomVotesList());
        when(voteDAO.findElectionReviewVotesByElectionId(anyInt(), anyString())).thenReturn(randomReviewVotesList());
        when(electionDAO.findElectionWithFinalVoteById(anyInt())).thenReturn(sampleElection);
        when(consentDAO.findConsentById(any())).thenReturn(consent);
    }

    @Test
    public void testOpenElections() throws Exception {
        initService();
        when(electionDAO.verifyOpenElections()).thenReturn(1);
        assertTrue("There are open elections", service.openElections());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        assertFalse("There aren't open elections", service.openElections());
    }

    @Test
    public void testDescribeElectionReviewByElectionId() throws Exception {
        initService();
        sampleElection.setElectionId(123);
        ElectionReview review = service.describeElectionReviewByElectionId(1);
        assertTrue("Consent should be equal to mocked response ", review.getConsent().equals(consent));
        assertTrue("Sample Election should be equal to mocked response ", review.getElection().equals(sampleElection));

    }

    @Test
    public void testDescribeAgreementVote() throws Exception {
        initService();
        assertTrue("The method should return 4 votes ",service.describeAgreementVote(1).size() == 4);
    }

    /* Mocked Data */
    Vote v1 = new Vote(1, null, 1, new Date(), new Date(), 1, "", "AGREEMENT", false, false);
    Vote v2 = new Vote(2, null, 2, new Date(), new Date(), 2, "", "AGREEMENT", false, false);
    Vote v3 = new Vote(3, null, 3, new Date(), new Date(), 3, "", "AGREEMENT", false, false);
    Vote v4 = new Vote(4, null, 4, new Date(), new Date(), 4, "", "AGREEMENT", false, false);

    private List<Vote> randomVotesList(){
        return Arrays.asList(v1, v2, v3, v4);
    }

    private List<ElectionReviewVote> randomReviewVotesList(){
        ElectionReviewVote rv1 = new ElectionReviewVote(v1, "Sample User 1", "Sample Vote Review 1");
        ElectionReviewVote rv2 = new ElectionReviewVote(v2, "Sample User 2", "Sample Vote Review 2");
        ElectionReviewVote rv3 = new ElectionReviewVote(v3, "Sample User 3", "Sample Vote Review 3");
        ElectionReviewVote rv4 = new ElectionReviewVote(v4, "Sample User 4", "Sample Vote Review 4");
        return Arrays.asList(rv1, rv2, rv3, rv4);
    }
}