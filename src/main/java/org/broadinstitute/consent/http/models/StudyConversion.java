package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.PropertyType;

public class StudyConversion {

  private String name;
  private String description;
  private List<String> dataTypes;
  private String phenotype;
  private String species;
  private String piName;
  private String dataSubmitterEmail;
  private Boolean publicVisibility;
  private String nihAnvilUse;
  private String datasetName;
  private DataUse dataUse;
  private Integer dacId;
  private String dataLocation;
  private String url;
  private Integer numberOfParticipants;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getDataTypes() {
    return dataTypes;
  }

  public void setDataTypes(List<String> dataTypes) {
    this.dataTypes = dataTypes;
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

  public String getDataSubmitterEmail() {
    return dataSubmitterEmail;
  }

  public void setDataSubmitterEmail(String dataSubmitterEmail) {
    this.dataSubmitterEmail = dataSubmitterEmail;
  }

  public Boolean getPublicVisibility() {
    return publicVisibility;
  }

  public void setPublicVisibility(Boolean publicVisibility) {
    this.publicVisibility = publicVisibility;
  }

  public String getNihAnvilUse() {
    return nihAnvilUse;
  }

  public void setNihAnvilUse(
      String nihAnvilUse) {
    this.nihAnvilUse = nihAnvilUse;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public DataUse getDataUse() {
    return dataUse;
  }

  public void setDataUse(DataUse dataUse) {
    this.dataUse = dataUse;
  }

  public Integer getDacId() {
    return dacId;
  }

  public void setDacId(Integer dacId) {
    this.dacId = dacId;
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

  public Integer getNumberOfParticipants() {
    return numberOfParticipants;
  }

  public void setNumberOfParticipants(Integer numberOfParticipants) {
    this.numberOfParticipants = numberOfParticipants;
  }

  public Study createNewStudyStub() {
    Study study = new Study();
    study.setName(getName());
    study.setDescription(getDescription());
    study.setPublicVisibility(getPublicVisibility());
    study.setPiName(getPiName());
    study.setDataTypes(getDataTypes());
    return study;
  }

  public Collection<StudyProperty> getStudyProperties() {
    List<StudyProperty> props = new ArrayList<>();
    if (getPhenotype() != null) {
      props.add(new StudyProperty("phenotypeIndication", getPhenotype(), PropertyType.String));
    }
    if (getSpecies() != null) {
      props.add(new StudyProperty("species", getSpecies(), PropertyType.String));
    }
    if (getNihAnvilUse() != null) {
      props.add(new StudyProperty("nihAnvilUse", getNihAnvilUse(), PropertyType.String));
    }
    return props;
  }

}
