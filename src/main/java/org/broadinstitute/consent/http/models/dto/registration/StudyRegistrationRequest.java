package org.broadinstitute.consent.http.models.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "studyName",
  "studyType",
  "studyDescription",
  "dataTypes",
  "phenotypeIndication",
  "species",
  "piName",
  "piEmail",
  "dataCustodianEmail",
  "publicVisibility",
  "throughBioId",
  "nihAnvilUse",
  "submittingToAnvil",
  "dbGaPPhsID",
  "dbGaPStudyRegistrationName",
  "embargoReleaseDate",
  "sequencingCenter",
  "piInstitution",
  "nihGrantContractNumber",
  "nihICsSupportingStudy",
  "nihProgramOfficerName",
  "nihInstitutionCenterSubmission",
  "nihGenomicProgramAdministratorName",
  "multiCenterStudy",
  "collaboratingSites",
  "controlledAccessRequiredForGenomicSummaryResultsGSR",
  "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation",
  "alternativeDataSharingPlan",
  "alternativeDataSharingPlanReasons",
  "alternativeDataSharingPlanExplanation",
  "alternativeDataSharingPlanFileName",
  "alternativeDataSharingPlanDataSubmitted",
  "alternativeDataSharingPlanDataReleased",
  "alternativeDataSharingPlanTargetDeliveryDate",
  "alternativeDataSharingPlanTargetPublicReleaseDate",
  "alternativeDataSharingPlanAccessManagement",
  "consentGroups",
  "models",
  "workspaces",
  "presentations",
  "publications",
  "clinicalTrials",
  "intellectualProperties",
  "biospecimens",
  "funding",
  "assets",
  "data",
  "externalIdentifier",
  "externalIdentifierType"
})
public class StudyRegistrationRequest {

  @JsonProperty("studyName")
  private String studyName;

  @JsonProperty("studyType")
  private DatasetRegistrationSchemaV1.StudyType studyType;

  @JsonProperty("studyDescription")
  private String studyDescription;

  @JsonProperty("dataTypes")
  private List<String> dataTypes;

  @JsonProperty("phenotypeIndication")
  private String phenotypeIndication;

  @JsonProperty("species")
  private String species;

  @JsonProperty("piName")
  private String piName;

  @JsonProperty("piEmail")
  private String piEmail;

  @JsonProperty("dataCustodianEmail")
  private List<String> dataCustodianEmail;

  @JsonProperty("publicVisibility")
  private Boolean publicVisibility;

  @JsonProperty("throughBioId")
  private String throughBioId;

  @JsonProperty("nihAnvilUse")
  private DatasetRegistrationSchemaV1.NihAnvilUse nihAnvilUse;

  @JsonProperty("submittingToAnvil")
  private Boolean submittingToAnvil;

  @JsonProperty("dbGaPPhsID")
  private String dbGaPPhsID;

  @JsonProperty("dbGaPStudyRegistrationName")
  private String dbGaPStudyRegistrationName;

  @JsonProperty("embargoReleaseDate")
  private String embargoReleaseDate;

  @JsonProperty("sequencingCenter")
  private String sequencingCenter;

  @JsonProperty("piInstitution")
  private Integer piInstitution;

  @JsonProperty("nihGrantContractNumber")
  private String nihGrantContractNumber;

  @JsonProperty("nihICsSupportingStudy")
  private List<NihICsSupportingStudy> nihICsSupportingStudy;

  @JsonProperty("nihProgramOfficerName")
  private String nihProgramOfficerName;

  @JsonProperty("nihInstitutionCenterSubmission")
  private DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission nihInstitutionCenterSubmission;

  @JsonProperty("nihGenomicProgramAdministratorName")
  private String nihGenomicProgramAdministratorName;

  @JsonProperty("multiCenterStudy")
  private Boolean multiCenterStudy;

  @JsonProperty("collaboratingSites")
  private List<String> collaboratingSites;

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSR")
  private Boolean controlledAccessRequiredForGenomicSummaryResultsGSR;

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation")
  private String controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;

  @JsonProperty("alternativeDataSharingPlan")
  private Boolean alternativeDataSharingPlan;

