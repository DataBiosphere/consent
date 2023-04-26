package org.broadinstitute.consent.http.models;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Study {
    private Integer studyId;
    private String studyName;
    private String studyDescription;
    private Boolean publicVisibility;
    private String piName;
    private Set<String> dataTypes;
    private Set<Dataset> datasets;
    private List<StudyProperty> properties;
    private FileStorageObject alternativeDataSharingPlan;
    private Date createDate;
    private Integer createUserId;
    private Date updateDate;
    private Integer updateUserId;


    public Integer getStudyId() {
        return studyId;
    }

    public void setStudyId(Integer studyId) {
        this.studyId = studyId;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public void setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
    }

    public Boolean getPublicVisibility() {
        return publicVisibility;
    }

    public void setPublicVisibility(Boolean publicVisibility) {
        this.publicVisibility = publicVisibility;
    }

    public String getPiName() {
        return piName;
    }

    public void setPiName(String piName) {
        this.piName = piName;
    }

    public Set<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(Set<Dataset> datasets) {
        this.datasets = datasets;
    }
    public void addDataset(Dataset ds) {
        if (Objects.isNull(this.datasets)) {
            this.datasets = new HashSet<>();
        }
        this.datasets.add(ds);
    }

    public Set<String> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Set<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public List<StudyProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<StudyProperty> properties) {
        this.properties = properties;
    }

    public FileStorageObject getAlternativeDataSharingPlan() {
        return alternativeDataSharingPlan;
    }

    public void setAlternativeDataSharingPlan(FileStorageObject alternativeDataSharingPlan) {
        this.alternativeDataSharingPlan = alternativeDataSharingPlan;
    }


}
