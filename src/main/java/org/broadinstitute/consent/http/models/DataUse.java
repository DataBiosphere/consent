package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unused", "SameParameterValue", "WeakerAccess"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataUse {

  private static final Logger logger = LoggerFactory.getLogger(
      "org.broadinstitute.consent.http.models.DataUse");

  private Boolean generalUse;
  private Boolean hmbResearch;
  private List<String> diseaseRestrictions;
  private Boolean populationOriginsAncestry;
  @Deprecated
  private Boolean populationStructure;
//  private Boolean commercialUse;
  private Boolean nonProfitUse;
  private Boolean methodsResearch;
  @Deprecated
  private String aggregateResearch;
  @Deprecated
  private String controlSetOption;
  private Boolean controls;
  private String gender;
  private Boolean pediatric;
  private Boolean population;
  @Deprecated
  private List<String> populationRestrictions;
  @Deprecated
  private Boolean otherRestrictions;
  private String dateRestriction;
  @Deprecated
  private Boolean recontactingDataSubjects;
  @Deprecated
  private String recontactMay;
  @Deprecated
  private String recontactMust;
  @Deprecated
  private String genomicPhenotypicData;
  @Deprecated
  private String cloudStorage;
  // Also known as "irb"
  private Boolean ethicsApprovalRequired;
  @Deprecated
  private Boolean collaboratorRequired;
  @Deprecated
  private String geographicalRestrictions;
  private String other;
  private String secondaryOther;
  private Boolean illegalBehavior;
  private Boolean addiction;
  private Boolean sexualDiseases;
  private Boolean stigmatizeDiseases;
  private Boolean vulnerablePopulations;
  private Boolean psychologicalTraits;
  private Boolean notHealth;
  @Deprecated
  private Boolean nonBiomedical;
  @Deprecated
  private Boolean manualReview;
  private Boolean geneticStudiesOnly;
  private Boolean publicationResults;
  @Deprecated
  private Boolean genomicResults;
  @Deprecated
  private String genomicSummaryResults;
  @Deprecated
  private Boolean collaborationInvestigators;
  private String publicationMoratorium;

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

  public Boolean getPopulationStructure() {
    return populationStructure;
  }

  public void setPopulationStructure(Boolean populationStructure) {
    this.populationStructure = populationStructure;
  }