  @JsonProperty("alternativeDataSharingPlanReasons")
  private List<AlternativeDataSharingPlanReason> alternativeDataSharingPlanReasons;

  @JsonProperty("alternativeDataSharingPlanExplanation")
  private String alternativeDataSharingPlanExplanation;

  @JsonProperty("alternativeDataSharingPlanFileName")
  private String alternativeDataSharingPlanFileName;

  @JsonProperty("alternativeDataSharingPlanDataSubmitted")
  private DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted
      alternativeDataSharingPlanDataSubmitted;

  @JsonProperty("alternativeDataSharingPlanDataReleased")
  private Boolean alternativeDataSharingPlanDataReleased;

  @JsonProperty("alternativeDataSharingPlanTargetDeliveryDate")
  private String alternativeDataSharingPlanTargetDeliveryDate;

  @JsonProperty("alternativeDataSharingPlanTargetPublicReleaseDate")
  private String alternativeDataSharingPlanTargetPublicReleaseDate;

  @JsonProperty("alternativeDataSharingPlanAccessManagement")
  private DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement
      alternativeDataSharingPlanAccessManagement;

  @JsonProperty("consentGroups")
  private List<ConsentGroupRequest> consentGroups;

  @JsonProperty("models")
  private List<Object> models;

  @JsonProperty("workspaces")
  private List<Object> workspaces;

  @JsonProperty("presentations")
  private List<Object> presentations;

  @JsonProperty("publications")
  private List<Object> publications;

  @JsonProperty("clinicalTrials")
  private List<Object> clinicalTrials;

  @JsonProperty("intellectualProperties")
  private List<Object> intellectualProperties;

  @JsonProperty("biospecimens")
  private List<Object> biospecimens;

  @JsonProperty("funding")
  private List<Object> funding;

  @JsonProperty("assets")
  private Map<String, Object> assets;

  @JsonProperty("data")
  private Map<String, Object> data;

  @JsonProperty("externalIdentifier")
  private String externalIdentifier;

  @JsonProperty("externalIdentifierType")
  private String externalIdentifierType;

  public String getStudyName() {
    return studyName;
  }

  public void setStudyName(String studyName) {
    this.studyName = studyName;
  }

  public DatasetRegistrationSchemaV1.StudyType getStudyType() {
    return studyType;
  }

  public void setStudyType(DatasetRegistrationSchemaV1.StudyType studyType) {
    this.studyType = studyType;
  }

  public String getStudyDescription() {
    return studyDescription;
  }

  public void setStudyDescription(String studyDescription) {
    this.studyDescription = studyDescription;
  }

  public List<String> getDataTypes() {
    return dataTypes;
  }

  public void setDataTypes(List<String> dataTypes) {
    this.dataTypes = dataTypes;
  }

  public String getPhenotypeIndication() {
    return phenotypeIndication;
  }

