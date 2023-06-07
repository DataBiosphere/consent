package org.broadinstitute.consent.http.models.elastic_search;

import java.util.List;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;

public class DatasetTerm {

  private Integer datasetId;
  private String datasetIdentifier;
  private Integer participantCount;
  private DataUseSummary dataUse;
  private String dataLocation;
  private String url;
  private Integer dacId;
  private Boolean openAccess;
  private List<Integer> approvedUserIds;
  private StudyTerm study;

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetIdentifier() {
    return datasetIdentifier;
  }

  public void setDatasetIdentifier(String datasetIdentifier) {
    this.datasetIdentifier = datasetIdentifier;
  }


  public Integer getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Integer participantCount) {
    this.participantCount = participantCount;
  }


  public DataUseSummary getDataUse() {
    return dataUse;
  }

  public void setDataUse(DataUseSummary dataUse) {
    this.dataUse = dataUse;
  }

  public String getDataLocation() {
    return dataLocation;
  }

  public void setDataLocation(String dataLocation) {
    this.dataLocation = dataLocation;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Integer getDacId() {
    return dacId;
  }

  public void setDacId(Integer dacId) {
    this.dacId = dacId;
  }

  public Boolean getOpenAccess() {
    return openAccess;
  }

  public void setOpenAccess(Boolean openAccess) {
    this.openAccess = openAccess;
  }

  public List<Integer> getApprovedUserIds() {
    return approvedUserIds;
  }

  public void setApprovedUserIds(List<Integer> approvedUsers) {
    this.approvedUserIds = approvedUsers;
  }

  public StudyTerm getStudy() {
    return study;
  }

  public void setStudy(StudyTerm study) {
    this.study = study;
  }
}