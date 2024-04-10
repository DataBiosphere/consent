package org.broadinstitute.consent.http.models;

import java.util.List;

/**
 * Syntactic sugar for creating a DataUse object. Copied from
 * org.broadinstitute.dsde.consent.ontology.resources.model.DataUseBuilder in Consent-Ontology
 */
@SuppressWarnings("unused")
public class DataUseBuilder {

  private final DataUse du;

  public DataUseBuilder() {
    du = new DataUse();
  }

  public DataUse build() {
    return du;
  }


  public DataUseBuilder setGeneralUse(Boolean generalUse) {
    du.setGeneralUse(generalUse);
    return this;
  }

  public DataUseBuilder setHmbResearch(Boolean hmbResearch) {
    du.setHmbResearch(hmbResearch);
    return this;
  }

  public DataUseBuilder setDiseaseRestrictions(List<String> diseaseRestrictions) {
    du.setDiseaseRestrictions(diseaseRestrictions);
    return this;
  }

  public DataUseBuilder setPopulationOriginsAncestry(Boolean populationOriginsAncestry) {
    du.setPopulationOriginsAncestry(populationOriginsAncestry);
    return this;
  }

  public DataUseBuilder setMethodsResearch(Boolean methodsResearch) {
    du.setMethodsResearch(methodsResearch);
    return this;
  }

  public DataUseBuilder setNonProfitUse(Boolean nonProfitUse) {
    du.setNonProfitUse(nonProfitUse);
    return this;
  }

  public DataUseBuilder setOther(String other) {
    du.setOther(other);
    return this;
  }

  public DataUseBuilder setSecondaryOther(String secondaryOther) {
    du.setSecondaryOther(secondaryOther);
    return this;
  }

  public DataUseBuilder setEthicsApprovalRequired(Boolean ethicsApprovalRequired) {
    du.setEthicsApprovalRequired(ethicsApprovalRequired);
    return this;
  }

  public DataUseBuilder setCollaboratorRequired(Boolean collaboratorRequired) {
    du.setCollaboratorRequired(collaboratorRequired);
    return this;
  }

  public DataUseBuilder setGeographicalRestrictions(String geographicalRestrictions) {
    du.setGeographicalRestrictions(geographicalRestrictions);
    return this;
  }

  public DataUseBuilder setGeneticStudiesOnly(Boolean geneticStudiesOnly) {
    du.setGeneticStudiesOnly(geneticStudiesOnly);
    return this;
  }

  public DataUseBuilder setPublicationResults(Boolean publicationResults) {
    du.setPublicationResults(publicationResults);
    return this;
  }

  public DataUseBuilder setPublicationMoratorium(String publicationMoratorium) {
    du.setPublicationMoratorium(publicationMoratorium);
    return this;
  }

  public DataUseBuilder setControl(Boolean controls) {
    du.setControls(controls);
    return this;
  }

  public DataUseBuilder setGender(String gender) {
    du.setGender(gender);
    return this;
  }

  public DataUseBuilder setPediatric(Boolean pediatric) {
    du.setPediatric(pediatric);
    return this;
  }

  public DataUseBuilder setPopulation(Boolean population) {
    du.setPopulation(population);
    return this;
  }

  public DataUseBuilder setIllegalBehavior(Boolean illegalBehavior) {
    du.setIllegalBehavior(illegalBehavior);
    return this;
  }

  public DataUseBuilder setSexualDiseases(Boolean sexualDiseases) {
    du.setSexualDiseases(sexualDiseases);
    return this;
  }

  public DataUseBuilder setStigmatizeDiseases(Boolean stigmatizeDiseases) {
    du.setStigmatizeDiseases(stigmatizeDiseases);
    return this;
  }

  public DataUseBuilder setVulnerablePopulations(Boolean vulnerablePopulations) {
    du.setVulnerablePopulations(vulnerablePopulations);
    return this;
  }

  public DataUseBuilder setPsychologicalTraits(Boolean psychologicalTraits) {
    du.setPsychologicalTraits(psychologicalTraits);
    return this;
  }

  public DataUseBuilder setNotHealth(Boolean notHealth) {
    du.setNotHealth(notHealth);
    return this;
  }

}
