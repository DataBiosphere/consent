package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;

import org.broadinstitute.consent.http.enumeration.DarCollectionActions;

import java.sql.Timestamp;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

public class DarCollectionSummary {

  @JsonProperty
  private Integer darCollectionId;

  @JsonProperty
  private String darCode;

  @JsonProperty
  private String name;

  @JsonProperty
  private Timestamp submissionDate;

  @JsonProperty
  private String researcherName;

  @JsonProperty
  private String institutionName;

  @JsonProperty
  private Integer datasetCount;

  @JsonProperty
  private String status;

  @JsonProperty
  private Set<String> actions;

  @JsonProperty
  private Boolean hasVoted; 

  private List<Vote> votes;
  private List<Election> elections;

  public DarCollectionSummary() {
    this.votes = new ArrayList<>();
    this.actions = new HashSet<>();
    this.elections = new ArrayList<>();
    this.hasVoted = false;
  }

  public List<Vote> getVotes() {
    return votes;
  }

  public void setVotes(List<Vote> votes) {
    this.votes = votes;
  }

  public void addVote(Vote vote) {
    this.votes.add(vote);
  }

  public void addElection(Election election) {
    this.elections.add(election);
  }
 
  public List<Election> getElections() {
    return elections;
  }

  public void setElections(List<Election> elections) {
    this.elections = elections;
  }

  public void setHasVoted(Boolean hasVoted) {
    this.hasVoted = hasVoted;
  }

  public Boolean getHasVoted() {
    return hasVoted;
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
  }

  public String getResearcherName() {
    return researcherName;
  }

  public void setResearcherName(String researcherName) {
    this.researcherName = researcherName;
  }

  public String getResearcherInstitutionName() {
    return institutionName;
  }

  public void setResearcherInstitutuionName(String institutionName) {
    this.institutionName = institutionName;
  }

  public Integer getDatasetCount() {
    return datasetCount;
  }

  public void setDatasetCount(Integer datasetCount) {
    this.datasetCount = datasetCount;
  }

  public void setDatasetCount(List<Dataset> datasets) {
    this.datasetCount = Math.toIntExact(datasets.stream().distinct().count());
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

  public void addAction(String action) {
    String newAction = DarCollectionActions.valueOf(action.toUpperCase()).getValue();
    actions.add(newAction);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

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
