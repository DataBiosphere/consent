package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsResourceTest {

  @Mock
  private MetricsService service;

  private MetricsResource resource;

  @BeforeEach
  void initResource() {
    resource = new MetricsResource(service);
  }

  @Test
  void testGetDatasetMetricsData() {
    DatasetMetrics metrics = new DatasetMetrics();
    when(service.generateDatasetMetrics(any())).thenReturn(metrics);

    Response response = resource.getDatasetMetricsData(1);
    assertEquals(200, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
  }

  @Test
  void testGetDatasetMetricsDataNotFound() {
    when(service.generateDatasetMetrics(any())).thenThrow(new NotFoundException());

    Response response = resource.getDatasetMetricsData(1);
    assertEquals(404, response.getStatus());
  }
}
