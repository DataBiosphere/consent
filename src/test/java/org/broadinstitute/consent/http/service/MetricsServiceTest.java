package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DecisionMetrics;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.service.MetricsService.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class MetricsServiceTest {

  @Mock private DacService dacService;

  @Mock private DatasetDAO dataSetDAO;

  @Mock private DataAccessRequestDAO darDAO;

  @Mock private MatchDAO matchDAO;

  @Mock private ElectionDAO electionDAO;

  @Mock private UserDAO userDAO;

  private MetricsService service;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initService() {
    service = new MetricsService(dacService, dataSetDAO, darDAO, matchDAO, electionDAO, userDAO);
  }

  @Test
  public void testGenerateDarDecisionMetricsNCase() {
    int darCount = RandomUtils.nextInt(1, 100);
    int datasetCount = RandomUtils.nextInt(1, 100);
    initializeMetricsDAOCalls(darCount, datasetCount);
    initService();
    List<? extends DecisionMetrics> metrics = service.generateDecisionMetrics(Type.DAR);
    assertFalse(metrics.isEmpty());
    assertEquals(darCount, metrics.size());
  }

  @Test
  public void testGenerateDacDecisionMetricsNCase() {
    int darCount = RandomUtils.nextInt(1, 100);
    int datasetCount = RandomUtils.nextInt(1, 100);
    initializeMetricsDAOCalls(darCount, datasetCount);

    initService();
    List<? extends DecisionMetrics> metrics = service.generateDecisionMetrics(Type.DAC);
    assertFalse(metrics.isEmpty());
  }

  @Test
  public void testGenerateDatasetMetrics() {
    List<DataAccessRequest> dars = generateDars(1);
    List<Election> election = generateElection(dars.get(0).getReferenceId());
    Set<DatasetDTO> dataset = new HashSet<>(generateDatasetDTO(1));

    when(dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(any())).thenReturn(dataset);
    when(darDAO.findAllDataAccessRequestsForDatasetMetrics(any())).thenReturn(dars);
    when(electionDAO.findLastElectionsByReferenceIdsAndType(any(), eq("DataAccess"))).thenReturn(election);

    initService();
    DatasetMetrics metrics = service.generateDatasetMetrics(1);

    assertEquals(metrics.getDars().get(0).projectTitle, toSummaries(dars).get(0).projectTitle);
    assertEquals(metrics.getElections(), election);
    assertEquals(metrics.getDataset(), dataset.iterator().next());
  }

  @Test(expected = NotFoundException.class)
  public void testGenerateDatasetMetricsNotFound() {
    when(dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(any())).thenReturn(new HashSet<>());

    initService();
    service.generateDatasetMetrics(1);

  }

  private void initializeMetricsDAOCalls(int darCount, int datasetCount) {
    when(darDAO.findAllDataAccessRequests()).thenReturn(generateDars(darCount));
    when(dataSetDAO.findDatasetsByIdList(any())).thenReturn(generateDatasets(datasetCount));
    when(electionDAO.findLastElectionsByReferenceIds(any())).thenReturn(Collections.emptyList());
    when(matchDAO.findMatchesForPurposeIds(any())).thenReturn(Collections.emptyList());
    when(electionDAO.findAllDacsForElectionIds(any())).thenReturn(Collections.emptyList());
    Dac dac = generateDac();
    when(dacService.findAllDacsWithMembers()).thenReturn(Collections.singletonList(dac));
    List<DatasetDTO> datasetDTOS = generateDatasetDTO(datasetCount);
    when(dataSetDAO.findDatasetsWithDacs()).thenReturn(new HashSet<>(datasetDTOS));
  }

  private Dac generateDac() {
    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setDescription("description");
    dac.setName("dac1");
    User chairUser = new User();
    chairUser.setDacUserId(1);
    chairUser.setEmail("chair@test.org");
    chairUser.setDisplayName("Chair");
    UserRole chairRole =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairUser.setRoles(Collections.singletonList(chairRole));
    User memberUser = new User();
    memberUser.setDacUserId(2);
    memberUser.setEmail("member@test.org");
    memberUser.setDisplayName("Member");
    UserRole memberRole =
        new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
    memberUser.setRoles(Collections.singletonList(memberRole));
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
              dto.setDataSetId(ds.getDataSetId());
              DataSetPropertyDTO name = new DataSetPropertyDTO("Dataset Name", ds.getName());
              DataSetPropertyDTO consent = new DataSetPropertyDTO("Consent ID", ds.getName());
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
              List<Integer> dataSetIds = Collections.singletonList(i);
              DataAccessRequest dar = new DataAccessRequest();
              dar.setId(count);
              dar.setReferenceId(referenceId);
              DataAccessRequestData data = new DataAccessRequestData();
              data.setDatasetIds(dataSetIds);
              data.setReferenceId(referenceId);
              data.setProjectTitle(UUID.randomUUID().toString());
              dar.setData(data);
              return dar;
            })
        .collect(Collectors.toList());
  }

  private List<DarMetricsSummary> toSummaries(List<DataAccessRequest> dars) {
    return dars.stream().map(dar -> service.new DarMetricsSummary(dar)).collect(Collectors.toList());
  }

  private List<DataSet> generateDatasets(int count) {
    return IntStream.range(1, count + 1)
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
