package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

import java.util.List;

public class DataCustodianApprovalModel {
    List<DatasetMailDTO> datasets;
    String dataDepositorName;
    String darCode;
    String researcherEmail;

    public DataCustodianApprovalModel(List<DatasetMailDTO> datasets,
                                      String dataDepositorName, String darCode, String researcherEmail) {
        this.datasets = datasets;
        this.dataDepositorName = dataDepositorName;
        this.darCode = darCode;
        this.researcherEmail = researcherEmail;
    }

    public List<DatasetMailDTO> getDatasets() {
        return datasets;
    }

    public DataCustodianApprovalModel setDatasets(List<DatasetMailDTO> datasets) {
        this.datasets = datasets;
        return this;
    }

    public String getDataDepositorName() {
        return dataDepositorName;
    }

    public DataCustodianApprovalModel setDataDepositorName(String dataDepositorName) {
        this.dataDepositorName = dataDepositorName;
        return this;
    }

    public String getDarCode() {
        return darCode;
    }

    public DataCustodianApprovalModel setDarCode(String darCode) {
        this.darCode = darCode;
        return this;
    }

    public String getResearcherEmail() {
        return researcherEmail;
    }

    public DataCustodianApprovalModel setResearcherEmail(String researcherEmail) {
        this.researcherEmail = researcherEmail;
        return this;
    }

}
