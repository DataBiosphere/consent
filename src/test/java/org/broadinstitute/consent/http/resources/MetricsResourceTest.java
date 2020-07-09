package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MetricsResourceTest {

  @Mock private MetricsService service;

  private MetricsResource resource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initResource() {
    resource = new MetricsResource(service);
  }

  @Test
  public void testGetMetricsData() {
    when(service.generateDarDecisionMetrics()).thenReturn(Collections.emptyList());
    initResource();
    Response response = resource.getMetricsData();
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
    assertTrue(response.getEntity().toString().contains(DarDecisionMetrics.getHeaderRow("\t")));
  }
}
