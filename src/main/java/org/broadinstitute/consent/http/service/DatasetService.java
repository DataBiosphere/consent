package org.broadinstitute.consent.http.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.DataSetDAO;

import javax.inject.Inject;
import java.util.Date;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;

public class DatasetService {

    private final DataSetDAO dataSetDAO;

    @Inject
    public DatasetService(DataSetDAO dataSetDAO) {
        this.dataSetDAO = dataSetDAO;
    }

    public DataSet createDataset(String name, Set<DataSetProperty> properties) {
        Date now = new Date();
        int lastAlias = dataSetDAO.findLastAlias();
        int alias = lastAlias + 1;

        Integer id = dataSetDAO.insertDataset(name, now, null, false, alias);

        List<DataSetProperty> propertyList = this.processDatasetProperties(id, now, properties);
        dataSetDAO.insertDataSetsProperties(propertyList);

        DataSet result = dataSetDAO.findDataSetById(id);
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
    public List<DataSetProperty> processDatasetProperties(Integer datasetId, Date now, Set<DataSetProperty> properties) {
        List<DataSetProperty> result = new ArrayList<>(11);
        Iterator<DataSetProperty> iterator = properties.iterator();
        int keyId = 1;
        while (iterator.hasNext()) {
            DataSetProperty dsp = iterator.next();
            dsp.setDataSetId(datasetId);
            dsp.setPropertyKey(keyId);
            dsp.setCreateDate(now);
            result.add(dsp);
            keyId++;
        }
        return result;
    }

}
