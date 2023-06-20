package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatasetServiceTest {

  private DatasetService datasetService;

  @Mock
  private ConsentDAO consentDAO;

  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;

  @Mock
  private DatasetDAO datasetDAO;

  @Mock
  private DatasetServiceDAO datasetServiceDAO;

  @Mock
  private UserRoleDAO userRoleDAO;
  @Mock
  private DacDAO dacDAO;
  @Mock
  private UseRestrictionConverter useRestrictionConverter;
  @Mock
  private EmailService emailService;
  @Mock
  private OntologyService ontologyService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private void initService() {
    datasetService = new DatasetService(consentDAO, dataAccessRequestDAO, datasetDAO,
        datasetServiceDAO, userRoleDAO, dacDAO, useRestrictionConverter, emailService,
        ontologyService);
  }

  @Test
  public void testCreateDataset() throws Exception {
    DatasetDTO test = getDatasetDTO();
    Dataset mockDataset = getDatasets().get(0);
    when(datasetDAO.insertDataset(anyString(), any(), anyInt(), anyString(), anyBoolean(), any(),
        any())).thenReturn(mockDataset.getDataSetId());
    when(datasetDAO.findDatasetById(any())).thenReturn(mockDataset);
    when(datasetDAO.findDatasetPropertiesByDatasetId(any())).thenReturn(getDatasetProperties());
    when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(any())).thenReturn(
        Collections.singleton(test));
    initService();

    DatasetDTO result = datasetService.createDatasetWithConsent(getDatasetDTO(), "Test Dataset 1",
        1);

    assertNotNull(result);
    assertEquals(mockDataset.getDataSetId(), result.getDataSetId());
    assertNotNull(result.getProperties());
    assertFalse(result.getProperties().isEmpty());
  }

  @Test
  public void testDescribeDataSetsByReceiveOrder() {
    when(datasetDAO.findDatasetsByReceiveOrder(Collections.singletonList(1)))
        .thenReturn(new HashSet<>(getDatasetDTOs()));
    initService();

    Collection<DatasetDTO> dataSetsByReceiveOrder = datasetService.describeDataSetsByReceiveOrder(
        Collections.singletonList(1));
    assertNotNull(dataSetsByReceiveOrder);
    assertEquals(dataSetsByReceiveOrder.size(), getDatasetDTOs().size());
  }

  @Test
  public void testDescribeDictionaryByDisplayOrder() {
    when(datasetDAO.getMappedFieldsOrderByDisplayOrder())
        .thenReturn(new ArrayList<>(getDictionaries()));
    initService();

    Collection<Dictionary> dictionaries = datasetService.describeDictionaryByDisplayOrder();
    assertNotNull(dictionaries);
    assertEquals(dictionaries.stream().findFirst().orElseThrow().getDisplayOrder(),
        getDictionaries().stream().findFirst().orElseThrow().getDisplayOrder());
  }

  @Test
  public void testDescribeDictionaryByReceiveOrder() {
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder())
        .thenReturn(new ArrayList<>(getDictionaries()));
    initService();

    Collection<Dictionary> dictionaries = datasetService.describeDictionaryByReceiveOrder();
    assertNotNull(dictionaries);
    assertEquals(dictionaries.stream().findFirst().orElseThrow().getReceiveOrder(),
        getDictionaries().stream().findFirst().orElseThrow().getReceiveOrder());
  }

  @Test
  public void testDisableDataset() {
    Integer dataSetId = 1;
    when(datasetDAO.findDatasetById(dataSetId))
        .thenReturn(getDatasets().get(0));
    doNothing().when(datasetDAO).updateDatasetActive(any(), any());

    initService();

    datasetService.disableDataset(dataSetId, false);
  }

  @Test
  public void testFindDatasetsByDacIds() {
    when(datasetDAO.findDatasetsByDacIds(anyList())).thenReturn(Collections.emptySet());
    initService();

    datasetService.findDatasetsByDacIds(List.of(1, 2, 3));
  }

  @Test
  public void testFindDatasetsByDacIdsEmptyList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetsByDacIds(Collections.emptyList());
    });
  }

  @Test
  public void testFindDatasetsByDacIdsNullList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetsByDacIds(null);
    });
  }

  @Test
  public void testFindDatasetListByDacIds() {
    when(datasetDAO.findDatasetListByDacIds(anyList())).thenReturn(List.of());
    initService();

    datasetService.findDatasetListByDacIds(List.of(1, 2, 3));
  }

  @Test
  public void testFindDatasetListByDacIdsEmptyList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetListByDacIds(Collections.emptyList());
    });
  }

  @Test
  public void testFindDatasetListByDacIdsNullList() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      datasetService.findDatasetListByDacIds(null);
    });
  }

  @Test
  public void testUpdateNeedsReviewDataSets() {
    Integer dataSetId = 1;
    when(datasetDAO.findDatasetById(dataSetId))
        .thenReturn(getDatasets().get(0));
    doNothing().when(datasetDAO).updateDatasetNeedsApproval(any(), any());
    initService();

    Dataset dataSet = datasetService.updateNeedsReviewDatasets(dataSetId, true);
    assertNotNull(dataSet);
  }

  @Test
  public void testDeleteDataset() throws Exception {
    Integer dataSetId = 1;
    when(datasetDAO.findDatasetById(any()))
        .thenReturn(getDatasets().get(0));
    when(datasetDAO.insertDatasetAudit(any()))
        .thenReturn(1);
    doNothing().when(datasetDAO).deleteUserAssociationsByDatasetId(any());
    doNothing().when(datasetDAO).deleteDatasetsProperties(any());
    doNothing().when(datasetDAO).deleteConsentAssociationsByDatasetId(any());

    initService();
    datasetService.deleteDataset(dataSetId, 1);
  }

  @Test
  public void testGetDatasetByName() {
    when(datasetDAO.getDatasetByName(getDatasets().get(0).getName().toLowerCase()))
        .thenReturn(getDatasets().get(0));
    initService();

    Dataset dataset = datasetService.getDatasetByName("Test Dataset 1");

    assertNotNull(dataset);
    assertEquals(dataset.getDataSetId(), getDatasets().get(0).getDataSetId());
  }

  @Test
  public void testFindAllActiveStudyNames() {
    when(datasetDAO.findAllActiveStudyNames())
        .thenReturn(Set.of("Hi", "Hello"));
    initService();

    Set<String> returned = datasetService.findAllActiveStudyNames();

    assertNotNull(returned);
    assertEquals(Set.of("Hi", "Hello"), returned);
  }

  @Test
  public void testFindDatasetById() {
    when(datasetDAO.findDatasetById(getDatasets().get(0).getDataSetId()))
        .thenReturn(getDatasets().get(0));
    initService();

    Dataset dataset = datasetService.findDatasetById(1);

    assertNotNull(dataset);
    assertEquals(dataset.getName(), getDatasets().get(0).getName());
  }

  @Test
  public void testProcessDatasetProperties() {
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    List<DatasetProperty> properties = datasetService
        .processDatasetProperties(1, getDatasetPropertiesDTO());

    assertEquals(properties.size(), getDatasetPropertiesDTO().size());
  }

  @Test
  public void testFindInvalidProperties() {
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    List<DatasetPropertyDTO> input = getDatasetPropertiesDTO().stream()
        .peek(p -> p.setPropertyKey("Invalid Key"))
        .collect(Collectors.toList());

    List<DatasetPropertyDTO> properties = datasetService.findInvalidProperties(input);

    assertFalse(properties.isEmpty());
  }

  @Test
  public void testFindDuplicateProperties() {
    initService();

    List<DatasetPropertyDTO> input = getDatasetPropertiesDTO();
    DatasetPropertyDTO duplicateProperty = input.get(0);
    input.add(duplicateProperty);

    List<DatasetPropertyDTO> properties = datasetService.findDuplicateProperties(input);

    assertFalse(properties.isEmpty());
    assertEquals(properties.get(0), duplicateProperty);
  }

  @Test
  public void testGetDatasetDTO() {
    Set<DatasetDTO> set = new HashSet<>();
    set.add(getDatasetDTO());
    when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(anyInt())).thenReturn(set);
    initService();

    DatasetDTO datasetDTO = datasetService.getDatasetDTO(1);

    assertNotNull(datasetDTO);
    assertFalse(datasetDTO.getProperties().isEmpty());
  }

  @Test
  public void testGetDatasetDTONotFound() {
    when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(anyInt())).thenReturn(
        Collections.emptySet());
    initService();
    assertThrows(NotFoundException.class, () -> {
      datasetService.getDatasetDTO(1);
    });
  }

  @Test
  public void testFindDatasetByIdentifier() {
    Dataset d = new Dataset();
    d.setAlias(3);
    d.setDatasetIdentifier();
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(d);

    initService();
    assertEquals(d, datasetService.findDatasetByIdentifier("DUOS-000003"));
  }

  @Test
  public void testFindDatasetByIdentifier_WrongIdentifier() {
    Dataset d = new Dataset();
    d.setAlias(3);
    d.setDatasetIdentifier();
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(d);

    initService();
    assertNull(datasetService.findDatasetByIdentifier("DUOS-0003"));
  }

  @Test
  public void testFindDatasetByIdentifier_NoDataset() {
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(null);

    initService();
    assertNull(datasetService.findDatasetByIdentifier("DUOS-00003"));
  }

  @Test
  public void testUpdateDatasetNotModified() {
    int datasetId = 1;
    DatasetDTO dataSetDTO = getDatasetDTO();
    Dataset dataset = getDatasets().get(0);
    dataSetDTO.setDatasetName(dataset.getName());
    Set<DatasetProperty> datasetProps = getDatasetProperties();
    List<DatasetPropertyDTO> dtoProps = datasetProps.stream().map(p ->
        new DatasetPropertyDTO(p.getPropertyKey().toString(), p.getPropertyValue().toString())
    ).collect(Collectors.toList());
    dataSetDTO.setProperties(dtoProps);
    dataset.setProperties(datasetProps);
    when(datasetDAO.findDatasetById(datasetId)).thenReturn(dataset);
    when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(datasetProps);
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> notModified = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertEquals(Optional.empty(), notModified);
  }


  @Test
  public void testUpdateDatasetDataUseAdmin() {
    doNothing().when(datasetDAO).updateDatasetDataUse(any(), any());
    when(datasetDAO.findDatasetById(any())).thenReturn(new Dataset());
    initService();
    User u = new User();
    u.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    try {
      datasetService.updateDatasetDataUse(u, 1, dataUse);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testUpdateDatasetDataUseNonAdmin() {
    doNothing().when(datasetDAO).updateDatasetDataUse(any(), any());
    when(datasetDAO.findDatasetById(any())).thenReturn(new Dataset());
    initService();
    User u = new User();
    Stream.of(
        UserRoles.CHAIRPERSON,
        UserRoles.MEMBER,
        UserRoles.RESEARCHER,
        UserRoles.SIGNINGOFFICIAL,
        UserRoles.DATAOWNER,
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
  public void testUpdateDatasetMultiFieldUpdateOnly() {
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
    when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
  }

  @Test
  public void testUpdateDatasetMultiFieldAddOnly() {
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
    when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
  }

  @Test
  public void testUpdateDatasetMultiFieldDeleteOnly() {
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
    when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(dataSetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
  }

  @Test
  public void testUpdateDatasetNameModified() {
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
    when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(datasetProps);
    when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
    initService();

    Optional<Dataset> updated = datasetService.updateDataset(datasetDTO, datasetId, 1);
    assertNotNull(updated);
    assertTrue(updated.isPresent());
    verify(datasetDAO, times(1)).updateDataset(eq(datasetId), eq(name), any(), any(), any(), any());
  }

  @Test
  public void testCreateConsentForDataset() throws IOException {
    DatasetDTO dataSetDTO = getDatasetDTO();
    DataUse dataUse = new DataUseBuilder().build();
    dataSetDTO.setDataUse(dataUse);
    Consent consent = new Consent();
    when(consentDAO.findConsentById(anyString())).thenReturn(consent);
    doNothing().when(consentDAO)
        .insertConsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    doNothing().when(consentDAO).insertConsentAssociation(any(), any(), any());
    initService();

    Consent result = datasetService.createConsentForDataset(dataSetDTO);
    assertNotNull(result);
  }

  @Test
  public void testCreateConsentForDatasetNullDataUse() {
    DatasetDTO dataSetDTO = getDatasetDTO();
    dataSetDTO.setDataUse(null);
    Consent consent = new Consent();
    when(consentDAO.findConsentById(anyString())).thenReturn(consent);
    initService();

    assertThrows(IllegalArgumentException.class, () -> {
      datasetService.createConsentForDataset(dataSetDTO);
    });
  }

  @Test
  public void testAutoCompleteDatasets() {
    List<DatasetDTO> dtos = getDatasetDTOs();
    Set<DatasetDTO> setOfDtos = new HashSet<>(dtos);
    when(datasetDAO.findAllDatasetDTOs()).thenReturn(setOfDtos);
    when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 0)).thenReturn(
        UserRoles.ADMIN.getRoleId());
    initService();
    List<Map<String, String>> result = datasetService.autoCompleteDatasets("a", 0);
    assertNotNull(result);
    assertEquals(result.size(), dtos.size());
  }

  @Test
  public void testSearchDatasetsOpenAccessFalse() {
    Dataset ds1 = new Dataset();
    ds1.setName("asdf1234");
    ds1.setAlias(3);
    DatasetProperty ds1PI = new DatasetProperty();
    ds1PI.setPropertyName("Principal Investigator(PI)");
    ds1PI.setPropertyValue("John Doe");
    ds1PI.setPropertyType(PropertyType.String);
    ds1.setProperties(Set.of(ds1PI));

    Dataset ds2 = new Dataset();
    ds2.setName("ghjk5678");
    ds2.setAlias(280);
    DatasetProperty ds2PI = new DatasetProperty();
    ds2PI.setPropertyName("Principal Investigator(PI)");
    ds2PI.setPropertyValue("Sally Doe");
    ds2PI.setPropertyType(PropertyType.String);
    ds2.setProperties(Set.of(ds2PI));

    User u = new User();
    u.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));

    when(datasetDAO.findAllDatasets()).thenReturn(List.of(ds1, ds2));

    initService();

    // query dataset name
    List<Dataset> results = datasetService.searchDatasets("asdf", false, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds1));

    // query pi name
    results = datasetService.searchDatasets("John", false, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds1));

    // query ds identifier
    results = datasetService.searchDatasets("DUOS-000280", false, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds2));

    // query pi name for all of them
    results = datasetService.searchDatasets("Doe", false, u);

    assertEquals(2, results.size());
    assertTrue(results.contains(ds2));
    assertTrue(results.contains(ds1));

    // search on two things at once
    results = datasetService.searchDatasets("Doe asdf", false, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds1));

    // query nonexistent phrase
    results = datasetService.searchDatasets("asdflasdfasdfasdfhalskdjf", false, u);
    assertEquals(0, results.size());
  }

  @Test
  public void testSearchDatasetsOpenAccessTrue() {
    Dataset ds1 = new Dataset();
    ds1.setName("string");
    ds1.setAlias(622);
    DatasetProperty ds1PI = new DatasetProperty();
    ds1PI.setPropertyName("Open Access");
    ds1PI.setPropertyValue("true");
    ds1PI.setPropertyType(PropertyType.String);
    ds1.setProperties(Set.of(ds1PI));

    Dataset ds2 = new Dataset();
    ds2.setName("TESTING NAME");
    ds2.setAlias(623);
    DatasetProperty ds2PI = new DatasetProperty();
    ds2PI.setPropertyName("Open Access");
    ds2PI.setPropertyValue("true");
    ds2PI.setPropertyType(PropertyType.String);
    ds2.setProperties(Set.of(ds2PI));

    User u = new User();
    u.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));

    when(datasetDAO.findAllDatasets()).thenReturn(List.of(ds1, ds2));

    initService();

    // query dataset name
    List<Dataset> results = datasetService.searchDatasets("string", true, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds1));

    // query ds identifier
    results = datasetService.searchDatasets("DUOS-000623", true, u);

    assertEquals(1, results.size());
    assertTrue(results.contains(ds2));

    // query nonexistent phrase
    results = datasetService.searchDatasets("asdflasdfasdfasdfhalskdjf", true, u);
    assertEquals(0, results.size());
  }

  @Test
  public void testDescribeDatasets() {
    List<DatasetDTO> dtos = getDatasetDTOs();
    Set<DatasetDTO> setOfDtos = new HashSet<>(dtos);
    Set<DatasetDTO> singleDtoSet = Collections.singleton(dtos.get(0));
    Set<DatasetDTO> emptyActiveDtoSet = Collections.emptySet();
    when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 0)).thenReturn(null);
    when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 1)).thenReturn(1);
    when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 2)).thenReturn(null);
    when(userRoleDAO.findRoleByNameAndUser(UserRoles.CHAIRPERSON.getRoleName(), 0)).thenReturn(
        null);
    when(userRoleDAO.findRoleByNameAndUser(UserRoles.CHAIRPERSON.getRoleName(), 2)).thenReturn(2);
    when(datasetDAO.findAllDatasetDTOs()).thenReturn(setOfDtos);
    when(datasetDAO.findActiveDatasetDTOs()).thenReturn(emptyActiveDtoSet);
    when(datasetDAO.findDatasetDTOsByUserId(2)).thenReturn(singleDtoSet);
    initService();

    Set<DatasetDTO> memberResult = datasetService.describeDatasets(0);
    assertNotNull(memberResult);
    assertEquals(memberResult.size(), 0);
    Set<DatasetDTO> adminResult = datasetService.describeDatasets(1);
    assertNotNull(adminResult);
    assertEquals(adminResult.size(), dtos.size());
    Set<DatasetDTO> chairResult = datasetService.describeDatasets(2);
    assertNotNull(chairResult);
    assertEquals(chairResult.size(), singleDtoSet.size());
  }

  @Test
  public void testFindAllDatasetsByUser_Admin() {
    User user = new User();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.addRole(admin);
    Dataset dataset = new Dataset();
    dataset.setDataSetId(1);
    when(datasetDAO.findAllDatasets()).thenReturn(List.of(dataset));
    initService();

    List<Dataset> datasets = datasetService.findAllDatasetsByUser(user);
    assertFalse(datasets.isEmpty());
    assertEquals(1, datasets.size());
    assertEquals(dataset.getDataSetId(), datasets.get(0).getDataSetId());
    verify(datasetDAO, times(1)).findAllDatasets();
    verify(datasetDAO, times(0)).getActiveDatasets();
    verify(datasetDAO, times(0)).findDatasetsByAuthUserEmail(any());
  }

  @Test
  public void testFindAllDatasetsByUser_Chair() {
    User user = new User();
    UserRole chair = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    user.addRole(chair);
    Dataset d1 = new Dataset();
    d1.setDataSetId(1);
    Dataset d2 = new Dataset();
    d2.setDataSetId(2);
    Dataset d3 = new Dataset();
    d2.setDataSetId(3);
    when(datasetDAO.getActiveDatasets()).thenReturn(List.of(d1, d2));
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
    verify(datasetDAO, times(1)).getActiveDatasets();
    verify(datasetDAO, times(1)).findDatasetsByAuthUserEmail(any());
  }

  @Test
  public void testFindAllDatasetsByUser() {
    User user = new User();
    UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(),
        UserRoles.RESEARCHER.getRoleName());
    user.addRole(researcher);
    Dataset d1 = new Dataset();
    d1.setDataSetId(1);
    Dataset d2 = new Dataset();
    d2.setDataSetId(2);
    when(datasetDAO.getActiveDatasets()).thenReturn(List.of(d1, d2));
    initService();

    List<Dataset> datasets = datasetService.findAllDatasetsByUser(user);
    assertFalse(datasets.isEmpty());
    assertEquals(2, datasets.size());
    assertTrue(datasets.contains(d1));
    assertTrue(datasets.contains(d2));
    verify(datasetDAO, times(0)).findAllDatasets();
    verify(datasetDAO, times(1)).getActiveDatasets();
    verify(datasetDAO, times(0)).findDatasetsByAuthUserEmail(any());
  }

  @Test
  public void testApproveDataset_AlreadyApproved_TrueSubmission() throws Exception {
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
    when(dacDAO.findById(3)).thenReturn(dac);

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
  public void testApprovedDataset_AlreadyApproved_FalseSubmission() {
    Dataset dataset = new Dataset();
    User user = new User();
    dataset.setDacApproval(true);
    initService();

    assertThrows(IllegalArgumentException.class, () -> {
      datasetService.approveDataset(dataset, user, false);
    });
  }

  @Test
  public void testApprovedDataset_AlreadyApproved_NullSubmission() {
    Dataset dataset = new Dataset();
    User user = new User();
    dataset.setDacApproval(true);
    initService();

    assertThrows(IllegalArgumentException.class, () -> {
      datasetService.approveDataset(dataset, user, null);
    });
  }

  @Test
  public void testApproveDataset() throws Exception {
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
  public void testApproveDataset_DenyDataset() throws Exception {
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

    // send denied email
    verify(emailService, times(1)).sendDatasetDeniedMessage(
        user,
        "DAC NAME",
        "DUOS-000001"
    );
  }

  @Test
  public void testSyncDataUseTranslation() {
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
  public void testSyncDataUseTranslationNotFound() {
    when(datasetDAO.findDatasetById(1)).thenReturn(null);
    initService();
    assertThrows(NotFoundException.class, () -> {
      datasetService.syncDatasetDataUseTranslation(1);
    });

  }

  /* Helper functions */

  private List<Dataset> getDatasets() {
    return IntStream.range(1, 3)
        .mapToObj(i -> {
          Dataset dataset = new Dataset();
          dataset.setDataSetId(i);
          dataset.setName("Test Dataset " + i);
          dataset.setConsentName("Test Consent " + i);
          dataset.setActive(true);
          dataset.setNeedsApproval(false);
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
          dataset.setActive(true);
          dataset.setNeedsApproval(false);
          dataset.setProperties(Collections.singletonList(nameProperty));
          return dataset;
        }).collect(Collectors.toList());
  }

  private Set<DatasetProperty> getDatasetProperties() {
    return IntStream.range(1, 11)
        .mapToObj(i ->
            new DatasetProperty(1,
                i,
                "Test Value" + RandomStringUtils.randomAlphanumeric(25),
                PropertyType.String,
                new Date())
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
    datasetDTO.setActive(true);
    datasetDTO.setNeedsApproval(false);
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
