package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;

import java.util.List;

public class DataCustodianApprovalModel {
    DataAccessRequest dataAccessRequest;
    List<DataSet> datasets;
    String userName;

    public DataCustodianApprovalModel(DataAccessRequest dataAccessRequest, List<DataSet> datasets, String userName) {
        this.dataAccessRequest = dataAccessRequest;
        this.datasets = datasets;
        this.userName = userName;
    }

    public DataAccessRequest getDataAccessRequest() {
        return dataAccessRequest;
    }

    public DataCustodianApprovalModel setDataAccessRequest(DataAccessRequest dataAccessRequest) {
        this.dataAccessRequest = dataAccessRequest;
        return this;
    }

    public List<DataSet> getDatasets() {
        return datasets;
    }

    public DataCustodianApprovalModel setDatasets(List<DataSet> datasets) {
        this.datasets = datasets;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public DataCustodianApprovalModel setUserName(String userName) {
        this.userName = userName;
        return this;
    }

}
