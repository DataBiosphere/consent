package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.MetricsDAO;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MetricsServiceTest {

  @Mock private MetricsDAO metricsDAO;

  private MetricsService service;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initService() {
    service = new MetricsService(metricsDAO);
  }

  @Test
  public void testGenerateDarDecisionMetricsNCase() {
    int darCount = RandomUtils.nextInt(1, 100);
    int datasetCount = RandomUtils.nextInt(1, 100);
    when(metricsDAO.findAllDars()).thenReturn(generateDars(darCount));
    when(metricsDAO.findDatasetsByIdList(any())).thenReturn(generateDatasets(datasetCount));
    when(metricsDAO.findLastElectionsByReferenceIds(any())).thenReturn(Collections.emptyList());
    when(metricsDAO.findMatchesForReferenceIds(any())).thenReturn(Collections.emptyList());
    when(metricsDAO.findAllDacsForElectionIds(any())).thenReturn(Collections.emptyList());
    initService();
    List<DarDecisionMetrics> metrics = service.generateDarDecisionMetrics();
    assertFalse(metrics.isEmpty());
    assertEquals(darCount, metrics.size());
  }

  private List<DataAccessRequest> generateDars(int count) {
    return IntStream.range(1, count+1)
        .mapToObj(
            i -> {
              String referenceId = UUID.randomUUID().toString();
              List<Integer> dataSetIds = Collections.singletonList(i);
              DataAccessRequest dar = new DataAccessRequest();
              dar.setId(count);
              dar.setReferenceId(referenceId);
              DataAccessRequestData data = new DataAccessRequestData();
              data.setDatasetId(dataSetIds);
              data.setReferenceId(referenceId);
              dar.setData(data);
              return dar;
            })
        .collect(Collectors.toList());
  }

  private List<DataSet> generateDatasets(int count) {
    return IntStream.range(1, count+1)
        .mapToObj(
            i -> {
              DataSet d = new DataSet();
              d.setAlias(count);
              d.setDataSetId(count);
              d.setName(UUID.randomUUID().toString());
              return d;
            })
        .collect(Collectors.toList());
  }
}
