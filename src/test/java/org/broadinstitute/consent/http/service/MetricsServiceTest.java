package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest extends AbstractTestHelper {

  @Mock private DatasetDAO dataSetDAO;

  @Mock private DataAccessRequestDAO darDAO;

  private MetricsService service;

  @BeforeEach
  void initService() {
    service = new MetricsService(dataSetDAO, darDAO);
  }

  @Test
  void testGenerateDarSummaries() {
    DataAccessRequest dar = generateDar();
    Dataset dataset = generateDataset();

    when(dataSetDAO.findDatasetById(dataset.getDatasetId())).thenReturn(dataset);
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(dar));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId());

    assertEquals(dar.getData().getProjectTitle(), metrics.getFirst().projectTitle());
    assertEquals(dar.getDarCode(), metrics.getFirst().darCode());
    verify(dataSetDAO).findDatasetById(dataset.getDatasetId());
    verify(darDAO).findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(dataset.getDatasetId());
  }

  @Test
  void testGenerateDarSummariesNotFound() {
    when(dataSetDAO.findDatasetById(any())).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.generateDarSummaries(1));
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

  private Dataset generateDataset() {
    Dataset d = new Dataset();
    d.setAlias(1);
    d.setDatasetId(1);
    d.setName(UUID.randomUUID().toString());
    return d;
  }
}
