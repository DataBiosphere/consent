package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataUse {

  private Boolean generalUse;
  private Boolean hmbResearch;
  private List<String> diseaseRestrictions;
  private Boolean populationOriginsAncestry;
  private Boolean methodsResearch;
  private Boolean nonProfitUse;
  private String other;
  private String secondaryOther;
  // Also known as "irb"
  private Boolean ethicsApprovalRequired;
  private Boolean collaboratorRequired;
  private String geographicalRestrictions;
  private Boolean geneticStudiesOnly;
  private Boolean publicationResults;
  private String publicationMoratorium;
  private Boolean controls;
  private String gender;
  private Boolean pediatric;
  private Boolean population;
  private Boolean illegalBehavior;
  private Boolean sexualDiseases;
  private Boolean stigmatizeDiseases;
  private Boolean vulnerablePopulations;
  private Boolean psychologicalTraits;
  private Boolean notHealth;

  @Override
  public String toString() {
    return new GsonBuilder().create().toJson(this);
  }

  public Boolean getGeneralUse() {
    return generalUse;
  }

  public void setGeneralUse(Boolean generalUse) {
    this.generalUse = generalUse;
  }

  public Boolean getHmbResearch() {
    return hmbResearch;
  }

  public void setHmbResearch(Boolean hmbResearch) {
    this.hmbResearch = hmbResearch;
  }

  public List<String> getDiseaseRestrictions() {
    return diseaseRestrictions;
  }

  public void setDiseaseRestrictions(List<String> diseaseRestrictions) {
    this.diseaseRestrictions = diseaseRestrictions;
  }

  public Boolean getPopulationOriginsAncestry() {
    return populationOriginsAncestry;
  }

  public void setPopulationOriginsAncestry(Boolean populationOriginsAncestry) {
    this.populationOriginsAncestry = populationOriginsAncestry;
  }

  public Boolean getMethodsResearch() {
    return methodsResearch;
  }

  public void setMethodsResearch(Boolean methodsResearch) {
    this.methodsResearch = methodsResearch;
  }

  public Boolean getNonProfitUse() {
    return nonProfitUse;
  }

  public void setNonProfitUse(Boolean nonProfitUse) {
    this.nonProfitUse = nonProfitUse;
  }

  public String getOther() {
    return other;
  }

  public void setOther(String other) {
    this.other = other;
  }

  public String getSecondaryOther() {
    return secondaryOther;
  }

  public void setSecondaryOther(String secondaryOther) {
    this.secondaryOther = secondaryOther;
  }

  public Boolean getEthicsApprovalRequired() {
    return ethicsApprovalRequired;
  }

  public void setEthicsApprovalRequired(Boolean ethicsApprovalRequired) {
    this.ethicsApprovalRequired = ethicsApprovalRequired;
  }

  public Boolean getCollaboratorRequired() {
    return collaboratorRequired;
  }

  public void setCollaboratorRequired(Boolean collaboratorRequired) {
    this.collaboratorRequired = collaboratorRequired;
  }

  public String getGeographicalRestrictions() {
    return geographicalRestrictions;
  }

  public void setGeographicalRestrictions(String geographicalRestrictions) {
    this.geographicalRestrictions = geographicalRestrictions;
  }

  public Boolean getGeneticStudiesOnly() {
    return geneticStudiesOnly;
  }

  public void setGeneticStudiesOnly(Boolean geneticStudiesOnly) {
    this.geneticStudiesOnly = geneticStudiesOnly;
  }

  public Boolean getPublicationResults() {
    return publicationResults;
  }

  public void setPublicationResults(Boolean publicationResults) {
    this.publicationResults = publicationResults;
  }

  public String getPublicationMoratorium() {
    return publicationMoratorium;
  }

  public void setPublicationMoratorium(String publicationMoratorium) {
    this.publicationMoratorium = publicationMoratorium;
  }

  public Boolean getControls() {
    return controls;
  }

  public void setControls(Boolean controls) {
    this.controls = controls;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public Boolean getPediatric() {
    return pediatric;
  }

  public void setPediatric(Boolean pediatric) {
    this.pediatric = pediatric;
  }

  public Boolean getPopulation() {
    return population;
  }

  public void setPopulation(Boolean population) {
    this.population = population;
  }

  public Boolean getIllegalBehavior() {
    return illegalBehavior;
  }

  public void setIllegalBehavior(Boolean illegalBehavior) {
    this.illegalBehavior = illegalBehavior;
  }

  public Boolean getSexualDiseases() {
    return sexualDiseases;
  }

  public void setSexualDiseases(Boolean sexualDiseases) {
    this.sexualDiseases = sexualDiseases;
  }

  public Boolean getStigmatizeDiseases() {
    return stigmatizeDiseases;
  }

  public void setStigmatizeDiseases(Boolean stigmatizeDiseases) {
    this.stigmatizeDiseases = stigmatizeDiseases;
  }

  public Boolean getVulnerablePopulations() {
    return vulnerablePopulations;
  }

  public void setVulnerablePopulations(Boolean vulnerablePopulations) {
    this.vulnerablePopulations = vulnerablePopulations;
  }

  public Boolean getPsychologicalTraits() {
    return psychologicalTraits;
  }

  public void setPsychologicalTraits(Boolean psychologicalTraits) {
    this.psychologicalTraits = psychologicalTraits;
  }

  public Boolean getNotHealth() {
    return notHealth;
  }

  public void setNotHealth(Boolean notHealth) {
    this.notHealth = notHealth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DataUse dataUse = (DataUse) o;

    return new EqualsBuilder().append(generalUse, dataUse.generalUse)
        .append(hmbResearch, dataUse.hmbResearch)
        .append(diseaseRestrictions, dataUse.diseaseRestrictions)
        .append(populationOriginsAncestry, dataUse.populationOriginsAncestry)
        .append(methodsResearch, dataUse.methodsResearch).append(nonProfitUse, dataUse.nonProfitUse)
        .append(other, dataUse.other).append(secondaryOther, dataUse.secondaryOther)
        .append(ethicsApprovalRequired, dataUse.ethicsApprovalRequired)
        .append(collaboratorRequired, dataUse.collaboratorRequired)
        .append(geographicalRestrictions, dataUse.geographicalRestrictions)
        .append(geneticStudiesOnly, dataUse.geneticStudiesOnly)
        .append(publicationResults, dataUse.publicationResults)
        .append(publicationMoratorium, dataUse.publicationMoratorium)
        .append(controls, dataUse.controls).append(gender, dataUse.gender)
        .append(pediatric, dataUse.pediatric).append(population, dataUse.population)
        .append(illegalBehavior, dataUse.illegalBehavior)
        .append(sexualDiseases, dataUse.sexualDiseases)
        .append(stigmatizeDiseases, dataUse.stigmatizeDiseases)
        .append(vulnerablePopulations, dataUse.vulnerablePopulations)
        .append(psychologicalTraits, dataUse.psychologicalTraits)
        .append(notHealth, dataUse.notHealth)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(generalUse).append(hmbResearch)
        .append(diseaseRestrictions).append(populationOriginsAncestry).append(methodsResearch)
        .append(nonProfitUse).append(other).append(secondaryOther).append(ethicsApprovalRequired)
        .append(collaboratorRequired).append(geographicalRestrictions).append(geneticStudiesOnly)
        .append(publicationResults).append(publicationMoratorium).append(controls).append(gender)
        .append(pediatric).append(population).append(illegalBehavior).append(sexualDiseases)
        .append(stigmatizeDiseases).append(vulnerablePopulations).append(psychologicalTraits)
        .append(notHealth).toHashCode();
  }
}
