package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.ConsentSummaryDetail;
import org.broadinstitute.consent.http.models.DataAccessRequestSummaryDetail;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.PendingCaseService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

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
        openMocks(this);
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
        assertNotNull(summary);
    }

    @Test
    public void testGetConsentSummaryDetailFileInvalid() {
        initResource();
        Response response = resource.getConsentSummaryDetailFile(UUID.randomUUID().toString(), null);
        Assert.assertEquals(200, response.getStatus());
        Object summaryFile = response.getEntity();
        Assert.assertNull(summaryFile);
    }

    @Test
    public void testGetConsentSummaryDetailFileDUL() {
        List<ConsentSummaryDetail> details = Collections.emptyList();
        when(summaryService.describeConsentSummaryDetail()).thenReturn(details);
        initResource();
        Response response = resource.getConsentSummaryDetailFile(ElectionType.TRANSLATE_DUL.getValue(), null);
        Assert.assertEquals(200, response.getStatus());
        Object summaryDetails = response.getEntity();
        assertNull(summaryDetails);
    }

    @Test
    public void testGetConsentSummaryDetailFileDataAccess() {
        List<DataAccessRequestSummaryDetail> details = Collections.emptyList();
        when(summaryService.listDataAccessRequestSummaryDetails()).thenReturn(details);
        initResource();
        Response response = resource.getConsentSummaryDetailFile(ElectionType.DATA_ACCESS.getValue(), null);
        Assert.assertEquals(200, response.getStatus());
        Object summaryDetails = response.getEntity();
        assertNull(summaryDetails);
    }

    @Test
    public void testGetConsentSummaryDetailFileNoSelection() {
        List<DataAccessRequestSummaryDetail> details = Collections.emptyList();
        when(summaryService.listDataAccessRequestSummaryDetails()).thenReturn(details);
        initResource();
        Response response = resource.getConsentSummaryDetailFile(null, null);
        Assert.assertEquals(200, response.getStatus());
        Object summaryDetails = response.getEntity();
        assertNull(summaryDetails);
    }

    private void initResource() {
        resource = new ConsentCasesResource(electionService, pendingCaseService, summaryService);
    }

}
