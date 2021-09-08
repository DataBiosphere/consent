package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;

//represents a multi-dataset access request
public class DarCollection {

  @JsonProperty
  Integer darCollectionId;

  @JsonProperty
  String darCode;

  @JsonProperty
  Timestamp createDate;

  @JsonProperty
  Integer createUserId;

  @JsonProperty
  Timestamp updateDate;

  @JsonProperty
  Integer updateUserId;

  @JsonProperty
  List<DataAccessRequest> dars;

  HashMap<DataUse, List<DatasetDTO>> datasetBins;

  public DarCollection() {
    this.createDate = new Timestamp(System.currentTimeMillis());
    this.datasetBins = new HashMap<DataUse, List<DatasetDTO>>();
  }

  public Integer getDarCollectionId() {
    return darCollectionId;
  }

  public void setDarCollectionId(Integer darCollectionId) {
    this.darCollectionId = darCollectionId;
  }

  public String getDarCode() {
    return darCode;
  }

  public void setDarCode(String darCode) {
    this.darCode = darCode;
  }

  public Timestamp getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Timestamp createDate) {
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

  public void setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
  }

  public Integer getUpdateUserId() {
    return updateUserId;
  }

  public void setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
  }

  public List<DataAccessRequest> getDars() {
    if (Objects.isNull(dars)) {
      return new ArrayList<>();
    }
    return dars;
  }

  public void setDars(List<DataAccessRequest> dars) { this.dars = dars; }

  public void addDar(DataAccessRequest dar) {
    if (Objects.isNull(dars)) {
      this.setDars(new ArrayList<>());
    }
    dars.add(dar);
  }

  public void addDatasetToBucket(DatasetDTO dataset) {
    for(DataUse dataUse : datasetBins.keySet()) {
      if(dataUse.isDataUseEqual(dataset.getDataUse())) {
        datasetBins.get(dataUse).add(dataset);
      } else {
        List<DatasetDTO> datasets = new ArrayList<DatasetDTO>();
        datasets.add(dataset);
        datasetBins.put(dataset.getDataUse(), datasets);
      }
    }
  }

  @Override
  public boolean equals(Object obj){
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    DarCollection other = (DarCollection) obj;
    return new EqualsBuilder()
      .append(this.getDarCollectionId(), other.getDarCollectionId())
      .append(this.getDarCode(), other.getDarCode())
      .isEquals();
  }


}
