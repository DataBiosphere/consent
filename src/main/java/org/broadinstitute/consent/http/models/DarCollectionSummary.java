package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.broadinstitute.consent.http.enumeration.DarCollectionActions;

public class DarCollectionSummary {

  @Expose
  private Integer darCollectionId;
  @Expose
  private Set<String> referenceIds;
  @Expose
  private String darCode;
  @Expose
  private String name;
  @Expose
  private Timestamp submissionDate;
  @Expose
  private boolean expired;
  @Expose
  private Timestamp expiresAt;
  @Expose
  private String researcherName;
  @Expose
  private String institutionName;
  @Expose
  private String status;
  @Expose
  private Set<String> actions;
  @Expose
  private int datasetCount;
  @Expose
  private final Map<Integer, Set<String>> parentToReferenceIds;
  @Expose
  private boolean progressReport;
  @Expose
  private String latestReferenceId;
  @Expose
  private Integer closeoutSigningOfficialId;
  @Expose
  private Timestamp closeoutSigningOfficialApprovalDate;
  @Expose
  private List<String> dacNames;
  @Expose
  private Integer researcherId;
  @Expose
  private Integer institutionId;
  @Expose
  private Set<Integer> datasetIds;

  // Normally unused by the UI, but used in data population. Can be included in the JSON response
  // if needed by using a GsonBuilder without `excludeFieldsWithoutExposeAnnotation`.
  private List<Vote> votes;
  private final Map<Integer, Election> datasetElections;
  private Map<Integer, Election> elections;
  private final Map<String, String> darStatuses;
  private CloseoutSupplement closeoutSupplement;

  public DarCollectionSummary() {
    this.votes = new ArrayList<>();
    this.actions = new HashSet<>();
    this.datasetElections = new HashMap<>();
    this.elections = new HashMap<>();
    this.datasetIds = new HashSet<>();
    this.referenceIds = new HashSet<>();
    this.darStatuses = new HashMap<>();
    this.parentToReferenceIds = new HashMap<>();
    this.datasetCount = 0;
  }

  public List<Vote> getVotes() {
    return votes;
  }

  public void setVotes(List<Vote> votes) {
    this.votes = votes;
  }

  public Set<String> getReferenceIds() {
    return this.referenceIds;
  }

  public void addReferenceId(String id) {
    this.referenceIds.add(id);
  }

  public void addParentChildRelationship(Integer parentId, String childReferenceId) {
    parentToReferenceIds.computeIfAbsent(parentId, k -> new HashSet<>()).add(childReferenceId);
    updateProgressReportStatus();
  }

  private Map<Integer, Set<String>> getParentToReferenceIds() {
    return parentToReferenceIds;
  }

  public void setReferenceIds(Set<String> referenceIds) {
    this.referenceIds = new HashSet<>(referenceIds);
  }

  public void addVote(Vote vote) {
    this.votes.add(vote);
  }

  // Compute a new dataset election if it does not exist, or update the existing one to be
  // the latest election for that dataset. Older dataset elections are discarded.
  public void addDatasetElection(Election election) {
    if (this.datasetElections.isEmpty() || !this.datasetElections.containsKey(election.getDatasetId())) {
      this.datasetElections.put(election.getDatasetId(), election);
    } else {
      this.datasetElections.computeIfPresent(
          election.getDatasetId(),
          (key, existingElection) -> {
            // Prefer comparing the creation date, id is an acceptable fallback
            if (election.getCreateDate() != null && existingElection.getCreateDate() != null && election.getCreateDate().after(existingElection.getCreateDate())) {
              return election;
            } else if (election.getElectionId() > existingElection.getElectionId()) {
              return election;
            }
            return existingElection;
          }
      );
    }
  }

  public Map<Integer, Election> getDatasetElections() {
    return datasetElections;
  }

  public void addElection(Election election) {
    this.elections.put(election.getElectionId(), election);
  }

  public Map<Integer, Election> getElections() {
    return elections;
  }

  public Election findElection(Integer electionId) {
    return elections.get(electionId);
  }