  public void setPhenotypeIndication(String phenotypeIndication) {
    this.phenotypeIndication = phenotypeIndication;
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

  public String getPiEmail() {
    return piEmail;
  }

  public void setPiEmail(String piEmail) {
    this.piEmail = piEmail;
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

  public String getThroughBioId() {
    return throughBioId;
  }

  public void setThroughBioId(String throughBioId) {
    this.throughBioId = throughBioId;
  }

  public DatasetRegistrationSchemaV1.NihAnvilUse getNihAnvilUse() {
    return nihAnvilUse;
  }

  public void setNihAnvilUse(DatasetRegistrationSchemaV1.NihAnvilUse nihAnvilUse) {
    this.nihAnvilUse = nihAnvilUse;
  }

  public Boolean getSubmittingToAnvil() {
    return submittingToAnvil;
  }

  public void setSubmittingToAnvil(Boolean submittingToAnvil) {
    this.submittingToAnvil = submittingToAnvil;
  }

  public String getDbGaPPhsID() {
    return dbGaPPhsID;
  }

  public void setDbGaPPhsID(String dbGaPPhsID) {
    this.dbGaPPhsID = dbGaPPhsID;
  }

  public String getDbGaPStudyRegistrationName() {
    return dbGaPStudyRegistrationName;
  }

  public void setDbGaPStudyRegistrationName(String dbGaPStudyRegistrationName) {
    this.dbGaPStudyRegistrationName = dbGaPStudyRegistrationName;
  }

  public String getEmbargoReleaseDate() {
    return embargoReleaseDate;
  }

  public void setEmbargoReleaseDate(String embargoReleaseDate) {
    this.embargoReleaseDate = embargoReleaseDate;
  }

  public String getSequencingCenter() {
    return sequencingCenter;
  }

  public void setSequencingCenter(String sequencingCenter) {
    this.sequencingCenter = sequencingCenter;
  }

  public Integer getPiInstitution() {
    return piInstitution;
  }

  public void setPiInstitution(Integer piInstitution) {
    this.piInstitution = piInstitution;
  }

  public String getNihGrantContractNumber() {
    return nihGrantContractNumber;
  }

  public void setNihGrantContractNumber(String nihGrantContractNumber) {
    this.nihGrantContractNumber = nihGrantContractNumber;
  }

  public List<NihICsSupportingStudy> getNihICsSupportingStudy() {
    return nihICsSupportingStudy;
  }

  public void setNihICsSupportingStudy(List<NihICsSupportingStudy> nihICsSupportingStudy) {
    this.nihICsSupportingStudy = nihICsSupportingStudy;
  }

  public String getNihProgramOfficerName() {
    return nihProgramOfficerName;
  }

  public void setNihProgramOfficerName(String nihProgramOfficerName) {
    this.nihProgramOfficerName = nihProgramOfficerName;
  }

  public DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission
      getNihInstitutionCenterSubmission() {
    return nihInstitutionCenterSubmission;
  }

  public void setNihInstitutionCenterSubmission(
      DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission nihInstitutionCenterSubmission) {
    this.nihInstitutionCenterSubmission = nihInstitutionCenterSubmission;
  }

  public String getNihGenomicProgramAdministratorName() {
    return nihGenomicProgramAdministratorName;
  }

  public void setNihGenomicProgramAdministratorName(String nihGenomicProgramAdministratorName) {
    this.nihGenomicProgramAdministratorName = nihGenomicProgramAdministratorName;
  }

  public Boolean getMultiCenterStudy() {
    return multiCenterStudy;
  }

  public void setMultiCenterStudy(Boolean multiCenterStudy) {
    this.multiCenterStudy = multiCenterStudy;
  }

  public List<String> getCollaboratingSites() {
    return collaboratingSites;
  }

  public void setCollaboratingSites(List<String> collaboratingSites) {
    this.collaboratingSites = collaboratingSites;
  }

  public Boolean getControlledAccessRequiredForGenomicSummaryResultsGSR() {
    return controlledAccessRequiredForGenomicSummaryResultsGSR;
  }

  public void setControlledAccessRequiredForGenomicSummaryResultsGSR(
      Boolean controlledAccessRequiredForGenomicSummaryResultsGSR) {
    this.controlledAccessRequiredForGenomicSummaryResultsGSR =
        controlledAccessRequiredForGenomicSummaryResultsGSR;
  }

  public String getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation() {
    return controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;
  }

  public void setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(
      String controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation) {
    this.controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation =
        controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;
  }

  public Boolean getAlternativeDataSharingPlan() {
    return alternativeDataSharingPlan;
  }

  public void setAlternativeDataSharingPlan(Boolean alternativeDataSharingPlan) {
    this.alternativeDataSharingPlan = alternativeDataSharingPlan;
  }

  public List<AlternativeDataSharingPlanReason> getAlternativeDataSharingPlanReasons() {
    return alternativeDataSharingPlanReasons;
  }

  public void setAlternativeDataSharingPlanReasons(
      List<AlternativeDataSharingPlanReason> alternativeDataSharingPlanReasons) {
    this.alternativeDataSharingPlanReasons = alternativeDataSharingPlanReasons;
  }

  public String getAlternativeDataSharingPlanExplanation() {
    return alternativeDataSharingPlanExplanation;
  }

  public void setAlternativeDataSharingPlanExplanation(
      String alternativeDataSharingPlanExplanation) {
    this.alternativeDataSharingPlanExplanation = alternativeDataSharingPlanExplanation;
  }

  public String getAlternativeDataSharingPlanFileName() {
    return alternativeDataSharingPlanFileName;
  }

  public void setAlternativeDataSharingPlanFileName(String alternativeDataSharingPlanFileName) {
    this.alternativeDataSharingPlanFileName = alternativeDataSharingPlanFileName;
  }

  public DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted
      getAlternativeDataSharingPlanDataSubmitted() {
    return alternativeDataSharingPlanDataSubmitted;
  }

  public void setAlternativeDataSharingPlanDataSubmitted(
      DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted
          alternativeDataSharingPlanDataSubmitted) {
    this.alternativeDataSharingPlanDataSubmitted = alternativeDataSharingPlanDataSubmitted;
  }

  public Boolean getAlternativeDataSharingPlanDataReleased() {
    return alternativeDataSharingPlanDataReleased;
  }

  public void setAlternativeDataSharingPlanDataReleased(
      Boolean alternativeDataSharingPlanDataReleased) {
    this.alternativeDataSharingPlanDataReleased = alternativeDataSharingPlanDataReleased;
  }

  public String getAlternativeDataSharingPlanTargetDeliveryDate() {
    return alternativeDataSharingPlanTargetDeliveryDate;
  }

  public void setAlternativeDataSharingPlanTargetDeliveryDate(
      String alternativeDataSharingPlanTargetDeliveryDate) {
    this.alternativeDataSharingPlanTargetDeliveryDate =
        alternativeDataSharingPlanTargetDeliveryDate;
  }

  public String getAlternativeDataSharingPlanTargetPublicReleaseDate() {
    return alternativeDataSharingPlanTargetPublicReleaseDate;
  }

  public void setAlternativeDataSharingPlanTargetPublicReleaseDate(
      String alternativeDataSharingPlanTargetPublicReleaseDate) {
    this.alternativeDataSharingPlanTargetPublicReleaseDate =
        alternativeDataSharingPlanTargetPublicReleaseDate;
  }

  public DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement
      getAlternativeDataSharingPlanAccessManagement() {
    return alternativeDataSharingPlanAccessManagement;
  }

  public void setAlternativeDataSharingPlanAccessManagement(
      DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement
          alternativeDataSharingPlanAccessManagement) {
    this.alternativeDataSharingPlanAccessManagement = alternativeDataSharingPlanAccessManagement;
  }

  public List<ConsentGroupRequest> getConsentGroups() {
    return consentGroups;
  }

  public void setConsentGroups(List<ConsentGroupRequest> consentGroups) {
    this.consentGroups = consentGroups;
  }

  public List<Object> getModels() {
    return models;
  }

  public void setModels(List<Object> models) {
    this.models = models;
  }

  public List<Object> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<Object> workspaces) {
    this.workspaces = workspaces;
  }

  public List<Object> getPresentations() {
    return presentations;
  }

  public void setPresentations(List<Object> presentations) {
    this.presentations = presentations;
  }

  public List<Object> getPublications() {
    return publications;
  }

  public void setPublications(List<Object> publications) {
    this.publications = publications;
  }

  public List<Object> getClinicalTrials() {
    return clinicalTrials;
  }

  public void setClinicalTrials(List<Object> clinicalTrials) {
    this.clinicalTrials = clinicalTrials;
  }

  public List<Object> getIntellectualProperties() {
    return intellectualProperties;
  }

  public void setIntellectualProperties(List<Object> intellectualProperties) {
    this.intellectualProperties = intellectualProperties;
  }

  public List<Object> getBiospecimens() {
    return biospecimens;
  }

  public void setBiospecimens(List<Object> biospecimens) {
    this.biospecimens = biospecimens;
  }

  public List<Object> getFunding() {
    return funding;
  }

  public void setFunding(List<Object> funding) {
    this.funding = funding;
  }

  public Map<String, Object> getAssets() {
    return assets;
  }

  public void setAssets(Map<String, Object> assets) {
    this.assets = assets;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  public String getExternalIdentifier() {
    return externalIdentifier;
  }

  public void setExternalIdentifier(String externalIdentifier) {
    this.externalIdentifier = externalIdentifier;
  }

  public String getExternalIdentifierType() {
    return externalIdentifierType;
  }

  public void setExternalIdentifierType(String externalIdentifierType) {
    this.externalIdentifierType = externalIdentifierType;
  }
}
