package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.models.Election;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ReviewResultsServiceTest {

    @Mock
    private ElectionDAO electionDAO;

    private ReviewResultsService service;

    private final Election sampleElection = new Election();

    private void initService() {
        service = new ReviewResultsService(electionDAO);
    }

    @Before
    public void setUp(){
        openMocks(this);
        when(electionDAO.findElectionWithFinalVoteById(anyInt())).thenReturn(sampleElection);
    }

    @Test
    public void testOpenElections() throws Exception {
        initService();
        when(electionDAO.verifyOpenElections()).thenReturn(1);
        assertTrue("There are open elections", service.openElections());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        assertFalse("There aren't open elections", service.openElections());
    }

}
