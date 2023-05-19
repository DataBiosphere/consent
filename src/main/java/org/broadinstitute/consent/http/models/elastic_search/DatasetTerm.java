package org.broadinstitute.consent.http.models.elastic_search;

import java.util.List;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;

public class DatasetTerm {

  private Integer datasetId;
  private String datasetIdentifier;
  private String description;
  private String studyName;
  private Integer studyId;
  private Integer participantCount;
  private String phenotype;
  private String species;
  private String piName;
  private UserTerm dataSubmitter;
  private String dataCustodian;
  private DataUseSummary dataUse;
  private List<String> dataTypes;
  private String dataLocation;
  private String dacName;
  private Boolean openAccess;
  private Boolean publicVisibility;
  private List<UserTerm> approvedUsers;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStudyName() {
    return studyName;
  }

  public void setStudyName(String studyName) {
    this.studyName = studyName;
  }

  public Integer getStudyId() {
    return studyId;
  }

  public void setStudyId(Integer studyId) {
    this.studyId = studyId;
  }

  public Integer getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Integer participantCount) {
    this.participantCount = participantCount;
  }

  public String getPhenotype() {
    return phenotype;
  }

  public void setPhenotype(String phenotype) {
    this.phenotype = phenotype;
  }

  public String getSpecies() {
    return species;
  }

  public void setSpecies(String species) {
    this.species = species;
  }

  public String getPiName() {
    return piName;
  }

  public void setPiName(String piName) {
    this.piName = piName;
  }

  public UserTerm getDataSubmitter() {
    return dataSubmitter;
  }

  public void setDataSubmitter(UserTerm dataSubmitter) {
    this.dataSubmitter = dataSubmitter;
  }

  public String getDataCustodian() {
    return dataCustodian;
  }

  public void setDataCustodian(String dataCustodian) {
    this.dataCustodian = dataCustodian;
  }

  public DataUseSummary getDataUse() {
    return dataUse;
  }

  public void setDataUse(DataUseSummary dataUse) {
    this.dataUse = dataUse;
  }

  public List<String> getDataTypes() {
    return dataTypes;
  }

  public void setDataTypes(List<String> dataTypes) {
    this.dataTypes = dataTypes;
  }

  public String getDataLocation() {
    return dataLocation;
  }

  public void setDataLocation(String dataLocation) {
    this.dataLocation = dataLocation;
  }

  public String getDacName() {
    return dacName;
  }

  public void setDacName(String dacName) {
    this.dacName = dacName;
  }

  public Boolean getOpenAccess() {
    return openAccess;
  }

  public void setOpenAccess(Boolean openAccess) {
    this.openAccess = openAccess;
  }

  public Boolean getPublicVisibility() {
    return publicVisibility;
  }

  public void setPublicVisibility(Boolean publicVisibility) {
    this.publicVisibility = publicVisibility;
  }

  public List<UserTerm> getApprovedUsers() {
    return approvedUsers;
  }

  public void setApprovedUsers(List<UserTerm> approvedUsers) {
    this.approvedUsers = approvedUsers;
  }
}
