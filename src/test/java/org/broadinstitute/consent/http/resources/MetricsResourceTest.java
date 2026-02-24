package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsResourceTest extends AbstractTestHelper {

  @Mock private MetricsService service;
  @Mock private DuosUser duosUser;

  private MetricsResource resource;

  @BeforeEach
  void setUp() {
    resource = new MetricsResource(service);
  }

  @Test
  void testGetDatasetMetricsData() {
    DatasetMetrics metrics = new DatasetMetrics();
    when(service.generateDatasetMetrics(any())).thenReturn(metrics);

    @SuppressWarnings("removal")
    Response response = resource.getDatasetMetricsData(1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertFalse(response.getEntity().toString().isEmpty());
  }

  @Test
  void testGetDatasetMetricsDataNotFound() {
    when(service.generateDatasetMetrics(any())).thenThrow(new NotFoundException());

    @SuppressWarnings("removal")
    Response response = resource.getDatasetMetricsData(1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGenerateDarSummaries() {
    DataAccessRequest dar = generateDar();
    when(service.generateDarSummaries(any())).thenReturn(List.of(new DarMetricsSummary(dar)));

    Response response = resource.getDarSummaryData(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGenerateDarSummariesNotFound() {
    when(service.generateDarSummaries(any())).thenThrow(new NotFoundException());

    Response response = resource.getDarSummaryData(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  private DataAccessRequest generateDar() {
    String referenceId = UUID.randomUUID().toString();
    List<Integer> datasetIds = Collections.singletonList(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setId(1);
    dar.setReferenceId(referenceId);
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setDatasetIds(datasetIds);
    data.setReferenceId(referenceId);
    data.setProjectTitle(UUID.randomUUID().toString());
    dar.setDarCode("DAR-" + randomInt(1, 100));
    dar.setData(data);
    return dar;
  }
}
