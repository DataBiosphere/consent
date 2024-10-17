package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;

@Deprecated
public class DatasetDTO {

  @JsonProperty
  private String datasetName;

  @JsonProperty
  private Integer dacId;

  @JsonProperty
  private Integer datasetId;

  @JsonProperty
  private String consentId;

  @JsonProperty
  private Boolean deletable;

  @JsonProperty
  private List<DatasetPropertyDTO> properties;

  @JsonProperty
  private Boolean isAssociatedToDataOwners;

  @JsonProperty
  private Boolean updateAssociationToDataOwnerAllowed;

  @JsonProperty
  private String alias;

  @JsonProperty
  private String objectId;

  @JsonProperty
  private Date createDate;

  @JsonProperty
  private Integer createUserId;

  @JsonProperty
  private Timestamp updateDate;

  @JsonProperty
  private Integer updateUserId;

  @JsonProperty
  public DataUse dataUse;

  public DatasetDTO() {
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public Integer getDacId() {
    return dacId;
  }

  public void setDacId(Integer dacId) {
    this.dacId = dacId;
  }

  public DatasetDTO(List<DatasetPropertyDTO> properties) {
    this.properties = properties;
  }

  public String getConsentId() {
    return consentId;
  }

  public void setConsentId(String consentId) {
    this.consentId = consentId;
  }

  public Boolean getDeletable() {
    return deletable;
  }

  public void setDeletable(Boolean deletable) {
    this.deletable = deletable;
  }

  public String getPropertyValue(String propertyName) {
    return properties.get(properties.indexOf(new DatasetPropertyDTO(propertyName, "")))
        .getPropertyValue();
  }

  public List<DatasetPropertyDTO> getProperties() {
    return properties;
  }

  public void setProperties(List<DatasetPropertyDTO> properties) {
    this.properties = properties;
  }

  public Boolean getIsAssociatedToDataOwners() {
    return isAssociatedToDataOwners;
  }

  public void setIsAssociatedToDataOwners(Boolean isAssociatedToDataOwners) {
    this.isAssociatedToDataOwners = isAssociatedToDataOwners;
  }

  public Boolean getUpdateAssociationToDataOwnerAllowed() {
    return updateAssociationToDataOwnerAllowed;
  }

  public void setUpdateAssociationToDataOwnerAllowed(Boolean updateAssociationToDataOwnerAllowed) {
    this.updateAssociationToDataOwnerAllowed = updateAssociationToDataOwnerAllowed;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setAlias(Integer alias) {
    this.alias = Dataset.parseAliasToIdentifier(alias);
  }

  public String getAlias() {
    return alias;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
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

  public Timestamp getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
  }

  public Integer getUpdateUserId() {
    return updateUserId;
  }

  public void setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
  }

  public void addProperty(DatasetPropertyDTO property) {
    if (this.getProperties() == null) {
      this.setProperties(new ArrayList<>());
    }
    this.getProperties().add(property);
  }

  public DataUse getDataUse() {
    return dataUse;
  }

  public void setDataUse(DataUse dataUse) {
    this.dataUse = dataUse;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DatasetDTO that = (DatasetDTO) o;
    return datasetId.equals(that.datasetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetId);
  }
}
