package org.broadinstitute.consent.http.models;

public class DarDecisionMetrics {

  private String darId;
  private String dacId;
  private String datasetId;
  private String dateSubmitted;
  private String dateApproved;
  private String dateDenied;
  private String darTurnaroundTime;
  private String dacDecision;
  private String algorithmDecision;
  private String srpDecision;

  public DarDecisionMetrics() {
  }

  public String getDarId() {
    return darId;
  }

  public void setDarId(String darId) {
    this.darId = darId;
  }

  public String getDacId() {
    return dacId;
  }

  public void setDacId(String dacId) {
    this.dacId = dacId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getDateSubmitted() {
    return dateSubmitted;
  }

  public void setDateSubmitted(String dateSubmitted) {
    this.dateSubmitted = dateSubmitted;
  }

  public String getDateApproved() {
    return dateApproved;
  }

  public void setDateApproved(String dateApproved) {
    this.dateApproved = dateApproved;
  }

  public String getDateDenied() {
    return dateDenied;
  }

  public void setDateDenied(String dateDenied) {
    this.dateDenied = dateDenied;
  }

  public String getDarTurnaroundTime() {
    return darTurnaroundTime;
  }

  public void setDarTurnaroundTime(String darTurnaroundTime) {
    this.darTurnaroundTime = darTurnaroundTime;
  }

  public String getDacDecision() {
    return dacDecision;
  }

  public void setDacDecision(String dacDecision) {
    this.dacDecision = dacDecision;
  }

  public String getAlgorithmDecision() {
    return algorithmDecision;
  }

  public void setAlgorithmDecision(String algorithmDecision) {
    this.algorithmDecision = algorithmDecision;
  }

  public String getSrpDecision() {
    return srpDecision;
  }

  public void setSrpDecision(String srpDecision) {
    this.srpDecision = srpDecision;
  }
}
