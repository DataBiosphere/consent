package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.DecisionMetrics;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

  @Mock
  private DacService dacService;

  @Mock
  private DatasetDAO dataSetDAO;

  @Mock
  private DataAccessRequestDAO darDAO;

  @Mock
  private DarCollectionDAO darCollectionDAO;

  @Mock
  private MatchDAO matchDAO;

  @Mock
  private ElectionDAO electionDAO;

  private MetricsService service;

  private void initService() {
    service = new MetricsService(dacService, dataSetDAO, darDAO, darCollectionDAO, matchDAO,
        electionDAO);
  }

  @Test
  void testGenerateDarDecisionMetricsNCase() {
    int darCount = RandomUtils.nextInt(1, 100);
    int datasetCount = RandomUtils.nextInt(1, 100);
    initializeMetricsDAOCalls(darCount, datasetCount);
    initService();
    List<? extends DecisionMetrics> metrics = service.generateDecisionMetrics(Type.DAR);
    assertFalse(metrics.isEmpty());
    assertEquals(darCount, metrics.size());
  }

  @Test
  void testGenerateDacDecisionMetricsNCase() {
    int darCount = RandomUtils.nextInt(1, 100);
    int datasetCount = RandomUtils.nextInt(1, 100);
    initializeMetricsDAOCalls(darCount, datasetCount);
    Dac dac = generateDac();
    when(dacService.findAllDacsWithMembers()).thenReturn(Collections.singletonList(dac));
    List<DatasetDTO> datasetDTOS = generateDatasetDTO(datasetCount);
    when(dataSetDAO.findDatasetsWithDacs()).thenReturn(new HashSet<>(datasetDTOS));
    initService();

    List<? extends DecisionMetrics> metrics = service.generateDecisionMetrics(Type.DAC);
    assertFalse(metrics.isEmpty());
  }

  @Test
  void testGenerateDatasetMetrics() {
    List<DataAccessRequest> dars = generateDars(1);
    List<Election> election = generateElection(dars.get(0).getReferenceId());
    Set<DatasetDTO> dataset = new HashSet<>(generateDatasetDTO(1));
    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-" + RandomUtils.nextInt(1, 999999999));

    when(dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(any())).thenReturn(dataset);
    when(darDAO.findApprovedDARsByDatasetId(any())).thenReturn(dars);
    when(darCollectionDAO.findDARCollectionByCollectionIds(any())).thenReturn(List.of(collection));
    when(electionDAO.findLastElectionsByReferenceIdsAndType(any(), eq("DataAccess"))).thenReturn(
        election);

    initService();
    DatasetMetrics metrics = service.generateDatasetMetrics(1);

    assertEquals(metrics.getDars().get(0).projectTitle,
        dars.get(0).getData().getProjectTitle());
    assertEquals(metrics.getDars().get(0).darCode, collection.getDarCode());
    assertEquals(metrics.getElections(), election);
    assertEquals(metrics.getDataset(), dataset.iterator().next());
  }

  @Test
  void testGenerateDatasetMetricsNotFound() {
    when(dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(any())).thenReturn(new HashSet<>());

    initService();
    assertThrows(NotFoundException.class, () -> {
      service.generateDatasetMetrics(1);
    });
  }

  private void initializeMetricsDAOCalls(int darCount, int datasetCount) {
    when(darDAO.findAllDataAccessRequests()).thenReturn(generateDars(darCount));
    when(dataSetDAO.findDatasetsByIdList(any())).thenReturn(generateDatasets(datasetCount));
    when(electionDAO.findLastElectionsByReferenceIds(any())).thenReturn(Collections.emptyList());
    when(matchDAO.findMatchesForPurposeIds(any())).thenReturn(Collections.emptyList());
    when(electionDAO.findAllDacsForElectionIds(any())).thenReturn(Collections.emptyList());
  }

  private Dac generateDac() {
    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setDescription("description");
    dac.setName("dac1");
    User chairUser = new User();
    chairUser.setUserId(1);
    chairUser.setEmail("chair@test.org");
    chairUser.setDisplayName("Chair");
    chairUser.setChairpersonRole();
    User memberUser = new User();
    memberUser.setUserId(2);
    memberUser.setEmail("member@test.org");
    memberUser.setDisplayName("Member");
    memberUser.setMemberRole();
    dac.setChairpersons(Collections.singletonList(chairUser));
    dac.setMembers(Collections.singletonList(memberUser));
    return dac;
  }

  private List<DatasetDTO> generateDatasetDTO(int datasetCount) {
    Dac dac = generateDac();
    return generateDatasets(datasetCount).stream()
        .map(
            ds -> {
              DatasetDTO dto = new DatasetDTO();
              dto.setDacId(dac.getDacId());
              dto.setAlias(ds.getAlias());
              dto.setDatasetId(ds.getDatasetId());
              DatasetPropertyDTO name = new DatasetPropertyDTO("Dataset Name", ds.getName());
              DatasetPropertyDTO consent = new DatasetPropertyDTO("Consent ID", ds.getName());
              dto.setProperties(Arrays.asList(name, consent));
              return dto;
            })
        .collect(Collectors.toList());
  }

  private List<DataAccessRequest> generateDars(int count) {
    return IntStream.range(1, count + 1)
        .mapToObj(
            i -> {
              String referenceId = UUID.randomUUID().toString();
              List<Integer> datasetIds = Collections.singletonList(i);
              DataAccessRequest dar = new DataAccessRequest();
              dar.setId(count);
              dar.setReferenceId(referenceId);
              DataAccessRequestData data = new DataAccessRequestData();
              dar.setDatasetIds(datasetIds);
              data.setReferenceId(referenceId);
              data.setProjectTitle(UUID.randomUUID().toString());
              dar.setData(data);
              return dar;
            })
        .collect(Collectors.toList());
  }

  private List<Dataset> generateDatasets(int count) {
    return IntStream.range(1, count + 1)
        .mapToObj(
            i -> {
              Dataset d = new Dataset();
              d.setAlias(count);
              d.setDatasetId(count);
              d.setName(UUID.randomUUID().toString());
              return d;
            })
        .collect(Collectors.toList());
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
