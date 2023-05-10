package org.broadinstitute.consent.http.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.SummaryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DataRequestCasesResourceTest {

    @Mock
    SummaryService summaryService;

    private DataRequestCasesResource resource;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetDataRequestSummaryCases() {
        when(summaryService.describeDataRequestSummaryCases(any())).thenReturn(new Summary());
        initResource();
        Response response = resource.getDataRequestSummaryCases(null, null);
        Assertions.assertEquals(200, response.getStatus());
        Summary summary = ((Summary) response.getEntity());
        Assertions.assertNotNull(summary);
    }

    @Test
    public void testGetMatchSummaryCases() {
        when(summaryService.describeMatchSummaryCases()).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.getMatchSummaryCases(null);
        Assertions.assertEquals(200, response.getStatus());
        List summaries = ((List) response.getEntity());
        Assertions.assertTrue(summaries.isEmpty());
    }

    private void initResource() {
        resource = new DataRequestCasesResource(summaryService);
    }

}
