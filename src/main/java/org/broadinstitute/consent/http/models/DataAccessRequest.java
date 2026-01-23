package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.BadRequestException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(Include.NON_NULL)
public class DataAccessRequest {

  public static final long EXPIRATION_DURATION_MILLIS = TimeUnit.DAYS.toMillis(365);

  @JsonProperty public Integer id;

  @JsonProperty public String referenceId;

  @JsonProperty public Integer collectionId;

  @JsonProperty public Integer parentId;

  @JsonProperty public DataAccessRequestData data;

  @JsonProperty public String darCode;

  @JsonProperty public Boolean draft = true;

  @JsonProperty public Boolean progressReport = false;

  @JsonProperty public Boolean expired = false;

  @JsonProperty public Timestamp expiresAt;

  @JsonProperty public Integer userId;

  @JsonProperty public Timestamp createDate;

  @JsonProperty public Timestamp submissionDate;

  @JsonProperty public Timestamp updateDate;
  @JsonProperty public List<Integer> datasetIds;
  @JsonProperty private Map<Integer, Election> elections;
  @JsonProperty private String eraCommonsId;

  @JsonProperty public Integer approvingSigningOfficialUserId;
  @JsonProperty public Timestamp approvingSigningOfficialApprovedDate;

  @JsonProperty public Timestamp closeoutSigningOfficialApprovedDate;

  @JsonProperty public Integer closeoutSigningOfficialApprovedUserId;

  @JsonProperty public boolean requiresSOApproval;

  public DataAccessRequest() {
    this.elections = new HashMap<>();
  }

