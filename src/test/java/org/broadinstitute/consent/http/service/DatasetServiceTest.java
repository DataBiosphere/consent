package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataCustodianEmail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

  private DatasetService datasetService;

  @Mock
  private DatasetDAO datasetDAO;
  @Mock
  private DacDAO dacDAO;
  @Mock
  private EmailService emailService;
  @Mock
  private OntologyService ontologyService;
  @Mock
  private StudyDAO studyDAO;
  @Mock
  private DatasetServiceDAO datasetServiceDAO;
  @Mock
  private UserDAO userDAO;

  private void initService() {
    datasetService = new DatasetService(datasetDAO, dacDAO, emailService,
      ontologyService, studyDAO, datasetServiceDAO, userDAO);
  }

  @Test
  void testCreateDataset() throws Exception {
    DatasetDTO test = getDatasetDTO();
    Dataset mockDataset = getDatasets().get(0);
    when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(any())).thenReturn(
        Collections.singleton(test));
    initService();

    DatasetDTO result = datasetService.createDatasetFromDatasetDTO(getDatasetDTO(),
        "Test Dataset 1",
        1);

    assertNotNull(result);
    assertEquals(mockDataset.getDataSetId(), result.getDataSetId());
    assertNotNull(result.getProperties());
    assertFalse(result.getProperties().isEmpty());
  }

  @Test
  void testDescribeDataSetsByReceiveOrder() {
    when(datasetDAO.findDatasetsByReceiveOrder(Collections.singletonList(1)))
        .thenReturn(new HashSet<>(getDatasetDTOs()));
    initService();

    Collection<DatasetDTO> dataSetsByReceiveOrder = datasetService.describeDataSetsByReceiveOrder(
        Collections.singletonList(1));
    assertNotNull(dataSetsByReceiveOrder);
    assertEquals(dataSetsByReceiveOrder.size(), getDatasetDTOs().size());
  }

  @Test
  void testDescribeDictionaryByReceiveOrder() {
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder())
        .thenReturn(new ArrayList<>(getDictionaries()));
    initService();

    Collection<Dictionary> dictionaries = datasetService.describeDictionaryByReceiveOrder();
    assertNotNull(dictionaries);
    assertEquals(dictionaries.stream().findFirst().orElseThrow().getReceiveOrder(),
        getDictionaries().stream().findFirst().orElseThrow().getReceiveOrder());
  }

  @Test
  void testFindDatasetsByDacIds() {
    when(datasetDAO.findDatasetsByDacIds(anyList())).thenReturn(Collections.emptySet());
    initService();

    datasetService.findDatasetsByDacIds(List.of(1, 2, 3));
  }

  @Test
  void testFindDatasetsByDacIdsEmptyList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetsByDacIds(Collections.emptyList());
    });
  }

  @Test
  void testFindDatasetsByDacIdsNullList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetsByDacIds(null);
    });
  }

  @Test
  void testFindDatasetListByDacIds() {
    when(datasetDAO.findDatasetListByDacIds(anyList())).thenReturn(List.of());
    initService();

    datasetService.findDatasetListByDacIds(List.of(1, 2, 3));
  }

  @Test
  void testFindDatasetListByDacIdsEmptyList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetListByDacIds(Collections.emptyList());
    });
  }

  @Test
  void testFindDatasetListByDacIdsNullList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetListByDacIds(null);
    });
  }

  @Test
  void testGetDatasetByName() {
    when(datasetDAO.getDatasetByName(getDatasets().get(0).getName().toLowerCase()))
        .thenReturn(getDatasets().get(0));
    initService();

    Dataset dataset = datasetService.getDatasetByName("Test Dataset 1");

    assertNotNull(dataset);
    assertEquals(dataset.getDataSetId(), getDatasets().get(0).getDataSetId());
  }

  @Test
  void testFindStudyNames() {
    when(datasetDAO.findAllStudyNames())
        .thenReturn(Set.of("Hi", "Hello"));
    initService();

    Set<String> returned = datasetService.findAllStudyNames();

    assertNotNull(returned);
    assertEquals(Set.of("Hi", "Hello"), returned);
  }

  @Test
  void testFindDatasetById() {
    when(datasetDAO.findDatasetById(getDatasets().get(0).getDataSetId()))
        .thenReturn(getDatasets().get(0));
    initService();

    Dataset dataset = datasetService.findDatasetById(1);

    assertNotNull(dataset);
    assertEquals(dataset.getName(), getDatasets().get(0).getName());
  }

  @Test
  void testProcessDatasetProperties() {
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    List<DatasetProperty> properties = datasetService
        .processDatasetProperties(1, getDatasetPropertiesDTO());

    assertEquals(properties.size(), getDatasetPropertiesDTO().size());
  }

  @Test
  void testFindInvalidProperties() {
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    List<DatasetPropertyDTO> input = getDatasetPropertiesDTO().stream()
        .peek(p -> p.setPropertyKey("Invalid Key"))
        .collect(Collectors.toList());

    List<DatasetPropertyDTO> properties = datasetService.findInvalidProperties(input);

    assertFalse(properties.isEmpty());
  }

  @Test
  void testFindDuplicateProperties() {
    initService();

    List<DatasetPropertyDTO> input = getDatasetPropertiesDTO();
    DatasetPropertyDTO duplicateProperty = input.get(0);
    input.add(duplicateProperty);

    List<DatasetPropertyDTO> properties = datasetService.findDuplicateProperties(input);

    assertFalse(properties.isEmpty());
    assertEquals(properties.get(0), duplicateProperty);
  }

  @Test
  void testGetDatasetDTO() {
    Set<DatasetDTO> set = new HashSet<>();
    set.add(getDatasetDTO());
    when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(anyInt())).thenReturn(set);
    initService();

    DatasetDTO datasetDTO = datasetService.getDatasetDTO(1);

    assertNotNull(datasetDTO);
    assertFalse(datasetDTO.getProperties().isEmpty());
  }

  @Test
  void testGetDatasetDTONotFound() {
    when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(anyInt())).thenReturn(
        Collections.emptySet());
    initService();
    assertThrows(NotFoundException.class, () -> {
      datasetService.getDatasetDTO(1);
    });
  }

  @Test
  void testFindDatasetByIdentifier() {
    Dataset d = new Dataset();
    d.setAlias(3);
    d.setDatasetIdentifier();
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(d);

    initService();
    assertEquals(d, datasetService.findDatasetByIdentifier("DUOS-000003"));
  }

  @Test
  void testFindDatasetByIdentifier_WrongIdentifier() {
    Dataset d = new Dataset();
    d.setAlias(3);
    d.setDatasetIdentifier();
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(d);

    initService();
    assertNull(datasetService.findDatasetByIdentifier("DUOS-0003"));
  }

  @Test
  void testFindDatasetByIdentifier_NoDataset() {
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(null);

    initService();
    assertNull(datasetService.findDatasetByIdentifier("DUOS-00003"));
  }

  @Test
  void testUpdateDatasetNotModified() {
    int datasetId = 1;
    DatasetDTO dataSetDTO = getDatasetDTO();
    Dataset dataset = getDatasets().get(0);
    dataSetDTO.setDatasetName(dataset.getName());
    Set<DatasetProperty> datasetProps = getDatasetProperties();
    List<DatasetPropertyDTO> dtoProps = datasetProps.stream().map(p ->
        new DatasetPropertyDTO(p.getPropertyName(), p.getPropertyValue().toString())
    ).collect(Collectors.toList());
    dataSetDTO.setProperties(dtoProps);
    dataset.setProperties(datasetProps);
    when(datasetDAO.findDatasetById(datasetId)).thenReturn(dataset);
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> notModified = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertEquals(Optional.empty(), notModified);
  }


  @Test
  void testUpdateDatasetDataUseAdmin() {
    doNothing().when(datasetDAO).updateDatasetDataUse(any(), any());
    when(datasetDAO.findDatasetById(any())).thenReturn(new Dataset());
    initService();
    User u = new User();
    u.setAdminRole();
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    try {
      datasetService.updateDatasetDataUse(u, 1, dataUse);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testUpdateDatasetDataUseNonAdmin() {
    when(datasetDAO.findDatasetById(any())).thenReturn(new Dataset());
    initService();
    User u = new User();
    Stream.of(
        UserRoles.CHAIRPERSON,
        UserRoles.MEMBER,
        UserRoles.RESEARCHER,
        UserRoles.SIGNINGOFFICIAL,
        UserRoles.DATASUBMITTER,
        UserRoles.ITDIRECTOR,
        UserRoles.ALUMNI
    ).forEach(r -> {
      u.addRole(new UserRole(r.getRoleId(), r.getRoleName()));
    });
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    try {
      datasetService.updateDatasetDataUse(u, 1, dataUse);
      fail(
          "Should have thrown an exception on datasetService.updateDatasetDataUse()");
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
  }

  @Test
  void testUpdateDatasetMultiFieldUpdateOnly() {
    int datasetId = 1;
    DatasetDTO dataSetDTO = getDatasetDTO();
    Dataset dataset = getDatasets().get(0);
    dataset.setProperties(getDatasetProperties());
    dataSetDTO.setDatasetName(dataset.getName());

    List<DatasetPropertyDTO> updatedProperties = getDatasetPropertiesDTO();
    updatedProperties.get(2).setPropertyValue("updated value");
    updatedProperties.get(3).setPropertyValue("updated value");
    dataSetDTO.setProperties(updatedProperties);

    when(datasetDAO.findDatasetById(datasetId)).thenReturn(dataset);
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
  }

  @Test
  void testUpdateDatasetMultiFieldAddOnly() {
    int datasetId = 1;
    DatasetDTO dataSetDTO = getDatasetDTO();
    Dataset dataset = getDatasets().get(0);
    dataSetDTO.setDatasetName(dataset.getName());
    List<DatasetProperty> properties = new ArrayList<>(getDatasetProperties());
    properties.remove(2);
    properties.remove(2);
    dataset.setProperties(new HashSet<>(properties));

    List<DatasetPropertyDTO> updatedProperties = getDatasetPropertiesDTO();
    updatedProperties.get(2).setPropertyValue("added value");
    updatedProperties.get(3).setPropertyValue("added value");
    dataSetDTO.setProperties(updatedProperties);

    when(datasetDAO.findDatasetById(datasetId)).thenReturn(dataset);
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
  }

  @Test
  void testUpdateDatasetMultiFieldDeleteOnly() {
    int datasetId = 1;
    DatasetDTO dataSetDTO = getDatasetDTO();
    Dataset dataset = getDatasets().get(0);
    dataset.setProperties(getDatasetProperties());
    dataSetDTO.setDatasetName(dataset.getName());

    List<DatasetPropertyDTO> updatedProperties = getDatasetPropertiesDTO();
    updatedProperties.remove(2);
    updatedProperties.remove(2);
    dataSetDTO.setProperties(updatedProperties);

    when(datasetDAO.findDatasetById(datasetId)).thenReturn(dataset);
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
  }

  @Test
  void testUpdateDatasetNameModified() {
    int datasetId = 1;
    DatasetDTO datasetDTO = getDatasetDTO();
    Dataset dataset = getDatasets().get(0);

    //dataset properties are the same between the existing dataset and the update datasetDTO - no modification
    Set<DatasetProperty> datasetProps = getDatasetProperties();
    List<DatasetPropertyDTO> dtoProps = datasetProps.stream().map(p ->
        new DatasetPropertyDTO(p.getPropertyKey().toString(), p.getPropertyValue().toString())
    ).collect(Collectors.toList());
    datasetDTO.setProperties(dtoProps);
    dataset.setProperties(datasetProps);

    //datasetDTO given the updated name - existing dataset requires name to be modified
    String name = RandomStringUtils.randomAlphabetic(10);
    datasetDTO.setDatasetName(name);

    when(datasetDAO.findDatasetById(datasetId)).thenReturn(dataset);
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(datasetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
    verify(datasetDAO, times(1)).updateDataset(eq(datasetId), eq(name), any(), any(), any());
  }

  @Test
  void testSearchDatasetsControlledAccess() {
    Dataset ds1 = new Dataset();
    ds1.setName("asdf1234");
    ds1.setAlias(3);
    DatasetProperty ds1PI = new DatasetProperty();
    ds1PI.setPropertyName("Species");
    ds1PI.setPropertyValue("human");
    ds1PI.setPropertyType(PropertyType.String);
    ds1.setProperties(Set.of(ds1PI));

    Dataset ds2 = new Dataset();
    ds2.setName("ghjk5678");
    ds2.setAlias(280);
    DatasetProperty ds2PI = new DatasetProperty();
    ds2PI.setPropertyName("Species");
    ds2PI.setPropertyValue("human");
    ds2PI.setPropertyType(PropertyType.String);
    ds2.setProperties(Set.of(ds2PI));

    User u = new User();
    u.setAdminRole();

    when(datasetDAO.findAllDatasets()).thenReturn(List.of(ds1, ds2));

    initService();

    // query dataset name
    List<Dataset> results = datasetService.searchDatasets("asdf", AccessManagement.CONTROLLED, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds1));

    // query species
    results = datasetService.searchDatasets("human", AccessManagement.CONTROLLED, u);

    assertEquals(2, results.size());
    assertTrue(results.contains(ds1));
    assertTrue(results.contains(ds2));

    // query ds identifier
    results = datasetService.searchDatasets("DUOS-000280", AccessManagement.CONTROLLED, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds2));

    // query missing pi name
    results = datasetService.searchDatasets("Doe", AccessManagement.CONTROLLED, u);

    assertEquals(0, results.size());

    // search on two things at once
    results = datasetService.searchDatasets("human asdf", AccessManagement.CONTROLLED, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds1));

    // query nonexistent phrase
    results = datasetService.searchDatasets("asdflasdfasdfasdfhalskdjf",
        AccessManagement.CONTROLLED, u);
    assertEquals(0, results.size());
  }

  @Test
  void testSearchDatasetsOpenAccess() {
    Dataset ds1 = new Dataset();
    ds1.setName("string");
    ds1.setAlias(622);
    DatasetProperty ds1PI = new DatasetProperty();
    ds1PI.setPropertyName("Access Management");
    ds1PI.setPropertyValue("open");
    ds1PI.setPropertyType(PropertyType.String);
    ds1.setProperties(Set.of(ds1PI));

    Dataset ds2 = new Dataset();
    ds2.setName("TESTING NAME");
    ds2.setAlias(623);
    DatasetProperty ds2PI = new DatasetProperty();
    ds2PI.setPropertyName("Access Management");
    ds2PI.setPropertyValue("open");
    ds2PI.setPropertyType(PropertyType.String);
    ds2.setProperties(Set.of(ds2PI));

    User u = new User();
    u.setAdminRole();

    when(datasetDAO.findAllDatasets()).thenReturn(List.of(ds1, ds2));

    initService();

    // query dataset name
    List<Dataset> results = datasetService.searchDatasets("string", AccessManagement.OPEN, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds1));

    // query ds identifier
    results = datasetService.searchDatasets("DUOS-000623", AccessManagement.OPEN, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds2));

    // query nonexistent phrase
    results = datasetService.searchDatasets("asdflasdfasdfasdfhalskdjf", AccessManagement.OPEN, u);
    assertEquals(0, results.size());
  }

  @Test
  void testFindAllDatasetsByUser_Admin() {
    User user = new User();
    user.setAdminRole();
    Dataset dataset = new Dataset();
    dataset.setDataSetId(1);
    when(datasetDAO.findAllDatasets()).thenReturn(List.of(dataset));
    initService();

    List<Dataset> datasets = datasetService.findAllDatasetsByUser(user);
    assertFalse(datasets.isEmpty());
    assertEquals(1, datasets.size());
    assertEquals(dataset.getDataSetId(), datasets.get(0).getDataSetId());
    verify(datasetDAO, times(1)).findAllDatasets();
    verify(datasetDAO, times(0)).getDatasets();
    verify(datasetDAO, times(0)).findDatasetsByAuthUserEmail(any());
  }

  @Test
  void testFindAllDatasetsByUser_Chair() {
    User user = new User();
    user.setChairpersonRole();
    Dataset d1 = new Dataset();
    d1.setDataSetId(1);
    Dataset d2 = new Dataset();
    d2.setDataSetId(2);
    Dataset d3 = new Dataset();
    d2.setDataSetId(3);
    when(datasetDAO.getDatasets()).thenReturn(List.of(d1, d2));
    when(datasetDAO.findDatasetsByAuthUserEmail(any())).thenReturn(List.of(d2, d3));
    initService();

    List<Dataset> datasets = datasetService.findAllDatasetsByUser(user);
    assertFalse(datasets.isEmpty());
    // Test that the two lists of datasets are distinctly combined in the final result
    assertEquals(3, datasets.size());
    assertTrue(datasets.contains(d1));
    assertTrue(datasets.contains(d2));
    assertTrue(datasets.contains(d3));
    verify(datasetDAO, times(0)).findAllDatasets();
    verify(datasetDAO, times(1)).getDatasets();
    verify(datasetDAO, times(1)).findDatasetsByAuthUserEmail(any());
  }

  @Test
  void testFindAllDatasetsByUser() {
    User user = new User();
    user.setResearcherRole();
    Dataset d1 = new Dataset();
    d1.setDataSetId(1);
    Dataset d2 = new Dataset();
    d2.setDataSetId(2);
    when(datasetDAO.getDatasets()).thenReturn(List.of(d1, d2));
    initService();

    List<Dataset> datasets = datasetService.findAllDatasetsByUser(user);
    assertFalse(datasets.isEmpty());
    assertEquals(2, datasets.size());
    assertTrue(datasets.contains(d1));
    assertTrue(datasets.contains(d2));
    verify(datasetDAO, times(0)).findAllDatasets();
    verify(datasetDAO, times(1)).getDatasets();
    verify(datasetDAO, times(0)).findDatasetsByAuthUserEmail(any());
  }

  @Test
  void testApproveDataset_AlreadyApproved_TrueSubmission() throws Exception {
    Dataset dataset = new Dataset();
    User user = new User();
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    dataset.setDacApproval(true);
    dataset.setDataSetId(1);
    dataset.setUpdateDate(new Date());
    dataset.setUpdateUserId(4);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");
    initService();

    Dataset datasetResult = datasetService.approveDataset(dataset, user, true);
    assertNotNull(datasetResult);
    assertEquals(dataset.getDataSetId(), datasetResult.getDataSetId());
    assertEquals(dataset.getUpdateUserId(), datasetResult.getUpdateUserId());
    assertEquals(dataset.getDacApproval(), datasetResult.getDacApproval());
    assertEquals(dataset.getUpdateDate(), datasetResult.getUpdateDate());
    verify(emailService, times(0)).sendDatasetApprovedMessage(
        any(),
        any(),
        any()
    );
  }

  @Test
  void testApprovedDataset_AlreadyApproved_FalseSubmission() {
    Dataset dataset = new Dataset();
    User user = new User();
    dataset.setDacApproval(true);
    initService();

    assertThrows(IllegalArgumentException.class, () -> {
      datasetService.approveDataset(dataset, user, false);
    });
  }

  @Test
  void testApprovedDataset_AlreadyApproved_NullSubmission() {
    Dataset dataset = new Dataset();
    User user = new User();
    dataset.setDacApproval(true);
    initService();

    assertThrows(IllegalArgumentException.class, () -> {
      datasetService.approveDataset(dataset, user, null);
    });
  }

  @Test
  void testApproveDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(1);
    User user = new User();
    user.setUserId(1);
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    Boolean payloadBool = true;
    Dataset updatedDataset = new Dataset();
    updatedDataset.setDataSetId(1);
    updatedDataset.setDacApproval(payloadBool);

    when(datasetDAO.findDatasetById(any())).thenReturn(updatedDataset);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");
    initService();
    when(dacDAO.findById(3)).thenReturn(dac);

    Dataset returnedDataset = datasetService.approveDataset(dataset, user, payloadBool);
    assertEquals(dataset.getDataSetId(), returnedDataset.getDataSetId());
    assertTrue(returnedDataset.getDacApproval());

    // send approved email
    verify(emailService, times(1)).sendDatasetApprovedMessage(
        user,
        "DAC NAME",
        "DUOS-000001"
    );
  }

  @Test
  void testApproveDataset_DenyDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(1);
    User user = new User();
    user.setUserId(1);
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    Boolean payloadBool = false;
    Dataset updatedDataset = new Dataset();
    updatedDataset.setDataSetId(1);
    updatedDataset.setDacApproval(payloadBool);

    when(datasetDAO.findDatasetById(any())).thenReturn(updatedDataset);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");
    dac.setEmail("dacEmail@gmail.com");
    initService();
    when(dacDAO.findById(3)).thenReturn(dac);

    Dataset returnedDataset = datasetService.approveDataset(dataset, user, payloadBool);
    assertEquals(dataset.getDataSetId(), returnedDataset.getDataSetId());
    assertFalse(returnedDataset.getDacApproval());

    // send denied email
    verify(emailService, times(1)).sendDatasetDeniedMessage(
        user,
        "DAC NAME",
        "DUOS-000001",
        "dacEmail@gmail.com"
    );
  }

  @Test
  void testApproveDataset_DenyDataset_WithNoDACEmail() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(1);
    User user = new User();
    user.setUserId(1);
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    Boolean payloadBool = false;
    Dataset updatedDataset = new Dataset();
    updatedDataset.setDataSetId(1);
    updatedDataset.setDacApproval(payloadBool);

    when(datasetDAO.findDatasetById(any())).thenReturn(updatedDataset);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");
    initService();
    when(dacDAO.findById(3)).thenReturn(dac);

    Dataset returnedDataset = datasetService.approveDataset(dataset, user, payloadBool);
    assertEquals(dataset.getDataSetId(), returnedDataset.getDataSetId());
    assertFalse(returnedDataset.getDacApproval());

    // do not send denied email
    verify(emailService, times(0)).sendDatasetDeniedMessage(
        user,
        "DAC NAME",
        "DUOS-000001",
        ""
    );

  }

  @Test
  void testSyncDataUseTranslation() {
    Dataset ds = new Dataset();
    ds.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    when(datasetDAO.findDatasetById(1)).thenReturn(ds);
    String translation = """
        Samples are restricted for use under the following conditions:
        Data is limited for health/medical/biomedical research. [HMB]
        Data use is limited for studying: cancerophobia [DS]
        Commercial use is not prohibited.
        Data use for methods development research irrespective of the specified data use limitations is not prohibited.
        Restrictions for use as a control set for diseases other than those defined were not specified.
        """;
    when(ontologyService.translateDataUse(ds.getDataUse(),
        DataUseTranslationType.DATASET)).thenReturn(translation);

    initService();
    datasetService.syncDatasetDataUseTranslation(1);

    verify(datasetDAO, times(1)).updateDatasetTranslatedDataUse(1, translation);
  }

  @Test
  void testSyncDataUseTranslationNotFound() {
    when(datasetDAO.findDatasetById(1)).thenReturn(null);
    initService();
    assertThrows(NotFoundException.class, () -> {
      datasetService.syncDatasetDataUseTranslation(1);
    });

  }

  @Test
  void testGetApprovedDatasets() {
    User user = new User(1, "test@domain.com", "Test User", new Date(),
        List.of(UserRoles.Researcher()));
    ApprovedDataset example = new ApprovedDataset(1, "sampleDarId", "sampleName", "sampleDac",
        new Date());
    when(datasetDAO.getApprovedDatasets(anyInt())).thenReturn(List.of(example));
    initService();
    assertEquals(1, datasetService.getApprovedDatasets(user).size());
    assertTrue(datasetService.getApprovedDatasets(user).get(0).isApprovedDatasetEqual(example));
  }

  @Test
  void testUpdateStudyCustodiansExisting() {
    User user = new User();
    user.setEmail("test@gmail.com");
    Study study = new Study();
    study.setStudyId(RandomUtils.nextInt(100, 10000));
    StudyProperty prop = new StudyProperty();
    prop.setValue("[test@gmail.com]");
    prop.setStudyId(study.getStudyId());
    prop.setType(PropertyType.Json);
    prop.setKey(dataCustodianEmail);
    study.setProperties(Set.of(prop));
    when(studyDAO.findStudyById(any())).thenReturn(study);

    initService();
    datasetService.updateStudyCustodians(user, study.getStudyId(), "[new-user@test.com]");
    verify(studyDAO, times(1)).updateStudyProperty(any(), any(), any(), any());
    verify(studyDAO, never()).insertStudyProperty(any(), any(), any(), any());
  }

  @Test
  void testUpdateStudyCustodiansNew() {
    User user = new User();
    user.setEmail("test@gmail.com");
    Study study = new Study();
    study.setStudyId(RandomUtils.nextInt(100, 10000));
    when(studyDAO.findStudyById(any())).thenReturn(study);

    initService();
    datasetService.updateStudyCustodians(user, study.getStudyId(), "[new-user@test.com]");
    verify(studyDAO, never()).updateStudyProperty(any(), any(), any(), any());
    verify(studyDAO, times(1)).insertStudyProperty(any(), any(), any(), any());
  }

  /* Helper functions */

  private List<Dataset> getDatasets() {
    return IntStream.range(1, 3)
        .mapToObj(i -> {
          Dataset dataset = new Dataset();
          dataset.setDataSetId(i);
          dataset.setName("Test Dataset " + i);
          dataset.setProperties(Collections.emptySet());
          return dataset;
        }).collect(Collectors.toList());
  }

  private List<DatasetDTO> getDatasetDTOs() {
    return IntStream.range(1, 3)
        .mapToObj(i -> {
          DatasetDTO dataset = new DatasetDTO();
          dataset.setDataSetId(i);
          DatasetPropertyDTO nameProperty = new DatasetPropertyDTO("Dataset Name",
              "Test Dataset " + i);
          dataset.setProperties(Collections.singletonList(nameProperty));
          return dataset;
        }).collect(Collectors.toList());
  }

  private Set<DatasetProperty> getDatasetProperties() {
    return IntStream.range(1, 11)
        .mapToObj(i -> {
              DatasetProperty prop = new DatasetProperty(1,
                  i,
                  "Test Value" + RandomStringUtils.randomAlphanumeric(25),
                  PropertyType.String,
                  new Date());
              prop.setPropertyName(RandomStringUtils.randomAlphanumeric(15));
              prop.setPropertyId(i);
              return prop;
            }
        ).collect(Collectors.toSet());
  }

  private List<DatasetPropertyDTO> getDatasetPropertiesDTO() {
    List<Dictionary> dictionaries = getDictionaries();
    return dictionaries.stream()
        .map(d ->
            new DatasetPropertyDTO(d.getKey(), "Test Value")
        ).collect(Collectors.toList());
  }

  private DatasetDTO getDatasetDTO() {
    DatasetDTO datasetDTO = new DatasetDTO();
    datasetDTO.setDataSetId(1);
    datasetDTO.setObjectId("Test ObjectId");
    datasetDTO.setProperties(getDatasetPropertiesDTO());
    DataUse dataUse = new DataUse();
    dataUse.setGeneralUse(true);
    datasetDTO.setDataUse(dataUse);
    return datasetDTO;
  }

  private List<Dictionary> getDictionaries() {
    return IntStream.range(1, 11)
        .mapToObj(i ->
            new Dictionary(i, String.valueOf(i), true, i, i)
        ).collect(Collectors.toList());
  }

}
