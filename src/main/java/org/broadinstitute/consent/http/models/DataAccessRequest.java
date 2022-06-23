package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public class DataAccessRequest {

  @JsonProperty public Integer id;

  @JsonProperty public String referenceId;

  @JsonProperty public Integer collectionId;

  @JsonProperty public String parentId;

  @JsonProperty public DataAccessRequestData data;

  @JsonProperty public Boolean draft;

  @JsonProperty public Integer userId;

  @JsonProperty public Timestamp createDate;

  /*
   * Legacy property on DARs. Used to display the sort order for a DAR. In practice, this also
   * functions as the Update Date. See also https://broadinstitute.atlassian.net/browse/DUOS-728
   */
  @JsonProperty public Timestamp sortDate;

  @JsonProperty public Timestamp submissionDate;

  @JsonProperty public Timestamp updateDate;

  @JsonProperty public List<Integer> datasetIds;

  @JsonProperty private Map<Integer, Election> elections;

  public DataAccessRequest() {
    this.elections = new HashMap<>();
  }

  public static boolean isCanceled(DataAccessRequest dar) {
    return
      Objects.nonNull(dar) &&
      Objects.nonNull(dar.getData()) &&
      Objects.nonNull(dar.getData().getStatus()) &&
      dar.getData().getStatus().equalsIgnoreCase("canceled");
  }

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

  public Integer getCollectionId() { return collectionId; }

  public void setCollectionId(Integer collectionId) { this.collectionId = collectionId; }

  public String getParentId() { return parentId; }

  public void setParentId(String parentId) { this.parentId = parentId; }

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

  public void setCreateDate(Timestamp createDate) {
    this.createDate = createDate;
  }

  public Date getSortDate() {
    return sortDate;
  }

  public void setSortDate(Timestamp sortDate) {
    this.sortDate = sortDate;
  }

  public Timestamp getSubmissionDate() {
    return submissionDate;
  }

  public void setSubmissionDate(Timestamp submissionDate) {
    this.submissionDate = submissionDate;
  }

  public Timestamp getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
  }

  public void setElections(Map<Integer, Election> elections) {
    this.elections = elections;
  }

  public Map<Integer, Election> getElections() {
    return elections;
  }

  public void addElection(Election election) {
    if(Objects.isNull(elections)) {
      this.setElections(new HashMap<>());
    }
    if(Objects.nonNull(election)) {
      Integer electionId = election.getElectionId();
      Election savedRecord = elections.get(electionId);
      if (Objects.isNull(savedRecord)) {
        elections.put(electionId, election);
      }
    }
  }

  public List<Integer> getDatasetIds() { return datasetIds; }

  public void addDatasetId(Integer id) {
    if (Objects.isNull(datasetIds)) {
      datasetIds = new ArrayList<>();
    }
    datasetIds.add(id);
  }

  /**
   * Merges the DAR and the DAR Data into a single Map Ignores a series of deprecated keys Null
   * values are ignored by default
   *
   * @return Map<String, Object> Dar in simple map format
   */
  public Map<String, Object> convertToSimplifiedDar() {
    // Serialize dates/timestamps as longs, but do not deserialize longs into dates so we can
    // output long values in the final result.
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive(date.getTime()))
        .registerTypeAdapter(Timestamp.class, (JsonSerializer<Timestamp>) (timestamp, type, jsonSerializationContext) -> new JsonPrimitive(timestamp.getTime()))
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

  public boolean requiresManualReview() {
    return
        Objects.nonNull(this.getData()) && (
            (Objects.nonNull(this.getData().getPoa()) && this.getData().getPoa()) ||
                (Objects.nonNull(this.getData().getPopulation()) && this.getData().getPopulation()) ||
                (Objects.nonNull(this.getData().getOther()) && this.getData().getOther()) ||
                (Objects.nonNull(this.getData().getOtherText()) && !this.getData().getOtherText().isBlank()) ||
                (Objects.nonNull(this.getData().getIllegalBehavior()) && this.getData().getIllegalBehavior()) ||
                (Objects.nonNull(this.getData().getAddiction()) && this.getData().getAddiction()) ||
                (Objects.nonNull(this.getData().getSexualDiseases()) && this.getData().getSexualDiseases()) ||
                (Objects.nonNull(this.getData().getStigmatizedDiseases()) && this.getData().getStigmatizedDiseases()) ||
                (Objects.nonNull(this.getData().getVulnerablePopulation()) && this.getData().getVulnerablePopulation()) ||
                (Objects.nonNull(this.getData().getPopulationMigration()) && this.getData().getPopulationMigration()) ||
                (Objects.nonNull(this.getData().getPsychiatricTraits()) && this.getData().getPsychiatricTraits()) ||
                (Objects.nonNull(this.getData().getNotHealth()) && this.getData().getNotHealth())
        );
  }
  /**
   * Make a shallow copy of the dar. This is mostly a workaround for problems serializing dates
   * when calling Gson.toJson on `this`
   *
   * @param dar DataAccessRequest
   * @return Shallow copy of DataAccessRequest
   */
  private Map<String, Object> shallowCopy(DataAccessRequest dar) {
    Map<String, Object> copy = new HashMap<>();
    if (Objects.nonNull(dar.getCreateDate())) copy.put("createDate", dar.getCreateDate().getTime());
    if (Objects.nonNull(dar.getDraft())) copy.put("draft", dar.getDraft());
    if (Objects.nonNull(dar.getId())) copy.put("id", dar.getId());
    if (Objects.nonNull(dar.getReferenceId())) copy.put("referenceId", dar.getReferenceId());
    if (Objects.nonNull(dar.getSortDate())) copy.put("sortDate", dar.getSortDate().getTime());
    if (Objects.nonNull(dar.getSubmissionDate())) copy.put("submissionDate", dar.getSubmissionDate().getTime());
    if (Objects.nonNull(dar.getUpdateDate())) copy.put("updateDate", dar.getUpdateDate().getTime());
    if (Objects.nonNull(dar.getUserId())) copy.put("userId", dar.getUserId());
    return copy;
  }
}
