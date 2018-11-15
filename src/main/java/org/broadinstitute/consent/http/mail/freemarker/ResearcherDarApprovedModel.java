package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

import java.util.List;

public class ResearcherDarApprovedModel {
    private String researcherName;
    private String darCode;
    private List<DatasetMailDTO> datasets;
    private String dataUseRestriction;
    private String researcherEmail;

    public ResearcherDarApprovedModel(String researcherName, String darCode, List<DatasetMailDTO> datasets, String dataUseRestriction, String researcherEmail) {
        this.researcherName = researcherName;
        this.darCode = darCode;
        this.datasets = datasets;
        this.dataUseRestriction = dataUseRestriction;
        this.researcherEmail = researcherEmail;
    }

    public String getResearcherName() {
        return researcherName;
    }

    public void setResearcherName(String researcherName) {
        this.researcherName = researcherName;
    }

    public String getDarCode() {
        return darCode;
    }

    public void setDarCode(String darCode) {
        this.darCode = darCode;
    }

    public List<DatasetMailDTO> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<DatasetMailDTO> datasets) {
        this.datasets = datasets;
    }

    public String getDataUseRestriction() {
        return dataUseRestriction;
    }

    public void setDataUseRestriction(String dataUseRestriction) {
        this.dataUseRestriction = dataUseRestriction;
    }

    public String getResearcherEmail() {
        return researcherEmail;
    }

    public void setResearcherEmail(String email) {
        this.researcherEmail = email;
    }
}

