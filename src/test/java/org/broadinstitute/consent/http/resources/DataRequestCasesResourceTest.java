package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.service.SummaryService;
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
    assertEquals(200, response.getStatus());
    Summary summary = ((Summary) response.getEntity());
    assertNotNull(summary);
  }

  @Test
  public void testGetMatchSummaryCases() {
    when(summaryService.describeMatchSummaryCases()).thenReturn(Collections.emptyList());
    initResource();
    Response response = resource.getMatchSummaryCases(null);
    assertEquals(200, response.getStatus());
    List summaries = ((List) response.getEntity());
    assertTrue(summaries.isEmpty());
  }

  private void initResource() {
    resource = new DataRequestCasesResource(summaryService);
  }

}
