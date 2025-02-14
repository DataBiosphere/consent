package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public class Election {

  public static final String QUERY_FIELDS_WITH_E_PREFIX =
      " e.election_id AS e_election_id, "
          + " e.reference_id AS e_reference_id, "
          + " e.status AS e_status, "
          + " e.create_date AS e_create_date, "
          + " e.last_update AS e_last_update, "
          + " e.dataset_id AS e_dataset_id, "
          + " e.election_type AS e_election_type, "
          + " e.archived AS e_archived, "
          + " e.latest AS e_latest ";

  @JsonProperty
  private Integer electionId;

  @JsonProperty
  private String electionType;

  @JsonProperty
  private Boolean finalVote;

  @JsonProperty
  private String status;

  @JsonProperty
  private Date createDate;

  @JsonProperty
  private Date lastUpdate;

  @JsonProperty
  private Date finalVoteDate;

  @JsonProperty
  private String referenceId;

  @JsonProperty
  private String finalRationale;

  @JsonProperty
  private Boolean finalAccessVote;

  @JsonProperty
  private Integer datasetId;

  @JsonProperty
  private String displayId;

  @JsonProperty
  private String dataUseLetter;

  @JsonProperty
  private String dulName;

  @JsonProperty
  private Boolean archived;

  @JsonProperty
  private Integer version;

  @JsonProperty
  private String consentGroupName;

  @JsonProperty
  private String projectTitle;

  @JsonProperty
  private Map<Integer, Vote> votes;

  public Election() {
  }

  public Election(Integer electionId, String electionType,
      String status, Date createDate,
      String referenceId, Date lastUpdate, Boolean finalAccessVote, Integer datasetId) {
    this.electionId = electionId;
    this.electionType = electionType;
    this.status = status;
    this.createDate = createDate;
    this.referenceId = referenceId;
    this.lastUpdate = lastUpdate;
    this.finalAccessVote = finalAccessVote;
    this.datasetId = datasetId;
    this.votes = new HashMap<>();
  }

  public Election(Integer electionId, String electionType,
      String status, Date createDate,
      String referenceId, Date lastUpdate, Boolean finalAccessVote, Integer datasetId,
      Boolean archived,
      String dulName, String dataUseLetter) {
    this.electionId = electionId;
    this.electionType = electionType;
    this.status = status;
    this.createDate = createDate;
    this.referenceId = referenceId;
    this.lastUpdate = lastUpdate;
    this.finalAccessVote = finalAccessVote;
    this.datasetId = datasetId;
    this.archived = archived;
    this.dulName = dulName;
    this.dataUseLetter = dataUseLetter;
  }

  public Integer getElectionId() {
    return electionId;
  }

  public void setElectionId(Integer electionId) {
    this.electionId = electionId;
  }

  public String getElectionType() {
    return electionType;
  }

  public void setElectionType(String electionType) {
    this.electionType = electionType;
  }

  public Boolean getFinalVote() {
    return finalVote;
  }

  public void setFinalVote(Boolean finalVote) {
    this.finalVote = finalVote;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getFinalVoteDate() {
    return finalVoteDate;
  }

  public void setFinalVoteDate(Date finalVoteDate) {
    this.finalVoteDate = finalVoteDate;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public String getFinalRationale() {
    return finalRationale;
  }

  public void setFinalRationale(String finalRationale) {
    this.finalRationale = finalRationale;
  }

  public Boolean getFinalAccessVote() {
    return finalAccessVote;
  }

  public void setFinalAccessVote(Boolean finalAccessVote) {
    this.finalAccessVote = finalAccessVote;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public String getDisplayId() {
    return displayId;
  }

  public void setDisplayId(String displayId) {
    this.displayId = displayId;
  }

  public String getDataUseLetter() {
    return dataUseLetter;
  }

  public void setDataUseLetter(String dataUseLetter) {
    this.dataUseLetter = dataUseLetter;
  }

  public String getDulName() {
    return dulName;
  }

  public void setDulName(String dulName) {
    this.dulName = dulName;
  }

  public Boolean getArchived() {
    return archived;
  }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getConsentGroupName() {
    return consentGroupName;
  }

  public void setConsentGroupName(String consentGroupName) {
    this.consentGroupName = consentGroupName;
  }

  public String getProjectTitle() {
    return projectTitle;
  }

  public void setProjectTitle(String projectTitle) {
    this.projectTitle = projectTitle;
  }

  public Map<Integer, Vote> getVotes() {
    return votes;
  }

  public void setVotes(Map<Integer, Vote> votes) {
    this.votes = votes;
  }

  public void addVote(Vote vote) {
    if (java.util.Objects.isNull(votes)) {
      this.setVotes(new HashMap<>());
    }
    if (java.util.Objects.nonNull(vote)) {
      Integer voteId = vote.getVoteId();
      Vote savedVote = votes.get(voteId);
      if (java.util.Objects.isNull(savedVote)) {
        votes.put(voteId, vote);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Election election = (Election) o;
    return Objects.equal(electionId, election.electionId) &&
        Objects.equal(referenceId, election.referenceId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(electionId, referenceId);
  }

}
