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

    public DataSet createTestDataSet(String json) {
        return new DataSet(json);
    }

    public Integer createDataset(String name, String objectId, Boolean active, Integer alias) {
        Date now = new Date();

        //     Integer insertDataset(
        //     @Bind("name") String name,
        //     @Bind("createDate") Date createDate,
        //     @Bind("objectId") String objectId,
        //     @Bind("active") Boolean active,
        //     @Bind("alias") Integer alias);
        return dataSetDAO.insertDataset(name, now, objectId, active, alias);
    }

}
