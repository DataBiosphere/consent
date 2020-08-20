package org.broadinstitute.consent.http.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.DataSetDAO;

import javax.inject.Inject;
import java.util.Date;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;

public class DatasetService {

    private final DataSetDAO dataSetDAO;

    @Inject
    public DatasetService(DataSetDAO dataSetDAO) {
        this.dataSetDAO = dataSetDAO;
    }

    public DataSetDTO createDataset(DataSetDTO dataset, String name) {
        Date now = new Date();
        int lastAlias = dataSetDAO.findLastAlias();
        int alias = lastAlias + 1;

        Integer id = dataSetDAO.insertDataset(name, now, dataset.getObjectId(), dataset.getActive(), alias);

        List<DataSetProperty> propertyList = processDatasetProperties(id, now, dataset.getProperties());
        dataSetDAO.insertDataSetsProperties(propertyList);

        DataSetDTO result = new DataSetDTO();
        Set<DataSetDTO> set = dataSetDAO.findDatasetDTOWithPropsByDatasetId(id);

        for (DataSetDTO ds : set) {
            result = ds;
        }
        return result;
    }

    // return -1 if no ds found
    public Integer findDatasetByName(String name) {
        Integer result = dataSetDAO.getDataSetByName(name);
        if (result == null) {
            return -1;
        }
        return result;
    }

    // assumes that you will receive non-null values for all properties in receiveOrder
    public List<DataSetProperty> processDatasetProperties(Integer datasetId, Date now, List<DataSetPropertyDTO> properties) {
        List<DataSetProperty> result = new ArrayList<>(10);
        // start at 1 to skip manually creating dataset name property
        for (int i = 1; i < properties.size(); i++) {
            DataSetProperty dsp = new DataSetProperty();
            dsp.setCreateDate(now);
            dsp.setDataSetId(datasetId);
            dsp.setPropertyKey(i+1);
            dsp.setPropertyValue(properties.get(i).getPropertyValue());
            result.add(dsp);
        }
        return result;
    }

}
