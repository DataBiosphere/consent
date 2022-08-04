package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    private void initResource() {
        resource = new DataRequestCasesResource(electionService, pendingCaseService, summaryService);
    }

}
