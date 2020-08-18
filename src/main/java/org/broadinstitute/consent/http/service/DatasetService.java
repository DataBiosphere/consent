package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DataSetDAO;

import javax.inject.Inject;
import java.util.Date;
import org.broadinstitute.consent.http.models.DataSet;


public class DatasetService {

    private final DataSetDAO dataSetDAO;

    @Inject
    public DatasetService(DataSetDAO dataSetDAO) {
        this.dataSetDAO = dataSetDAO;
    }

    public DataSet createDataset(String name) {
        Date now = new Date();
        int lastAlias = dataSetDAO.findLastAlias();
        int alias = lastAlias + 1;

        //     Integer insertDataset(
        //     @Bind("name") String name,
        //     @Bind("createDate") Date createDate,
        //     @Bind("objectId") String objectId,
        //     @Bind("active") Boolean active,
        //     @Bind("alias") Integer alias);
        int id = dataSetDAO.insertDataset(name, now, null, false, alias);
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

}
