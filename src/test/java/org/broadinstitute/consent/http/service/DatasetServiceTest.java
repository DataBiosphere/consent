package org.broadinstitute.consent.http.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatasetServiceTest {

    private DatasetService datasetService;

    @Mock
    private DataSetDAO datasetDAO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        datasetService = new DatasetService(datasetDAO);
    }

    @Test
    public void testCreateDataset() {
        int datasetId = 1;
        when(datasetDAO.insertDatasetV2(anyString(), any(), anyInt(), anyString(), anyBoolean(), anyInt()))
            .thenReturn(datasetId);
        when(datasetDAO.findDataSetById(datasetId)).thenReturn(getDatasets().get(0));
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        initService();

        DataSet result = datasetService.createDataset(getDatasetDTO(), "Test Dataset 1", 1);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.getName(), getDatasets().get(0).getName());
        Assert.assertNotNull(result.getProperties());
        Assert.assertFalse(result.getProperties().isEmpty());
    }

    @Test
    public void testGetDatasetByName() {
        when(datasetDAO.getDatasetByName(getDatasets().get(0).getName()))
            .thenReturn(getDatasets().get(0));
        initService();

        DataSet dataset = datasetService.getDatasetByName("Test Dataset 1");

        Assert.assertNotNull(dataset);
        Assert.assertEquals(dataset.getDataSetId(), getDatasets().get(0).getDataSetId());
    }

    @Test
    public void testFindDatasetById() {
        when(datasetDAO.findDataSetById(getDatasets().get(0).getDataSetId()))
            .thenReturn(getDatasets().get(0));
        initService();

        DataSet dataset = datasetService.findDatasetById(1);

        Assert.assertNotNull(dataset);
        Assert.assertEquals(dataset.getName(), getDatasets().get(0).getName());
    }

    @Test
    public void testGetDatasetProperties() {
        when(datasetDAO.findDatasetPropertiesByDatasetId(anyInt())).thenReturn(Collections.emptySet());
        initService();

        Assert.assertTrue(datasetService.getDatasetProperties(1).isEmpty());
    }

    @Test
    public void testGetDatasetWithPropertiesById() {
        int datasetId = 1;
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.findDataSetById(datasetId)).thenReturn(getDatasets().get(0));
        initService();

        Assert.assertEquals(datasetService.getDatasetProperties(datasetId), datasetDAO.findDatasetPropertiesByDatasetId(1));
    }

    @Test
    public void testProcessDatasetProperties() {
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        List<DataSetProperty> properties = datasetService
            .processDatasetProperties(1, getDatasetPropertiesDTO());

        Assert.assertEquals(properties.size(), getDatasetPropertiesDTO().size());
    }

    @Test
    public void testFindInvalidProperties() {
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        List<DataSetPropertyDTO> input = getDatasetPropertiesDTO().stream()
            .peek(p -> p.setPropertyKey("Invalid Key"))
            .collect(Collectors.toList());

        List<DataSetPropertyDTO> properties = datasetService.findInvalidProperties(input);

        Assert.assertFalse(properties.isEmpty());
    }

    @Test
    public void testGetDatasetDTO() {
        Set<DataSetDTO> set = new HashSet<>();
        set.add(getDatasetDTO());
        when(datasetDAO.findDatasetDTOWithPropertiesByDatasetId(anyInt())).thenReturn(set);
        initService();

        DataSetDTO datasetDTO = datasetService.getDatasetDTO(1);

        Assert.assertNotNull(datasetDTO);
        Assert.assertFalse(datasetDTO.getProperties().isEmpty());
    }

    @Test
    public void testUpdateDataset() {
        int datasetId = 1;
        DataSetDTO dataSetDTO = getDatasetDTO();
        DataSet dataset = getDatasets().get(0);
        dataset.setProperties(getDatasetProperties());
        when(datasetDAO.findDataSetById(datasetId)).thenReturn(dataset);
        when(datasetDAO.findDatasetPropertiesByDatasetId(datasetId)).thenReturn(getDatasetProperties());
        when(datasetDAO.getMappedFieldsOrderByReceiveOrder()).thenReturn(getDictionaries());
        initService();

        Optional<DataSet> notModified = datasetService.updateDataset(dataSetDTO, datasetId, 1);
        Assert.assertEquals(Optional.empty(), notModified);

        List<DataSetPropertyDTO> updatedProperties = getDatasetPropertiesDTO();
        updatedProperties.get(3).setPropertyValue("updated value");
        dataSetDTO.setProperties(updatedProperties);

        DataSet updated = datasetService.updateDataset(dataSetDTO, datasetId, 1).get();
        Assert.assertNotNull(updated);
    }

    /* Helper functions */

    private List<DataSet> getDatasets() {
        return IntStream.range(1, 3)
            .mapToObj(i -> {
                DataSet dataset = new DataSet();
                dataset.setDataSetId(i);
                dataset.setName("Test Dataset " + i);
                dataset.setActive(true);
                dataset.setNeedsApproval(false);
                dataset.setProperties(Collections.emptySet());
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
