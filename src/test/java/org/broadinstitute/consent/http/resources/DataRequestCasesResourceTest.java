package org.broadinstitute.consent.http.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataRequestCasesResourceTest {

    @Mock
    ElectionService electionService;

    @Mock
    PendingCaseService pendingCaseService;

    @Mock
    SummaryService summaryService;

    private DataRequestCasesResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetDataRequestPendingCases() {
        when(pendingCaseService.describeDataRequestPendingCases(any())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.getDataRequestPendingCases(null, null);
        Assert.assertEquals(200, response.getStatus());
        List cases = ((List) response.getEntity());
        Assert.assertTrue(cases.isEmpty());
    }

    @Test
    public void testGetDataOwnerPendingCases() {
        when(pendingCaseService.describeDataOwnerPendingCases(anyInt(), anyObject())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.getDataOwnerPendingCases(null, null);
        Assert.assertEquals(200, response.getStatus());
        List cases = ((List) response.getEntity());
        Assert.assertTrue(cases.isEmpty());
    }

    @Test
    public void testGetDataOwnerPendingCasesError() {
        when(pendingCaseService.describeDataOwnerPendingCases(anyInt(), anyObject())).thenThrow(new ServerErrorException(500));
        initResource();
        Response response = resource.getDataOwnerPendingCases(null, null);
        Assert.assertEquals(500, response.getStatus());
    }

    @Test
    public void testGetDataRequestSummaryCases() {
        when(summaryService.describeDataRequestSummaryCases(any())).thenReturn(new Summary());
        initResource();
        Response response = resource.getDataRequestSummaryCases(null, null);
        Assert.assertEquals(200, response.getStatus());
        Summary summary = ((Summary) response.getEntity());
        Assert.assertNotNull(summary);
    }

    @Test
    public void testGetMatchSummaryCases() {
        when(summaryService.describeMatchSummaryCases()).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.getMatchSummaryCases(null);
        Assert.assertEquals(200, response.getStatus());
        List summaries = ((List) response.getEntity());
        Assert.assertTrue(summaries.isEmpty());
    }

    @Test
    public void testDescribeClosedElections() {
        when(electionService.describeClosedElectionsByType(any(), any())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.describeClosedElections(null);
        Assert.assertEquals(200, response.getStatus());
        List cases = ((List) response.getEntity());
        Assert.assertTrue(cases.isEmpty());
    }

    private void initResource() {
        resource = new DataRequestCasesResource(electionService, pendingCaseService, summaryService);
    }

}
