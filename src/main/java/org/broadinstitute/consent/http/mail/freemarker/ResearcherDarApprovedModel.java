package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

import java.util.List;

public class ResearcherDarApprovedModel {
    private String researcherName;
    private String darCode;
    private List<DatasetMailDTO> datasets;
    private String dataUseRestriction;
    private String researcherEmail;

    public String getResearcherName() {
        return researcherName;
    }

    public ResearcherDarApprovedModel setResearcherName(String researcherName) {
        this.researcherName = researcherName;
        return this;
    }

    public String getDarCode() {
        return darCode;
    }

    public ResearcherDarApprovedModel setDarCode(String darCode) {
        this.darCode = darCode;
        return this;
    }

    public List<DatasetMailDTO> getDatasets() {
        return datasets;
    }

    public ResearcherDarApprovedModel setDatasets(List<DatasetMailDTO> datasets) {
        this.datasets = datasets;
        return this;
    }

    public String getDataUseRestriction() {
        return dataUseRestriction;
    }

    public ResearcherDarApprovedModel setDataUseRestriction(String dataUseRestriction) {
        this.dataUseRestriction = dataUseRestriction;
        return this;
    }

    public String getResearcherEmail() {
        return researcherEmail;
    }

    public ResearcherDarApprovedModel setResearcherEmail(String email) {
        this.researcherEmail = email;
        return this;
    }
}

