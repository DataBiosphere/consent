package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MetricsResourceTest {

  private final String darHeader = "DAR ID";

  private final String dacHeader = "DAC ID";

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
    when(service.generateDecisionMetrics(Type.DAR)).thenReturn(Collections.emptyList());
    when(service.getHeaderRow(Type.DAR)).thenReturn(darHeader);
    initResource();
    Response response = resource.getDarMetricsData();
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
    assertTrue(response.getEntity().toString().contains(service.getHeaderRow(Type.DAR)));
  }

  @Test
  public void testGetDacMetricsData() {
    when(service.generateDecisionMetrics(Type.DAC)).thenReturn(Collections.emptyList());
    when(service.getHeaderRow(Type.DAC)).thenReturn(dacHeader);
    initResource();
    Response response = resource.getDacMetricsData();
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
    String headerRow = service.getHeaderRow(Type.DAC);
    assertTrue(response.getEntity().toString().contains(headerRow));
  }

  @Test
  public void testGetDatasetMetricsData() {
    DatasetMetrics metrics = new DatasetMetrics();
    when(service.generateDatasetMetrics(any())).thenReturn(metrics);
    initResource();
    Response response = resource.getDatasetMetricsData(1);
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
  }

  @Test
  public void testGetDatasetMetricsDataNotFound() {
    when(service.generateDatasetMetrics(any())).thenThrow(new NotFoundException());
    initResource();
    Response response = resource.getDatasetMetricsData(1);
    assertEquals(404, response.getStatus());
  }
}
