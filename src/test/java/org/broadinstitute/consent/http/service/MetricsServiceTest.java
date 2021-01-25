package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.MetricsDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class MetricsServiceTest {

  @Mock private DacService dacService;

  @Mock private DataSetDAO dataSetDAO;

  @Mock private MetricsDAO metricsDAO;

  private MetricsService service;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initService() {
    service = new MetricsService(dacService, dataSetDAO, metricsDAO);
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

  private void initializeMetricsDAOCalls(int darCount, int datasetCount) {
    when(metricsDAO.findAllDars()).thenReturn(generateDars(darCount));
    when(metricsDAO.findDatasetsByIds(any())).thenReturn(generateDatasets(datasetCount));
    when(metricsDAO.findLastElectionsByReferenceIds(any())).thenReturn(Collections.emptyList());
    when(metricsDAO.findMatchesForPurposeIds(any())).thenReturn(Collections.emptyList());
    when(metricsDAO.findAllDacsForElectionIds(any())).thenReturn(Collections.emptyList());
    Dac dac = generateDac();
    when(dacService.findAllDacsWithMembers()).thenReturn(Collections.singletonList(dac));
    List<DataSetDTO> datasetDTOs = generateDatasetDTO(datasetCount);
    when(dataSetDAO.findDatasetsWithDacs()).thenReturn(new HashSet<>(datasetDTOs));
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

  private List<DataSetDTO> generateDatasetDTO(int datasetCount) {
    Dac dac = generateDac();
    return generateDatasets(datasetCount).stream()
        .map(
            ds -> {
              DataSetDTO dto = new DataSetDTO();
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
              dar.setData(data);
              return dar;
            })
        .collect(Collectors.toList());
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
}
