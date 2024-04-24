package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;


public class DatasetAssociation {


  @JsonProperty
  private Integer datasetId;

  @JsonProperty
  private Integer userId;

  @JsonProperty
  private Date createDate;

  public DatasetAssociation() {
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Integer getUserId() {
    return userId;
  }

}
