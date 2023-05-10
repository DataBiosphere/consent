package org.broadinstitute.consent.http.resources;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.ConsentSummaryDetail;
import org.broadinstitute.consent.http.models.DataAccessRequestSummaryDetail;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.SummaryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ConsentCasesResourceTest {

    @Mock
    SummaryService summaryService;

    private ConsentCasesResource resource;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetConsentSummaryCases() {
        when(summaryService.describeConsentSummaryCases()).thenReturn(new Summary());
        initResource();
        Response response = resource.getConsentSummaryCases(null);
        Assertions.assertEquals(200, response.getStatus());
        Object summary = response.getEntity();
        Assertions.assertNotNull(summary);
    }

    @Test
    public void testGetConsentSummaryDetailFileInvalid() {
        initResource();
        Response response = resource.getConsentSummaryDetailFile(UUID.randomUUID().toString(), null);
        Assertions.assertEquals(200, response.getStatus());
        Object summaryFile = response.getEntity();
        Assertions.assertNull(summaryFile);
    }

    @Test
    public void testGetConsentSummaryDetailFileDUL() {
        List<ConsentSummaryDetail> details = Collections.emptyList();
        when(summaryService.describeConsentSummaryDetail()).thenReturn(details);
        initResource();
        Response response = resource.getConsentSummaryDetailFile(ElectionType.TRANSLATE_DUL.getValue(), null);
        Assertions.assertEquals(200, response.getStatus());
        Object summaryDetails = response.getEntity();
        Assertions.assertNull(summaryDetails);
    }

    @Test
    public void testGetConsentSummaryDetailFileDataAccess() {
        List<DataAccessRequestSummaryDetail> details = Collections.emptyList();
        when(summaryService.listDataAccessRequestSummaryDetails()).thenReturn(details);
        initResource();
        Response response = resource.getConsentSummaryDetailFile(ElectionType.DATA_ACCESS.getValue(), null);
        Assertions.assertEquals(200, response.getStatus());
        Object summaryDetails = response.getEntity();
        Assertions.assertNull(summaryDetails);
    }

    @Test
    public void testGetConsentSummaryDetailFileNoSelection() {
        List<DataAccessRequestSummaryDetail> details = Collections.emptyList();
        when(summaryService.listDataAccessRequestSummaryDetails()).thenReturn(details);
        initResource();
        Response response = resource.getConsentSummaryDetailFile(null, null);
        Assertions.assertEquals(200, response.getStatus());
        Object summaryDetails = response.getEntity();
        Assertions.assertNull(summaryDetails);
    }

    private void initResource() {
        resource = new ConsentCasesResource(summaryService);
    }

}