  public void setElections(Map<Integer, Election> elections) {
    this.elections = elections;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Timestamp getSubmissionDate() {
    return submissionDate;
  }

  public void setSubmissionDate(Timestamp submissionDate) {
    this.submissionDate = submissionDate;
    if (submissionDate != null) {
      this.expiresAt = Timestamp.from(Instant.ofEpochMilli(submissionDate.getTime() + DataAccessRequest.EXPIRATION_DURATION_MILLIS));
      this.expired = this.expiresAt.before(Timestamp.from(Instant.now()));
    }
    updateProgressReportStatus();
  }

  public void setCloseoutSigningOfficialId(Integer darCloseoutSigningOfficialApprovalId) {
    this.closeoutSigningOfficialId = darCloseoutSigningOfficialApprovalId;
  }

  public Timestamp getCloseoutSigningOfficialApprovalDate() {
    return closeoutSigningOfficialApprovalDate;
  }

  public void setCloseoutSigningOfficialApprovalDate(
      Timestamp darCloseoutSigningOfficialApprovalDate) {
    this.closeoutSigningOfficialApprovalDate = darCloseoutSigningOfficialApprovalDate;
  }

  public Integer getCloseoutSigningOfficialApprovalId() {
    return closeoutSigningOfficialId;
  }

  public boolean isExpired() {
    return expired;
  }

  public Timestamp getExpiresAt() {
    return expiresAt;
  }

  public String getResearcherName() {
    return researcherName;
  }

  public void setResearcherName(String researcherName) {
    this.researcherName = researcherName;
  }

  public Integer getResearcherId() {
    return researcherId;
  }

  public void setResearcherId(Integer researcherId) {
    this.researcherId = researcherId;
  }

  public String getInstitutionName() {
    return institutionName;
  }

  public void setInstitutionName(String institutionName) {
    this.institutionName = institutionName;
  }

  public Integer getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(Integer institutionId) {
    this.institutionId = institutionId;
  }

  public Set<Integer> getDatasetIds() {
    return datasetIds;
  }

  public void setDatasetIds(Set<Integer> datasetIds) {
    this.datasetIds = datasetIds;
    this.datasetCount = this.datasetIds.size();
  }

  public void addDatasetId(Integer datasetId) {
    this.datasetIds.add(datasetId);
    this.datasetCount = this.datasetIds.size();
  }

  public int getDatasetCount() {
    return datasetCount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Set<String> getActions() {
    return actions;
  }

  public void setActions(Set<String> actions) {
    this.actions = actions;
  }

  public void addAction(DarCollectionActions action) {
    actions.add(action.getValue());
  }

  public void addStatus(String status, String referenceId) {
    darStatuses.put(referenceId, status);
  }

  public Map<String, String> getDarStatuses() {
    return darStatuses;
  }

  public List<String> getDacNames() {
    return dacNames;
  }

  public void setDacNames(List<String> dacNames) {
    this.dacNames = dacNames;
  }

  public void addDacName(String dacName) {
    if (Objects.isNull(this.dacNames)) {
      this.dacNames = new ArrayList<>();
    }
    if (!this.dacNames.contains(dacName)) {
      this.dacNames.add(dacName);
    }
  }

  public boolean getProgressReport() {
    return progressReport;
  }

  public String getLatestReferenceId() {
    return latestReferenceId;
  }

  public void setLatestReferenceId(String latestReferenceId) {
    this.latestReferenceId = latestReferenceId;
  }

  public CloseoutSupplement getCloseoutSupplement() {
    return closeoutSupplement;
  }

  public void setCloseoutSupplement(
      CloseoutSupplement closeoutSupplement) {
    this.closeoutSupplement = closeoutSupplement;
  }

  private void updateProgressReportStatus() {
    progressReport = !getParentToReferenceIds().isEmpty() && Objects.nonNull(getSubmissionDate());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    DarCollectionSummary other = (DarCollectionSummary) obj;
    return new EqualsBuilder()
        .append(this.getDarCollectionId(), other.getDarCollectionId())
        .append(this.getDarCode(), other.getDarCode())
        .isEquals();
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
