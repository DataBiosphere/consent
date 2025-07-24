package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.Election;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest extends AbstractTestHelper {

  @Mock
  private DatasetDAO dataSetDAO;

  @Mock
  private DataAccessRequestDAO darDAO;

  @Mock
  private DarCollectionDAO darCollectionDAO;

  @Mock
  private ElectionDAO electionDAO;

  private MetricsService service;

  private void initService() {
    service = new MetricsService(dataSetDAO, darDAO, darCollectionDAO, electionDAO);
  }

  @Test
  void testGenerateDatasetMetrics() {
    DataAccessRequest dar = generateDar();
    List<Election> election = generateElection(dar.getReferenceId());
    Dataset dataset = generateDataset();
    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-" + randomInt(1, 999999999));

    when(dataSetDAO.findDatasetById(dataset.getDatasetId())).thenReturn(dataset);
    when(darDAO.findApprovedDARsByDatasetId(any())).thenReturn(List.of(dar));
    when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(collection));
    when(electionDAO.findLastElectionsByReferenceIdsAndType(any(), eq("DataAccess"))).thenReturn(
        election);

    initService();
    DatasetMetrics metrics = service.generateDatasetMetrics(1);

    assertEquals(metrics.getDars().get(0).projectTitle, dar.getData().getProjectTitle());
    assertEquals(metrics.getDars().get(0).darCode, collection.getDarCode());
    assertEquals(metrics.getElections(), election);
    assertEquals(metrics.getDataset(), dataset);
  }

  @Test
  void testGenerateDatasetMetricsNotFound() {
    when(dataSetDAO.findDatasetById(any())).thenReturn(null);

    initService();
    assertThrows(NotFoundException.class, () -> {
      service.generateDatasetMetrics(1);
    });
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

  private List<Election> generateElection(String ref) {
    ArrayList<Election> list = new ArrayList<>();
    Election e = new Election();
    e.setElectionId(1);
    e.setReferenceId(ref);
    e.setElectionType("DataAccess");
    list.add(e);
    return list;
  }
}
