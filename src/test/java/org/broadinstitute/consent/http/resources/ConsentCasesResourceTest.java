package org.broadinstitute.consent.http.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
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

public class ConsentCasesResourceTest {

    @Mock
    ElectionService electionService;

    @Mock
    PendingCaseService pendingCaseService;

    @Mock
    SummaryService summaryService;

    private ConsentCasesResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetConsentPendingCases() {
        when(pendingCaseService.describeConsentPendingCases(any())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.getConsentPendingCases(null, null);
        Assert.assertEquals(200, response.getStatus());
        List cases = ((List) response.getEntity());
        Assert.assertTrue(cases.isEmpty());
    }

    @Test
    public void testGetConsentSummaryCases() {
        when(summaryService.describeConsentSummaryCases()).thenReturn(new Summary());
        initResource();
        Response response = resource.getConsentSummaryCases(null);
        Assert.assertEquals(200, response.getStatus());
        Object summary = response.getEntity();
        Assert.assertNotNull(summary);
    }


    @Test
    public void testDescribeClosedElections() {
        when(electionService.describeClosedElectionsByType(anyString(), notNull())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.describeClosedElections(null);
        Assert.assertEquals(200, response.getStatus());
        List elections = ((List) response.getEntity());
        Assert.assertTrue(elections.isEmpty());
    }

    private void initResource() {
        resource = new ConsentCasesResource(electionService, pendingCaseService, summaryService);
    }

}
