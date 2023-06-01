package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import java.sql.Timestamp;
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

  @JsonProperty
  private Integer darCollectionId;

  @JsonProperty
  private Set<String> referenceIds;

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
  private String status;

  @JsonProperty
  private Set<String> actions;

  @JsonProperty
  private int datasetCount;

  private List<String> dacNames;

  private Integer researcherId;
  private Integer institutionId;
  private Set<Integer> datasetIds;
  private List<Vote> votes;
  private Map<Integer, Election> elections;
  private Map<String, String> darStatuses;

  public DarCollectionSummary() {
    this.votes = new ArrayList<>();
    this.actions = new HashSet<>();
    this.elections = new HashMap<>();
    this.datasetIds = new HashSet<>();
    this.referenceIds = new HashSet<>();
    this.darStatuses = new HashMap<>();
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

  public void setReferenceIds(Set<String> referenceIds) {
    this.referenceIds = referenceIds;
  }

  public void addVote(Vote vote) {
    this.votes.add(vote);
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

  public void addAction(String action) {
    String newAction = DarCollectionActions.valueOf(action.toUpperCase()).getValue();
    actions.add(newAction);
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
