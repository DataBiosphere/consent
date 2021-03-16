package org.broadinstitute.consent.http.service;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
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

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetAuditDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatasetServiceTest {

    private DatasetService datasetService;

    @Mock
    private ConsentDAO consentDAO;

    @Mock
    private DataSetDAO datasetDAO;

    @Mock
    private UserRoleDAO userRoleDAO;

    @Mock
    private DataSetAuditDAO dataSetAuditDAO;

    @Mock
    private DatasetAssociationDAO datasetAssociationDAO;

    @Mock
    private UseRestrictionConverter useRestrictionConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        datasetService = new DatasetService(consentDAO, datasetDAO, userRoleDAO, dataSetAuditDAO, datasetAssociationDAO, useRestrictionConverter);
    }

    @Test
    public void testCreateDataset() {
        int datasetId = 1;
        DataSetDTO test = getDatasetDTO();
        when(datasetDAO.insertDatasetV2(anyString(), any(), anyInt(), anyString(), anyBoolean(), anyInt()))
            .thenReturn(datasetId);
        when(datasetDAO.findDataSetById(datasetId)).thenReturn(getDatasets().get(0));
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(datasetId)).thenReturn(Collections.singleton(test));
        initService();

        DataSetDTO result = datasetService.createDataset(getDatasetDTO(), "Test Dataset 1", 1);

        assertNotNull(result);
        assertEquals(result.getDataSetId(), getDatasets().get(0).getDataSetId());
        assertNotNull(result.getProperties());
        assertFalse(result.getProperties().isEmpty());
    }

    @Test
    public void testGetDatasetsForConsent() {
        when(datasetDAO.getDataSetsForConsent(getDatasets().get(0).getConsentName()))
                .thenReturn(getDatasets());
        initService();

        List<DataSet> setsForConsent = datasetService.getDataSetsForConsent("Test Consent 1");
        assertNotNull(setsForConsent);
        assertEquals(setsForConsent.size(), getDatasets().size());
        assertEquals(setsForConsent.get(0).getDataSetId(), getDatasets().get(0).getDataSetId());
    }

    @Test
    public void testDescribeDataSetsByReceiveOrder() {
        when(datasetDAO.findDataSetsByReceiveOrder(Arrays.asList(1)))
            .thenReturn(getDatasetDTOs().stream().collect(Collectors.toSet()));
        initService();

        Collection<DataSetDTO> dataSetsByReceiveOrder = datasetService.describeDataSetsByReceiveOrder(Arrays.asList(1));
        assertNotNull(dataSetsByReceiveOrder);
        assertEquals(dataSetsByReceiveOrder.size(), getDatasetDTOs().size());
    }

    @Test
    public void testDescribeDictionaryByDisplayOrder() {
        when(datasetDAO.getMappedFieldsOrderByDisplayOrder())
                .thenReturn(getDictionaries().stream().collect(Collectors.toList()));
        initService();

        Collection<Dictionary> dictionaries = datasetService.describeDictionaryByDisplayOrder();
        assertNotNull(dictionaries);
        assertEquals(dictionaries.stream().findFirst().orElseThrow().getDisplayOrder(), getDictionaries().stream().findFirst().orElseThrow().getDisplayOrder());
    }

    @Test
    public void testDescribeDictionaryByReceiveOrder() {
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder())
                .thenReturn(getDictionaries().stream().collect(Collectors.toList()));
        initService();

        Collection<Dictionary> dictionaries = datasetService.describeDictionaryByReceiveOrder();
        assertNotNull(dictionaries);
        assertEquals(dictionaries.stream().findFirst().orElseThrow().getReceiveOrder(), getDictionaries().stream().findFirst().orElseThrow().getReceiveOrder());
    }

    @Test
    public void testDisableDataset() {
        Integer dataSetId = 1;
        when(datasetDAO.findDataSetById(dataSetId))
                .thenReturn(getDatasets().get(0));
        doNothing().when(datasetDAO).updateDataSetActive(any(), any());

        initService();

        datasetService.disableDataset(dataSetId, false);
    }

    @Test
    public void testUpdateNeedsReviewDataSets(){
        Integer dataSetId = 1;
        when(datasetDAO.findDataSetById(dataSetId))
                .thenReturn(getDatasets().get(0));
        doNothing().when(datasetDAO).updateDataSetNeedsApproval(any(), any());
        initService();

        DataSet dataSet = datasetService.updateNeedsReviewDataSets(dataSetId, true);
        assertNotNull(dataSet);
    }

    @Test
    public void testFindNeedsApprovalDataSetsByObjectId() {
        when(datasetDAO.findNeedsApprovalDataSetByDataSetId(Arrays.asList(1)))
                .thenReturn(getDatasets());
        initService();

        List<DataSet> dataSets = datasetService.findNeedsApprovalDataSetByObjectId(Arrays.asList(1));
        assertNotNull(dataSets);
        assertEquals(dataSets.stream().findFirst().orElseThrow().getDataSetId(), getDatasets().stream().findFirst().orElseThrow().getDataSetId());
    }

    @Test
    public void testDeleteDataset() {
        Integer dataSetId = 1;
        when(datasetDAO.findDataSetById(any()))
                .thenReturn(getDatasets().get(0));
        when(dataSetAuditDAO.insertDataSetAudit(any()))
                .thenReturn(1);
        doNothing().when(datasetAssociationDAO).delete(any());
        doNothing().when(datasetDAO).deleteDataSetsProperties(any());
        doNothing().when(datasetDAO).logicalDatasetDelete(any());
        doNothing().when(consentDAO).deleteAssociationsByDataSetId(any());
        doNothing().when(datasetDAO).deleteDataSets(any());

        initService();
        datasetService.deleteDataset(dataSetId, 1);
    }

    @Test
    public void testGetDatasetByName() {
        when(datasetDAO.getDatasetByName(getDatasets().get(0).getName().toLowerCase()))
            .thenReturn(getDatasets().get(0));
        initService();

        DataSet dataset = datasetService.getDatasetByName("Test Dataset 1");

        assertNotNull(dataset);
        assertEquals(dataset.getDataSetId(), getDatasets().get(0).getDataSetId());
    }

    @Test
    public void testFindDatasetById() {
        when(datasetDAO.findDataSetById(getDatasets().get(0).getDataSetId()))
            .thenReturn(getDatasets().get(0));
        initService();

        DataSet dataset = datasetService.findDatasetById(1);

        assertNotNull(dataset);
        assertEquals(dataset.getName(), getDatasets().get(0).getName());
    }

    @Test
    public void testGetDatasetProperties() {
        when(datasetDAO.findDatasetPropertiesByDatasetId(anyInt())).thenReturn(Collections.emptySet());
        initService();

        assertTrue(datasetService.getDatasetProperties(1).isEmpty());
    }

    @Test
    public void testGetDatasetWithPropertiesById() {
        int datasetId = 1;
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.findDataSetById(datasetId)).thenReturn(getDatasets().get(0));
        initService();

        assertEquals(datasetService.getDatasetProperties(datasetId), datasetDAO.findDatasetPropertiesByDatasetId(1));
    }

    @Test
    public void testProcessDatasetProperties() {
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        List<DataSetProperty> properties = datasetService
            .processDatasetProperties(1, getDatasetPropertiesDTO());

        assertEquals(properties.size(), getDatasetPropertiesDTO().size());
    }

    @Test
    public void testFindInvalidProperties() {
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        List<DataSetPropertyDTO> input = getDatasetPropertiesDTO().stream()
            .peek(p -> p.setPropertyKey("Invalid Key"))
            .collect(Collectors.toList());

        List<DataSetPropertyDTO> properties = datasetService.findInvalidProperties(input);

        assertFalse(properties.isEmpty());
    }

    @Test
    public void testFindDuplicateProperties() {
        initService();

        List<DataSetPropertyDTO> input = getDatasetPropertiesDTO();
        DataSetPropertyDTO duplicateProperty = input.get(0);
        input.add(duplicateProperty);

        List<DataSetPropertyDTO> properties = datasetService.findDuplicateProperties(input);

        assertFalse(properties.isEmpty());
        assertEquals(properties.get(0), duplicateProperty);
    }

    @Test
    public void testGetDatasetDTO() {
        Set<DataSetDTO> set = new HashSet<>();
        set.add(getDatasetDTO());
        when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(anyInt())).thenReturn(set);
        initService();

        DataSetDTO datasetDTO = datasetService.getDatasetDTO(1);

        assertNotNull(datasetDTO);
        assertFalse(datasetDTO.getProperties().isEmpty());
    }

    @Test
    public void testUpdateDatasetNotModified() {
        int datasetId = 1;
        DataSetDTO dataSetDTO = getDatasetDTO();
        DataSet dataset = getDatasets().get(0);
        dataset.setProperties(getDatasetProperties());
        when(datasetDAO.findDataSetById(datasetId)).thenReturn(dataset);
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        Optional<DataSet> notModified = datasetService.updateDataset(dataSetDTO, datasetId, 1);
        assertEquals(Optional.empty(), notModified);
    }

    @Test
    public void testUpdateDatasetMultiFieldUpdateOnly() {
        int datasetId = 1;
        DataSetDTO dataSetDTO = getDatasetDTO();
        DataSet dataset = getDatasets().get(0);
        dataset.setProperties(getDatasetProperties());

        List<DataSetPropertyDTO> updatedProperties = getDatasetPropertiesDTO();
        updatedProperties.get(2).setPropertyValue("updated value");
        updatedProperties.get(3).setPropertyValue("updated value");
        dataSetDTO.setProperties(updatedProperties);

        when(datasetDAO.findDataSetById(datasetId)).thenReturn(dataset);
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        DataSet updated = datasetService.updateDataset(dataSetDTO, datasetId, 1).get();
        assertNotNull(updated);
    }

    @Test
    public void testUpdateDatasetMultiFieldAddOnly() {
        int datasetId = 1;
        DataSetDTO dataSetDTO = getDatasetDTO();
        DataSet dataset = getDatasets().get(0);
        List<DataSetProperty> properties = getDatasetProperties().stream().collect(Collectors.toList());
        properties.remove(2);
        properties.remove(2);
        dataset.setProperties(properties.stream().collect(Collectors.toSet()));

        List<DataSetPropertyDTO> updatedProperties = getDatasetPropertiesDTO();
        updatedProperties.get(2).setPropertyValue("added value");
        updatedProperties.get(3).setPropertyValue("added value");
        dataSetDTO.setProperties(updatedProperties);

        when(datasetDAO.findDataSetById(datasetId)).thenReturn(dataset);
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        DataSet updated = datasetService.updateDataset(dataSetDTO, datasetId, 1).get();
        assertNotNull(updated);
    }

    @Test
    public void testUpdateDatasetMultiFieldDeleteOnly() {
        int datasetId = 1;
        DataSetDTO dataSetDTO = getDatasetDTO();
        DataSet dataset = getDatasets().get(0);
        dataset.setProperties(getDatasetProperties());

        List<DataSetPropertyDTO> updatedProperties = getDatasetPropertiesDTO();
        updatedProperties.remove(2);
        updatedProperties.remove(2);
        dataSetDTO.setProperties(updatedProperties);

        when(datasetDAO.findDataSetById(datasetId)).thenReturn(dataset);
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        DataSet updated = datasetService.updateDataset(dataSetDTO, datasetId, 1).get();
        assertNotNull(updated);
    }

    @Test
    public void testCreateConsentForDataset() throws IOException {
        DataSetDTO dataSetDTO = getDatasetDTO();
        DataUse dataUse = new DataUseBuilder().build();
        dataSetDTO.setDataUse(dataUse);
        UseRestriction useRestriction = UseRestriction.parse("{\"type\":\"everything\"}");
        Consent consent = new Consent();
        when(consentDAO.findConsentById(anyString())).thenReturn(consent);
        when(useRestrictionConverter.parseUseRestriction(any())).thenReturn(useRestriction);
        doNothing().when(consentDAO).insertConsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        doNothing().when(consentDAO).insertConsentAssociation(any(), any(), any());
        initService();

        Consent result = datasetService.createConsentForDataset(dataSetDTO);
        assertNotNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateConsentForDatasetNullDataUse() {
        DataSetDTO dataSetDTO = getDatasetDTO();
        Consent consent = new Consent();
        when(consentDAO.findConsentById(anyString())).thenReturn(consent);
        initService();

        datasetService.createConsentForDataset(dataSetDTO);
    }

    @Test
    public void testAutoCompleteDatasets() {
        List<DataSetDTO> dtos = getDatasetDTOs();
        Set<DataSetDTO> setOfDtos = dtos.stream().collect(Collectors.toSet());
        when(datasetDAO.findAllDatasets()).thenReturn(setOfDtos);
        when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 0)).thenReturn(UserRoles.ADMIN.getRoleId());
        initService();

        List<Map<String, String>> result = datasetService.autoCompleteDatasets("a", 0);
        assertNotNull(result);
        assertEquals(result.size(), dtos.size());
    }

    @Test
    public void testGetAllActiveDatasets() {
        List<DataSetDTO> dtos = getDatasetDTOs();
        Set<DataSetDTO> setOfDtos = dtos.stream().collect(Collectors.toSet());
        when(datasetDAO.findActiveDatasets()).thenReturn(setOfDtos);
        initService();

        Set<DataSetDTO> result = datasetService.getAllActiveDatasets();
        assertNotNull(result);
        assertEquals(result.size(), dtos.size());
    }

    @Test
    public void testDescribeDatasets() {
        List<DataSetDTO> dtos = getDatasetDTOs();
        Set<DataSetDTO> setOfDtos = dtos.stream().collect(Collectors.toSet());
        Set<DataSetDTO> singleDtoSet = Collections.singleton(dtos.get(0));
        Set<DataSetDTO> emptyActiveDtoSet = Collections.emptySet();
        when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 0)).thenReturn(null);
        when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 1)).thenReturn(1);
        when(userRoleDAO.findRoleByNameAndUser(UserRoles.ADMIN.getRoleName(), 2)).thenReturn(null);
        when(userRoleDAO.findRoleByNameAndUser(UserRoles.CHAIRPERSON.getRoleName(), 0)).thenReturn(null);
        when(userRoleDAO.findRoleByNameAndUser(UserRoles.CHAIRPERSON.getRoleName(), 2)).thenReturn(2);
        when(datasetDAO.findAllDatasets()).thenReturn(setOfDtos);
        when(datasetDAO.findActiveDatasets()).thenReturn(emptyActiveDtoSet);
        when(datasetDAO.findDatasetsByUser(2)).thenReturn(singleDtoSet);
        initService();

        Set<DataSetDTO> memberResult = datasetService.describeDatasets(0);
        assertNotNull(memberResult);
        assertEquals(memberResult.size(), emptyActiveDtoSet.size());
        Set<DataSetDTO> adminResult = datasetService.describeDatasets(1);
        assertNotNull(adminResult);
        assertEquals(adminResult.size(), dtos.size());
        Set<DataSetDTO> chairResult = datasetService.describeDatasets(2);
        assertNotNull(chairResult);
        assertEquals(chairResult.size(), singleDtoSet.size());
    }

    /* Helper functions */

    private List<DataSet> getDatasets() {
        return IntStream.range(1, 3)
            .mapToObj(i -> {
                DataSet dataset = new DataSet();
                dataset.setDataSetId(i);
                dataset.setName("Test Dataset " + i);
                dataset.setConsentName("Test Consent " + i);
                dataset.setActive(true);
                dataset.setNeedsApproval(false);
                dataset.setProperties(Collections.emptySet());
                return dataset;
            }).collect(Collectors.toList());
    }

    private List<DataSetDTO> getDatasetDTOs() {
        return IntStream.range(1, 3)
              .mapToObj(i -> {
                  DataSetDTO dataset = new DataSetDTO();
                  dataset.setDataSetId(i);
                  DataSetPropertyDTO nameProperty = new DataSetPropertyDTO("Dataset Name", "Test Dataset " + i);
                  dataset.setActive(true);
                  dataset.setNeedsApproval(false);
                  dataset.setProperties(Collections.singletonList(nameProperty));
                  return dataset;
              }).collect(Collectors.toList());
    }

    private Set<DataSetProperty> getDatasetProperties() {
        return IntStream.range(1, 11)
            .mapToObj(i ->
                new DataSetProperty(1, i, "Test Value", new Date())
            ).collect(Collectors.toSet());
    }

    private List<DataSetPropertyDTO> getDatasetPropertiesDTO() {
        List<Dictionary> dictionaries = getDictionaries();
        return dictionaries.stream()
            .map(d ->
                new DataSetPropertyDTO(d.getKey(), "Test Value")
            ).collect(Collectors.toList());
    }

    private DataSetDTO getDatasetDTO() {
        DataSetDTO datasetDTO = new DataSetDTO();
        datasetDTO.setDataSetId(1);
        datasetDTO.setObjectId("Test ObjectId");
        datasetDTO.setActive(true);
        datasetDTO.setProperties(getDatasetPropertiesDTO());
        return datasetDTO;
    }

    private List<Dictionary> getDictionaries() {
        return IntStream.range(1, 11)
            .mapToObj(i ->
                new Dictionary(i, String.valueOf(i), true, i, i)
            ).collect(Collectors.toList());
    }

}
