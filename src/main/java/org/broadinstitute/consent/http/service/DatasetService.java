package org.broadinstitute.consent.http.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DataSetDAO;

import javax.inject.Inject;
import java.util.Date;
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

    public DataSet createTestDataSet(String json) {
        return new DataSet(json);
    }

    public DataSet createDataset(DataSetDTO dataset, String name) {
        Date now = new Date();
        int lastAlias = dataSetDAO.findLastAlias();
        int alias = lastAlias + 1;

        Integer id = dataSetDAO
            .insertDataset(name, now, dataset.getObjectId(), dataset.getActive(), alias);

        List<DataSetProperty> propertyList = processDatasetProperties(id, now, dataset.getProperties());
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
        Set<DataSetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(datasetId);
        return properties;
    }

    public DataSet getDatasetWithPropertiesById(Integer datasetId) {
        DataSet dataset = dataSetDAO.findDataSetById(datasetId);
        Set<DataSetProperty> properties = getDatasetProperties(datasetId);
        dataset.setProperties(properties);
        return dataset;
    }

    // convert dsp DTO -> dsp by associating with extra fields from dictionary def
    public List<DataSetProperty> processDatasetProperties(Integer datasetId, Date createDate, List<DataSetPropertyDTO> properties) {
        List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(d -> d.getKey()).collect(Collectors.toList());

        return properties.stream()
            .filter(p -> keys.contains(p.getPropertyName()) && !p.getPropertyName().equals("Dataset Name"))
            .map(p ->
                new DataSetProperty(datasetId, null, p.getPropertyValue(), createDate)
            )
            .collect(Collectors.toList());
    }

}
