package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.ws.rs.core.Response;
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
  public void testGetDarMetricsData() {
    when(service.generateDecisionMetrics("dar")).thenReturn(Collections.emptyList());
    initResource();
    Response response = resource.getDarMetricsData();
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
    assertTrue(response.getEntity().toString().contains(service.getHeaderRow("dar")));
  }

  @Test
  public void testGetDacMetricsData() {
    when(service.generateDecisionMetrics("dac")).thenReturn(Collections.emptyList());
    initResource();
    Response response = resource.getDarMetricsData();
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
    String headerRow = service.getHeaderRow("dac");
    assertTrue(response.getEntity().toString().contains(headerRow));
  }
}
