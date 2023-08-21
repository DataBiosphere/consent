package org.broadinstitute.consent.http.models;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Study {

  private Integer studyId;
  private String name;
  private String description;
  private Boolean publicVisibility;
  private String piName;
  private List<String> dataTypes;
  private Set<Integer> datasetIds;
  private Set<Dataset> datasets;
  private Set<StudyProperty> properties;
  private FileStorageObject alternativeDataSharingPlan;
  private Date createDate;
  private String createUserEmail;
  private Integer createUserId;
  private Date updateDate;
  private Integer updateUserId;
  private UUID uuid;


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

  public List<String> getDataTypes() {
    return dataTypes;
  }

  public void setDataTypes(List<String> dataTypes) {
    this.dataTypes = dataTypes;
  }

  public Set<StudyProperty> getProperties() {
    return properties;
  }

  public void setProperties(Set<StudyProperty> properties) {
    this.properties = properties;
  }

  public void addProperty(StudyProperty prop) {
    if (Objects.isNull(this.properties)) {
      this.properties = new HashSet<>();
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

  public void addDatasetId(Integer datasetId) {
    if (Objects.isNull(this.datasetIds)) {
      this.datasetIds = new HashSet<>();
    }

    this.datasetIds.add(datasetId);
  }

  public void addDatasets(List<Dataset> datasetList) {
    this.datasets = new HashSet<>(datasetList);
  }

  public Set<Dataset> getDatasets() {
    return datasets;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public String getCreateUserEmail() {
    return createUserEmail;
  }

  public void setCreateUserEmail(String createUserEmail) {
    this.createUserEmail = createUserEmail;
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

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }
}
