package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

public class DataAccessRequest {

  private static final Gson GSON = new Gson();

  @JsonProperty public Integer id;

  @JsonProperty public String referenceId;

  @JsonProperty public DataAccessRequestData data;

  @JsonProperty public Boolean draft;

  @JsonProperty public Integer userId;

  @JsonProperty public Date createDate;

  /**
   * Legacy property on DARs. Used to display the sort order for a DAR.
   * In practice, this also functions as the Update Date.
   * See also https://broadinstitute.atlassian.net/browse/DUOS-728
   */
  @JsonProperty public Date sortDate;

  @JsonProperty public Date submissionDate;

  @JsonProperty public Date updateDate;

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

  /**
   * Merges the DAR and the DAR Data into a single Map
   * Ignores a series of deprecated keys
   * Null values are ignored by default
   *
   * @return Map<String, Object> Dar in simple map format
   */
  public Map<String, Object> convertToSimplifiedDar() {
    JsonObject dar = GSON.toJsonTree(this).getAsJsonObject();
    dar.remove("data");
    JsonObject darData = GSON.toJsonTree(this.getData()).getAsJsonObject();
    DataAccessRequestData.DEPRECATED_PROPS.forEach(darData::remove);
    for (String dataKey: darData.keySet()) {
      String camelCasedDataKey = dataKey.contains("_") ?
          CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, dataKey) :
          dataKey;
      if (!dar.has(camelCasedDataKey)) {
        dar.add(camelCasedDataKey, darData.get(dataKey));
      }
    }
    Type darMapType = new TypeToken<Map<String, Object>>() {}.getType();
    return GSON.fromJson(dar.toString(), darMapType);
  }
}