//  public Boolean getCommercialUse() {
//    return commercialUse;
//  }
//
//  public void setCommercialUse(Boolean commercialUse) {
//    this.commercialUse = commercialUse;
//  }

  public Boolean getNonProfitUse() {
    return nonProfitUse;
  }

  public void setNonProfitUse(Boolean nonProfitUse) {
    this.nonProfitUse = nonProfitUse;
  }

  public Boolean getMethodsResearch() {
    return methodsResearch;
  }

  public void setMethodsResearch(Boolean methodsResearch) {
    this.methodsResearch = methodsResearch;
  }

  public String getAggregateResearch() {
    return aggregateResearch;
  }

  public void setAggregateResearch(String aggregateResearch) {
    this.aggregateResearch = aggregateResearch;
  }

  public String getControlSetOption() {
    return controlSetOption;
  }

  public void setControlSetOption(String controlSetOption) {
    this.controlSetOption = controlSetOption;
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

  public List<String> getPopulationRestrictions() {
    return populationRestrictions;
  }

  public void setPopulationRestrictions(List<String> populationRestrictions) {
    this.populationRestrictions = populationRestrictions;
  }

  public Boolean getOtherRestrictions() {
    return otherRestrictions;
  }

  public void setOtherRestrictions(Boolean otherRestrictions) {
    this.otherRestrictions = otherRestrictions;
  }

  public String getDateRestriction() {
    return dateRestriction;
  }

  public void setDateRestriction(String dateRestriction) {
    this.dateRestriction = dateRestriction;
  }

  public Boolean getRecontactingDataSubjects() {
    return recontactingDataSubjects;
  }

  public void setRecontactingDataSubjects(Boolean recontactingDataSubjects) {
    this.recontactingDataSubjects = recontactingDataSubjects;
  }

  public String getRecontactMay() {
    return recontactMay;
  }

  public void setRecontactMay(String recontactMay) {
    this.recontactMay = recontactMay;
  }

  public String getRecontactMust() {
    return recontactMust;
  }

  public void setRecontactMust(String recontactMust) {
    this.recontactMust = recontactMust;
  }

  public String getGenomicPhenotypicData() {
    return genomicPhenotypicData;
  }

  public void setGenomicPhenotypicData(String genomicPhenotypicData) {
    this.genomicPhenotypicData = genomicPhenotypicData;
  }

  public String getCloudStorage() {
    return cloudStorage;
  }

  public void setCloudStorage(String cloudStorage) {
    this.cloudStorage = cloudStorage;
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

  public Boolean getIllegalBehavior() {
    return illegalBehavior;
  }

  public void setIllegalBehavior(Boolean illegalBehavior) {
    this.illegalBehavior = illegalBehavior;
  }

  public Boolean getAddiction() {
    return addiction;
  }

  public void setAddiction(Boolean addiction) {
    this.addiction = addiction;
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

  public Boolean getNonBiomedical() {
    return nonBiomedical;
  }

  public void setNonBiomedical(Boolean nonBiomedical) {
    this.nonBiomedical = nonBiomedical;
  }

  public Boolean getManualReview() {
    return manualReview;
  }

  public void setManualReview(Boolean manualReview) {
    this.manualReview = manualReview;
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

  public Boolean getGenomicResults() {
    return genomicResults;
  }

  public void setGenomicResults(Boolean genomicResults) {
    this.genomicResults = genomicResults;
  }

  public String getGenomicSummaryResults() {
    return genomicSummaryResults;
  }

  public void setGenomicSummaryResults(String genomicSummaryResults) {
    this.genomicSummaryResults = genomicSummaryResults;
  }

  public Boolean getCollaborationInvestigators() {
    return collaborationInvestigators;
  }

  public void setCollaborationInvestigators(Boolean collaborationInvestigators) {
    this.collaborationInvestigators = collaborationInvestigators;
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

  public Boolean getPopulation() {
    return population;
  }

  public void setPopulation(Boolean population) {
    this.population = population;
  }

  public Boolean getNotHealth() {
    return notHealth;
  }

  public void setNotHealth(Boolean notHealth) {
    this.notHealth = notHealth;
  }

  @Override
  public String toString() {
    return new GsonBuilder().create().toJson(this);
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
    return Objects.equal(getGeneralUse(), dataUse.getGeneralUse())
        && Objects.equal(getHmbResearch(), dataUse.getHmbResearch())
        && Objects.equal(getDiseaseRestrictions(), dataUse.getDiseaseRestrictions())
        && Objects.equal(getPopulationOriginsAncestry(), dataUse.getPopulationOriginsAncestry())
        && Objects.equal(getPopulationStructure(), dataUse.getPopulationStructure())
        && Objects.equal(getNonProfitUse(), dataUse.getNonProfitUse())
        && Objects.equal(getMethodsResearch(), dataUse.getMethodsResearch())
        && Objects.equal(getAggregateResearch(), dataUse.getAggregateResearch())
        && Objects.equal(getControlSetOption(), dataUse.getControlSetOption())
        && Objects.equal(getGender(), dataUse.getGender())
        && Objects.equal(getPediatric(), dataUse.getPediatric())
        && Objects.equal(getPopulationRestrictions(), dataUse.getPopulationRestrictions())
        && Objects.equal(getOtherRestrictions(), dataUse.getOtherRestrictions())
        && Objects.equal(getDateRestriction(), dataUse.getDateRestriction())
        && Objects.equal(getRecontactingDataSubjects(), dataUse.getRecontactingDataSubjects())
        && Objects.equal(getRecontactMay(), dataUse.getRecontactMay())
        && Objects.equal(getRecontactMust(), dataUse.getRecontactMust())
        && Objects.equal(getGenomicPhenotypicData(), dataUse.getGenomicPhenotypicData())
        && Objects.equal(getCloudStorage(), dataUse.getCloudStorage())
        && Objects.equal(getEthicsApprovalRequired(), dataUse.getEthicsApprovalRequired())
        && Objects.equal(getCollaboratorRequired(), dataUse.getCollaboratorRequired())
        && Objects.equal(getGeographicalRestrictions(), dataUse.getGeographicalRestrictions())
        && Objects.equal(getOther(), dataUse.getOther())
        && Objects.equal(getSecondaryOther(), dataUse.getSecondaryOther())
        && Objects.equal(getIllegalBehavior(), dataUse.getIllegalBehavior())
        && Objects.equal(getAddiction(), dataUse.getAddiction())
        && Objects.equal(getSexualDiseases(), dataUse.getSexualDiseases())
        && Objects.equal(getStigmatizeDiseases(), dataUse.getStigmatizeDiseases())
        && Objects.equal(getVulnerablePopulations(), dataUse.getVulnerablePopulations())
        && Objects.equal(getPsychologicalTraits(), dataUse.getPsychologicalTraits())
        && Objects.equal(getNonBiomedical(), dataUse.getNonBiomedical())
        && Objects.equal(getManualReview(), dataUse.getManualReview())
        && Objects.equal(getGeneticStudiesOnly(), dataUse.getGeneticStudiesOnly())
        && Objects.equal(getPublicationResults(), dataUse.getPublicationResults())
        && Objects.equal(getGenomicResults(), dataUse.getGenomicResults())
        && Objects.equal(getGenomicSummaryResults(), dataUse.getGenomicSummaryResults())
        && Objects.equal(getCollaborationInvestigators(), dataUse.getCollaborationInvestigators())
        && Objects.equal(getPublicationMoratorium(), dataUse.getPublicationMoratorium())
        && Objects.equal(getControls(), dataUse.getControls())
        && Objects.equal(getPopulation(), dataUse.getPopulation())
        && Objects.equal(getNotHealth(), dataUse.getNotHealth());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getGeneralUse(), getHmbResearch(), getDiseaseRestrictions(),
        getPopulationOriginsAncestry(), getPopulationStructure(),
        getMethodsResearch(), getAggregateResearch(), getControlSetOption(), getGender(),
        getPediatric(), getPopulationRestrictions(), getOtherRestrictions(),
        getDateRestriction(),
        getRecontactingDataSubjects(), getRecontactMay(), getRecontactMust(),
        getGenomicPhenotypicData(), getCloudStorage(), getEthicsApprovalRequired(),
        getCollaboratorRequired(), getGeographicalRestrictions(), getOther(),
        getSecondaryOther(),
        getIllegalBehavior(), getAddiction(), getSexualDiseases(), getStigmatizeDiseases(),
        getVulnerablePopulations(), getPsychologicalTraits(), getNonBiomedical(),
        getManualReview(),
        getGeneticStudiesOnly(), getPublicationResults(), getGenomicResults(),
        getGenomicSummaryResults(), getCollaborationInvestigators(),
        getPublicationMoratorium(), getControls(), getPopulation(), getNotHealth());
  }
}
