package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.AbstractPendingCaseAPI;
import org.broadinstitute.consent.http.service.AbstractSummaryAPI;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseAPI;
import org.broadinstitute.consent.http.service.SummaryAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractPendingCaseAPI.class,
        AbstractSummaryAPI.class
})
public class DataRequestCasesResourceTest {

    @Mock
    ElectionService electionService;

    @Mock
    PendingCaseAPI pendingCaseApi;

    @Mock
    SummaryAPI summaryApi;

    private DataRequestCasesResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractPendingCaseAPI.class);
        PowerMockito.mockStatic(AbstractSummaryAPI.class);
        when(AbstractPendingCaseAPI.getInstance()).thenReturn(pendingCaseApi);
        when(AbstractSummaryAPI.getInstance()).thenReturn(summaryApi);
    }

    @Test
    public void testGetDataRequestPendingCases() {
        when(pendingCaseApi.describeDataRequestPendingCases(any())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.getDataRequestPendingCases(null, null);
        Assert.assertEquals(200, response.getStatus());
        List cases = ((List) response.getEntity());
        Assert.assertTrue(cases.isEmpty());
    }

    @Test
    public void testGetDataOwnerPendingCases() {
        when(pendingCaseApi.describeDataOwnerPendingCases(any())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.getDataOwnerPendingCases(null, null);
        Assert.assertEquals(200, response.getStatus());
        List cases = ((List) response.getEntity());
        Assert.assertTrue(cases.isEmpty());
    }

    @Test
    public void testGetDataOwnerPendingCasesError() {
        when(pendingCaseApi.describeDataOwnerPendingCases(any())).thenThrow(new ServerErrorException(500));
        initResource();
        Response response = resource.getDataOwnerPendingCases(null, null);
        Assert.assertEquals(500, response.getStatus());
    }

    @Test
    public void testGetDataRequestSummaryCases() {
        when(summaryApi.describeDataRequestSummaryCases(any())).thenReturn(new Summary());
        initResource();
        Response response = resource.getDataRequestSummaryCases(null, null);
        Assert.assertEquals(200, response.getStatus());
        Summary summary = ((Summary) response.getEntity());
        Assert.assertNotNull(summary);
    }

    @Test
    public void testGetMatchSummaryCases() {
        when(summaryApi.describeMatchSummaryCases()).thenReturn(Collections.emptyList());
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
        resource = new DataRequestCasesResource(electionService);
    }

}
