package org.broadinstitute.consent.http.service;

import java.util.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DataSetDAO;

import javax.inject.Inject;

import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;

public class DatasetService {

    private final DataSetDAO dataSetDAO;

    @Inject
    public DatasetService(DataSetDAO dataSetDAO) {
        this.dataSetDAO = dataSetDAO;
    }

    public DataSet createDataset(DataSetDTO dataset, String name, Integer userId) {
        Timestamp now = new Timestamp(new Date().getTime());
        int lastAlias = dataSetDAO.findLastAlias();
        int alias = lastAlias + 1;

        Integer id = dataSetDAO
            .insertDatasetV2(name, now, userId, dataset.getObjectId(), dataset.getActive(), alias);

        List<DataSetProperty> propertyList = processDatasetProperties(id, dataset.getProperties());
        dataSetDAO.insertDataSetsProperties(propertyList);

        return getDatasetWithPropertiesById(id);
    }

    public DataSet getDatasetByName(String name) {
        return dataSetDAO.getDatasetByName(name);
    }

    public DataSet findDatasetById(Integer id) {
        return dataSetDAO.findDataSetById(id);
    }

    public Set<DataSetProperty> getDatasetProperties(Integer datasetId) {
        return dataSetDAO.findDatasetPropertiesByDatasetId(datasetId);
    }

    public DataSet getDatasetWithPropertiesById(Integer datasetId) {
        DataSet dataset = dataSetDAO.findDataSetById(datasetId);
        Set<DataSetProperty> properties = getDatasetProperties(datasetId);
        dataset.setProperties(properties);
        return dataset;
    }

    public DataSet updateDataset(DataSetDTO dataset, Integer datasetId, Integer userId) {
        Timestamp now = new Timestamp(new Date().getTime());

        DataSet old = getDatasetWithPropertiesById(datasetId);
        Set<DataSetProperty> oldProperties = old.getProperties();

        List<DataSetPropertyDTO> dtos = dataset.getProperties();
        List<DataSetProperty> jsonProperties = processDatasetProperties(datasetId, dtos);
              ;
        List<DataSetProperty> propertiesToAdd = jsonProperties.stream()
              .filter(p -> oldProperties.stream()
                    .noneMatch(op -> op.getPropertyKey() == p.getPropertyKey()))
              .collect(Collectors.toList());

        List<DataSetProperty> propertiesToUpdate = jsonProperties.stream()
              .filter(p -> oldProperties.stream()
                    .anyMatch(op -> equalsProperty(op.getPropertyKey(), op.getPropertyValue(), p.getPropertyKey(), p.getPropertyValue())))
              .collect(Collectors.toList());

        if (propertiesToAdd.isEmpty() || propertiesToUpdate.isEmpty()) {
            return null;
        }

        propertiesToUpdate.stream().peek(p -> dataSetDAO.updateDatasetProperty(datasetId, p.getPropertyKey(), p.getPropertyValue()));
        dataSetDAO.insertDataSetsProperties(propertiesToAdd);
        dataSetDAO.updateDataset(datasetId, now, userId);
        DataSet updatedDataset = getDatasetWithPropertiesById(datasetId);
        return updatedDataset;
    }

    public DataSetDTO getDatasetDTO(Integer datasetId) {
        Set<DataSetDTO> dataset = dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(datasetId);
        DataSetDTO result = new DataSetDTO();
        for (DataSetDTO d : dataset) {
            result = d;
        }
        return result;
    }

    public List<DataSetProperty> processDatasetProperties(Integer datasetId, List<DataSetPropertyDTO> properties) {
        Date now = new Date();
        List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(Dictionary::getKey).collect(Collectors.toList());

        return properties.stream()
            .filter(p -> keys.contains(p.getPropertyName()) && !p.getPropertyName().equals("Dataset Name"))
            .map(p ->
                new DataSetProperty(datasetId, dictionaries.get(keys.indexOf(p.getPropertyName())).getKeyId(), p.getPropertyValue(), now)
            )
            .collect(Collectors.toList());
    }

    public List<DataSetPropertyDTO> findInvalidProperties(List<DataSetPropertyDTO> properties) {
        List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(Dictionary::getKey).collect(Collectors.toList());

        return properties.stream()
            .filter(p -> !keys.contains(p.getPropertyName()))
            .collect(Collectors.toList());
    }

    public boolean equalsProperty(Integer propOneKey,  String propOneValue, Integer propTwoKey, String propTwoValue) {
        return (propOneKey == propTwoKey && propOneValue.equalsIgnoreCase(propTwoValue));
    }
}
