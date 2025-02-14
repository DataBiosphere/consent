package org.broadinstitute.consent.http.models.elastic_search;

import java.util.List;

public class StudyTerm {

  private String description;
  private String studyName;
  private Integer studyId;
  private String phsId;
  private String phenotype;
  private String species;
  private String piName;
  private String dataSubmitterEmail;
  private Integer dataSubmitterId;
  private List<String> dataCustodianEmail;
  private Boolean publicVisibility;
  private List<String> dataTypes;


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

  public String getPhsId() { return phsId; }

  public void setPhsId(String phsId) { this.phsId = phsId; }

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

  public String getDataSubmitterEmail() {
    return dataSubmitterEmail;
  }

  public void setDataSubmitterEmail(String dataSubmitterEmail) {
    this.dataSubmitterEmail = dataSubmitterEmail;
  }

  public Integer getDataSubmitterId() {
    return dataSubmitterId;
  }

  public void setDataSubmitterId(
      Integer dataSubmitterId) {
    this.dataSubmitterId = dataSubmitterId;
  }

  public List<String> getDataCustodianEmail() {
    return dataCustodianEmail;
  }

  public void setDataCustodianEmail(List<String> dataCustodianEmail) {
    this.dataCustodianEmail = dataCustodianEmail;
  }

  public Boolean getPublicVisibility() {
    return publicVisibility;
  }

  public void setPublicVisibility(Boolean publicVisibility) {
    this.publicVisibility = publicVisibility;
  }

  public List<String> getDataTypes() {
    return dataTypes;
  }

  public void setDataTypes(List<String> dataTypes) {
    this.dataTypes = dataTypes;
  }
}
