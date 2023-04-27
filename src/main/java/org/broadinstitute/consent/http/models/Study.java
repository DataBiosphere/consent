package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Study {
    private Integer studyId;
    private String name;
    private String description;
    private Boolean publicVisibility;
    private String piName;
    private Set<String> dataTypes;
    private Set<Integer> datasetIds;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void addProperty(StudyProperty prop) {
        if (Objects.isNull(this.properties)) {
            this.properties = new ArrayList<>();
        }
        this.properties.add(prop);
    }

    public FileStorageObject getAlternativeDataSharingPlan() {
        return alternativeDataSharingPlan;
    }

    public void setAlternativeDataSharingPlan(FileStorageObject alternativeDataSharingPlan) {
        this.alternativeDataSharingPlan = alternativeDataSharingPlan;
    }
    public Set<Integer> getDatasetIds() {
        return datasetIds;
    }

    public void setDatasetIds(Set<Integer> datasetIds) {
        this.datasetIds = datasetIds;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Integer getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Integer createUserId) {
        this.createUserId = createUserId;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Integer updateUserId) {
        this.updateUserId = updateUserId;
    }
}
