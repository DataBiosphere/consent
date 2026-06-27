package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.Dataset;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest extends AbstractTestHelper {

  @Mock private Jdbi jdbi;

  @Mock private DatasetDAO dataSetDAO;

  @Mock private DataAccessRequestDAO darDAO;

  private MetricsService service;

  @BeforeEach
  void initService() {
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(dataSetDAO);
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(darDAO);
    service = new MetricsService(jdbi);
  }

  @Test
  void testGenerateDarSummaries() {
    DarMetricsSummary summary = generateDarMetricsSummary();
    Dataset dataset = generateDataset();

    when(dataSetDAO.findDatasetById(dataset.getDatasetId())).thenReturn(dataset);
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(summary));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId());

    assertEquals(summary.projectTitle(), metrics.getFirst().projectTitle());
    assertEquals(summary.darCode(), metrics.getFirst().darCode());
    verify(dataSetDAO).findDatasetById(dataset.getDatasetId());
    verify(darDAO).findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(dataset.getDatasetId());
  }

  @Test
  void testGenerateDarSummariesNotFound() {
    when(dataSetDAO.findDatasetById(any())).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.generateDarSummaries(1));
  }

  private DarMetricsSummary generateDarMetricsSummary() {
    return new DarMetricsSummary(
        null,
        UUID.randomUUID().toString(),
        "DAR-" + randomInt(1, 100),
        null,
        UUID.randomUUID().toString(),
        false);
  }

  private Dataset generateDataset() {
    Dataset d = new Dataset();
    d.setAlias(1);
    d.setDatasetId(1);
    d.setName(UUID.randomUUID().toString());
    return d;
  }
}
