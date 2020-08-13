package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.models.DataSet;

import javax.inject.Inject;
import java.util.Date;


public class DataSetService {

    private final DataSetDAO dataSetDAO;

    @Inject
    public DataSetService(DataSetDAO dataSetDAO) {
        this.dataSetDAO = dataSetDAO;
    }

    public Integer createDataSet(String name, Date createDate, String objectId, Boolean active, Integer alias) {
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