  public static boolean isCanceled(DataAccessRequest dar) {
    return Objects.nonNull(dar)
        && Objects.nonNull(dar.getData())
        && Objects.nonNull(dar.getData().getStatus())
        && dar.getData().getStatus().equalsIgnoreCase("canceled");
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

  public Integer getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(Integer collectionId) {
    this.collectionId = collectionId;
  }

  public Integer getParentId() {
    return parentId;
  }

  public void setParentId(Integer parentId) {
    this.parentId = parentId;
    updateProgressReportState();
  }

  public DataAccessRequestData getData() {
    return data;
  }

  public void setData(DataAccessRequestData data) {
    this.data = data;
  }

  public boolean getDraft() {
    return draft;
  }

  public boolean getExpired() {
    return expired;
  }

  public Timestamp getExpiresAt() {
    return expiresAt;
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

  public Timestamp getSubmissionDate() {
    return submissionDate;
  }

  public void setSubmissionDate(Timestamp submissionDate) {
    this.submissionDate = submissionDate;
    draft = submissionDate == null;
    expired =
        submissionDate != null
            && submissionDate.before(
                new Timestamp(System.currentTimeMillis() - EXPIRATION_DURATION_MILLIS));
    expiresAt =
        (submissionDate != null)
            ? new Timestamp(submissionDate.getTime() + EXPIRATION_DURATION_MILLIS)
            : null;
    updateProgressReportState();
  }

  public boolean getProgressReport() {
    return progressReport;
  }

  public Timestamp getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
  }

  public Map<Integer, Election> getElections() {
    return elections;
  }

  public void setElections(Map<Integer, Election> elections) {
    this.elections = elections;
  }

  public void addElection(Election election) {
    if (Objects.isNull(elections)) {
      this.setElections(new HashMap<>());
    }
    if (Objects.nonNull(election)) {
      Integer electionId = election.getElectionId();
      Election savedRecord = elections.get(electionId);
      if (Objects.isNull(savedRecord)) {
        elections.put(electionId, election);
      }
    }
  }

  public List<Integer> getDatasetIds() {
    if (Objects.isNull(datasetIds)) {
      return List.of();
    }
    return datasetIds;
  }

  public void setDatasetIds(List<Integer> datasetIds) {
    this.datasetIds = datasetIds;
  }

  public void addDatasetId(Integer id) {
    if (Objects.isNull(datasetIds)) {
      datasetIds = new ArrayList<>();
    }
    if (!datasetIds.contains(id)) {
      datasetIds.add(id);
    }
  }

  public void addDatasetIds(List<Integer> ids) {
    if (Objects.isNull(datasetIds)) {
      datasetIds = new ArrayList<>();
    }
    if (Objects.nonNull(ids) && !ids.isEmpty()) {
      datasetIds =
          Stream.of(datasetIds, ids).flatMap(List::stream).distinct().collect(Collectors.toList());
    }
  }

  public void setDarCode(String darCode) {
    this.darCode = darCode;
  }

  public String getDarCode() {
    return darCode;
  }

  public String getEraCommonsId() {
    return eraCommonsId;
  }

  public void setEraCommonsId(String eraCommonsId) {
    this.eraCommonsId = eraCommonsId;
  }

  public Integer getCloseoutSigningOfficialApprovedUserId() {
    return closeoutSigningOfficialApprovedUserId;
  }

  public void setCloseoutSigningOfficialApprovedUserId(
      Integer closeoutSigningOfficialApprovedUserId) {
    this.closeoutSigningOfficialApprovedUserId = closeoutSigningOfficialApprovedUserId;
  }

  public Timestamp getCloseoutSigningOfficialApprovedDate() {
    return closeoutSigningOfficialApprovedDate;
  }

  public void setCloseoutSigningOfficialApprovedDate(
      Timestamp closeoutSigningOfficialApprovedDate) {
    this.closeoutSigningOfficialApprovedDate = closeoutSigningOfficialApprovedDate;
  }

  public Integer getApprovingSigningOfficialUserId() {
    return this.approvingSigningOfficialUserId;
  }

  public Timestamp getApprovingSigningOfficialApprovedDate() {
    return this.approvingSigningOfficialApprovedDate;
  }

  public void setApprovingSigningOfficialUserId(Integer userId) {
    this.approvingSigningOfficialUserId = userId;
  }

  public void setApprovingSigningOfficialApprovedDate(Timestamp when) {
    this.approvingSigningOfficialApprovedDate = when;
  }

  public void setRequiresSOApproval(boolean requiresSOApproval) {
    this.requiresSOApproval = requiresSOApproval;
  }

  public boolean getRequiresSOApproval() {
    return this.requiresSOApproval;
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
    Gson gson =
        new GsonBuilder()
            .registerTypeAdapter(
                Date.class,
                (JsonSerializer<Date>)
                    (date, type, jsonSerializationContext) -> new JsonPrimitive(date.getTime()))
            .registerTypeAdapter(
                Timestamp.class,
                (JsonSerializer<Timestamp>)
                    (timestamp, type, jsonSerializationContext) ->
                        new JsonPrimitive(timestamp.getTime()))
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
    return Objects.nonNull(this.getData())
        && ((Objects.nonNull(this.getData().getPoa()) && this.getData().getPoa())
            || (Objects.nonNull(this.getData().getPopulation()) && this.getData().getPopulation())
            || (Objects.nonNull(this.getData().getOther()) && this.getData().getOther())
            || (Objects.nonNull(this.getData().getOtherText())
                && !this.getData().getOtherText().isBlank())
            || (Objects.nonNull(this.getData().getIllegalBehavior())
                && this.getData().getIllegalBehavior())
            || (Objects.nonNull(this.getData().getAddiction()) && this.getData().getAddiction())
            || (Objects.nonNull(this.getData().getSexualDiseases())
                && this.getData().getSexualDiseases())
            || (Objects.nonNull(this.getData().getStigmatizedDiseases())
                && this.getData().getStigmatizedDiseases())
            || (Objects.nonNull(this.getData().getVulnerablePopulation())
                && this.getData().getVulnerablePopulation())
            || (Objects.nonNull(this.getData().getPopulationMigration())
                && this.getData().getPopulationMigration())
            || (Objects.nonNull(this.getData().getPsychiatricTraits())
                && this.getData().getPsychiatricTraits())
            || (Objects.nonNull(this.getData().getNotHealth()) && this.getData().getNotHealth()));
  }

  /**
   * Populate a new Data Access Request from the JSON string and the parent Data Access Request.
   * Copies all the data from the parent dar, then overwrites the collaborators and datasets. Adds
   * all progress report specific fields.
   *
   * @param json The JSON string to populate the new Progress Report.
   * @param parentDar The parent Data Access Request to copy data from.
   * @return A new Progress Report populated with the provided JSON string and parent DAR data.
   */
  public static DataAccessRequest populateProgressReportFromJsonString(
      String json, DataAccessRequest parentDar) {
    DataAccessRequest newDar = new DataAccessRequest();
    DataAccessRequestData newData = DataAccessRequestData.populateDARData(json);
    DataAccessRequestData originalDataCopy =
        DataAccessRequestData.fromString(parentDar.getData().toString());

    String referenceId = UUID.randomUUID().toString();
    newDar.setReferenceId(referenceId);
    newDar.setParentId(parentDar.getId());
    newDar.setCollectionId(parentDar.getCollectionId());

    newDar.addDatasetIds(newData.getDatasetIds());
    originalDataCopy.setInternalCollaborators(newData.getInternalCollaborators());
    originalDataCopy.setExternalCollaborators(newData.getExternalCollaborators());
    originalDataCopy.setLabCollaborators(newData.getLabCollaborators());
    originalDataCopy.setProgressReportSummary(newData.getProgressReportSummary());
    originalDataCopy.setIntellectualProperties(newData.getIntellectualProperties());
    originalDataCopy.setPublications(newData.getPublications());
    originalDataCopy.setPresentations(newData.getPresentations());
    originalDataCopy.setDmi(newData.getDmi());
    originalDataCopy.setResearchPlans(newData.getResearchPlans());
    validateCloseoutSupplement(newData.getCloseoutSupplement());
    originalDataCopy.setCloseoutSupplement(newData.getCloseoutSupplement());
    originalDataCopy.setPubAcknowledgement(newData.getPubAcknowledgement());
    originalDataCopy.setDSAcknowledgement(newData.getDSAcknowledgement());
    originalDataCopy.setGSOAcknowledgement(newData.getGSOAcknowledgement());

    // These values will be updated in populateProgressReportWithDocuments if documents exist.
    // Its important we don't copy over the parent values so those documents are not deleted.
    originalDataCopy.setCollaborationLetterName(null);
    originalDataCopy.setCollaborationLetterLocation(null);
    originalDataCopy.setIrbDocumentName(null);
    originalDataCopy.setIrbDocumentLocation(null);

    // We need to update the reference ID in the DataAccessRequestData object with the new reference
    // ID computed in this method so that the frontend can rely on the value set to point to this
    // object and not the original DAR.
    originalDataCopy.setReferenceId(referenceId);

    newDar.setData(originalDataCopy);
    return newDar;
  }

  @VisibleForTesting
  protected static void validateCloseoutSupplement(CloseoutSupplement closeoutSupplement) {
    if (Objects.isNull(closeoutSupplement)) {
      return;
    }

    if ((Objects.isNull(closeoutSupplement.reasons()) || closeoutSupplement.reasons().isEmpty())
        && Objects.isNull(closeoutSupplement.signingOfficialId())
        && (Objects.isNull(closeoutSupplement.otherText())
            || closeoutSupplement.otherText().isEmpty())) {
      throw new BadRequestException("A closeout supplement must have values provided.");
    }

    if (Objects.isNull(closeoutSupplement.reasons()) || closeoutSupplement.reasons().isEmpty()) {
      throw new BadRequestException("A closeout supplement must have reasons provided.");
    }

    if (Objects.isNull(closeoutSupplement.signingOfficialId())) {
      throw new BadRequestException(
          "A closeout supplement must have a signing official id provided.");
    }
  }

  /**
   * Make a shallow copy of the dar. This is mostly a workaround for problems serializing dates when
   * calling Gson.toJson on `this`
   *
   * @param dar DataAccessRequest
   * @return Shallow copy of DataAccessRequest
   */
  private Map<String, Object> shallowCopy(DataAccessRequest dar) {
    Map<String, Object> copy = new HashMap<>();
    if (Objects.nonNull(dar.getCreateDate())) {
      copy.put("createDate", dar.getCreateDate().getTime());
    }
    copy.put("draft", dar.getDraft());
    copy.put("expired", dar.getExpired());
    copy.put("expiredAt", dar.getExpiresAt());
    if (Objects.nonNull(dar.getId())) {
      copy.put("id", dar.getId());
    }
    if (Objects.nonNull(dar.getReferenceId())) {
      copy.put("referenceId", dar.getReferenceId());
    }
    if (Objects.nonNull(dar.getSubmissionDate())) {
      copy.put("submissionDate", dar.getSubmissionDate().getTime());
    }
    if (Objects.nonNull(dar.getUpdateDate())) {
      copy.put("updateDate", dar.getUpdateDate().getTime());
    }
    if (Objects.nonNull(dar.getUserId())) {
      copy.put("userId", dar.getUserId());
    }
    if (Objects.nonNull(dar.getCollectionId())) {
      copy.put("collectionId", dar.getCollectionId());
    }
    if (Objects.nonNull(dar.getDatasetIds())) {
      copy.put("datasetIds", dar.getDatasetIds());
    }
    if (Objects.nonNull(dar.getParentId())) {
      copy.put("parentId", dar.getParentId());
    }
    if (Objects.nonNull(dar.getDarCode())) {
      copy.put("darCode", dar.getDarCode());
    }
    if (dar.getEraCommonsId() != null) {
      copy.put("eraCommonsId", dar.getEraCommonsId());
    }
    if (dar.getCloseoutSigningOfficialApprovedUserId() != null) {
      copy.put(
          "closeoutSigningOfficialApprovedUserId", dar.getCloseoutSigningOfficialApprovedUserId());
    }
    if (dar.getCloseoutSigningOfficialApprovedDate() != null) {
      copy.put(
          "closeoutSigningOfficialApprovedDate", dar.getCloseoutSigningOfficialApprovedUserId());
    }
    if (dar.getApprovingSigningOfficialUserId() != null) {
      copy.put("approvingSigningOfficialUserId", dar.getApprovingSigningOfficialUserId());
    }
    if (dar.getApprovingSigningOfficialApprovedDate() != null) {
      copy.put(
          "approvingSigningOfficialApprovedDate", dar.getApprovingSigningOfficialApprovedDate());
    }
    if (dar.getRequiresSOApproval()) {
      copy.put("requiresSOApproval", dar.getRequiresSOApproval());
    }
    return copy;
  }

  // Simple state machine for determining if the Data Access Request
  // is a progress report.
  private void updateProgressReportState() {
    progressReport = !getDraft() && (getParentId() != null);
  }

  public boolean getIsCloseoutProgressReport() {
    return Objects.nonNull(getParentId())
        && this.getData() != null
        && this.getData().getCloseoutSupplement() != null
        && !this.getData().getCloseoutSupplement().reasons().isEmpty();
  }

  public boolean getHasSOCloseoutApproval() {
    return this.getCloseoutSigningOfficialApprovedDate() != null
        && this.getCloseoutSigningOfficialApprovedUserId() != null;
  }

  public boolean getHasDMI() {
    return this.getData() != null
        && this.getData().getDmi() != null
        && this.getData().getDmi().incidents() != null
        && !this.getData().getDmi().incidents().isEmpty();
  }
}
