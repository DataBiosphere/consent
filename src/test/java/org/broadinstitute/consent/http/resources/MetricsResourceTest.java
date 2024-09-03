package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsResourceTest {

  private final String darHeader = "DAR ID";

  private final String dacHeader = "DAC ID";

  @Mock
  private MetricsService service;

  private MetricsResource resource;

  private void initResource() {
    resource = new MetricsResource(service);
  }

  @Test
  void testGetDarMetricsData() {
    when(service.generateDecisionMetrics(Type.DAR)).thenReturn(Collections.emptyList());
    when(service.getHeaderRow(Type.DAR)).thenReturn(darHeader);
    initResource();
    Response response = resource.getDarMetricsData();
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
    assertTrue(
        response.getEntity().toString().contains(service.getHeaderRow(Type.DAR)));
  }

  @Test
  void testGetDacMetricsData() {
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
  void testGetDatasetMetricsData() {
    DatasetMetrics metrics = new DatasetMetrics();
    when(service.generateDatasetMetrics(any())).thenReturn(metrics);
    initResource();
    Response response = resource.getDatasetMetricsData(1);
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
  }

  @Test
  void testGetDatasetMetricsDataNotFound() {
    when(service.generateDatasetMetrics(any())).thenThrow(new NotFoundException());
    initResource();
    Response response = resource.getDatasetMetricsData(1);
    assertEquals(404, response.getStatus());
  }
}
