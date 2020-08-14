package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataAccessRequest {

  @JsonProperty public Integer id;

  @JsonProperty public String referenceId;

  @JsonProperty public DataAccessRequestData data;

  @JsonProperty public Boolean draft;

  @JsonProperty public Integer userId;

  @JsonProperty public Date createDate;

  /*
   * Legacy property on DARs. Used to display the sort order for a DAR. In practice, this also
   * functions as the Update Date. See also https://broadinstitute.atlassian.net/browse/DUOS-728
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
   * Merges the DAR and the DAR Data into a single Map Ignores a series of deprecated keys Null
   * values are ignored by default
   *
   * @return Map<String, Object> Dar in simple map format
   */
  public Map<String, Object> convertToSimplifiedDar() {
    // Serialize dates as longs, but do not deserialize longs into dates so we can output long
    // values in the final result.
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive(date.getTime()))
        .create();
    DataAccessRequestData dataCopy = this.getData();
    this.setData(null);

    String serializedDar = gson.toJson(shallowCopy(this));
    JsonObject dar = gson.fromJson(serializedDar, JsonObject.class);

    String serializedDarData = gson.toJson(dataCopy);
    JsonObject darData = gson.fromJson(serializedDarData, JsonObject.class);

    DataAccessRequestData.DEPRECATED_PROPS.forEach(darData::remove);
    for (String dataKey : darData.keySet()) {
      String camelCasedDataKey =
          dataKey.contains("_")
              ? CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, dataKey)
              : dataKey;
      if (!dar.has(camelCasedDataKey)) {
        dar.add(camelCasedDataKey, darData.get(dataKey));
      }
    }
    Type darMapType = new TypeToken<Map<String, Object>>() {}.getType();
    return gson.fromJson(dar.toString(), darMapType);
  }

  /**
   * Make a shallow copy of the dar. This is mostly a workaround for problems serializing dates
   * when calling Gson.toJson on `this`
   * @param dar DataAccessRequest
   * @return Shallow copy of DataAccessRequest
   */
  private Map<String, Object> shallowCopy(DataAccessRequest dar) {
    Map<String, Object> copy = new HashMap<>();
    copy.put("id", dar.getId());
    copy.put("referenceId", dar.getReferenceId());
    copy.put("createDate", dar.getCreateDate().getTime());
    copy.put("updateDate", dar.getUpdateDate().getTime());
    copy.put("sortDate", dar.getSortDate().getTime());
    copy.put("submissionDate", dar.getSubmissionDate().getTime());
    return copy;
  }
}
