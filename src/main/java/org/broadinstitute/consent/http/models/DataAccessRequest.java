package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

public class DataAccessRequest {

  @JsonProperty public Integer id;

  @JsonProperty public String referenceId;

  @JsonProperty public DataAccessRequestData data;

  @JsonProperty public Boolean draft;

  @JsonProperty public Integer userId;

  @JsonProperty public Date createDate;

  @JsonProperty public Date sortDate;

  @JsonProperty public Date submissionDate;

  @JsonProperty public Date updateDate;

  public DataAccessRequest() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public DataAccessRequestData getData() {
    return data;
  }

  public void setData(DataAccessRequestData data) {
    this.data = data;
  }

  public Boolean getDraft() {
    return draft;
  }

  public void setDraft(Boolean draft) {
    this.draft = draft;
  }

  public Integer getUserId() {
    return userId;
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

  public Date getSortDate() {
    return sortDate;
  }

  public void setSortDate(Date sortDate) {
    this.sortDate = sortDate;
  }

  public Date getSubmissionDate() {
    return submissionDate;
  }

  public void setSubmissionDate(Date submissionDate) {
    this.submissionDate = submissionDate;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }
}
