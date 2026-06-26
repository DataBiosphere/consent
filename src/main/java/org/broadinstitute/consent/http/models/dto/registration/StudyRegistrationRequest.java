package org.broadinstitute.consent.http.models.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.HashMap;
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
  "externalIdentifier",
  "externalIdentifierType",
  "consentGroups",
  "assets",
  "data"
})
public class StudyRegistrationRequest {

  @JsonProperty("studyName")
  private String studyName;

  @JsonProperty("studyType")
  private DatasetRegistrationSchemaV1.StudyType studyType;

  @JsonProperty("studyDescription")
  private String studyDescription;

  @JsonProperty("dataTypes")
  private List<String> dataTypes = new ArrayList<>();

  @JsonProperty("phenotypeIndication")
  private String phenotypeIndication;

  @JsonProperty("species")
  private String species;

  @JsonProperty("piName")
  private String piName;

  @JsonProperty("piEmail")
  private String piEmail;

  @JsonProperty("dataCustodianEmail")
  private List<String> dataCustodianEmail = new ArrayList<>();

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
  private List<NihICsSupportingStudy> nihICsSupportingStudy = new ArrayList<>();

  @JsonProperty("nihProgramOfficerName")
  private String nihProgramOfficerName;

  @JsonProperty("nihInstitutionCenterSubmission")
  private DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission nihInstitutionCenterSubmission;

  @JsonProperty("nihGenomicProgramAdministratorName")
  private String nihGenomicProgramAdministratorName;

  @JsonProperty("multiCenterStudy")
  private Boolean multiCenterStudy;

  @JsonProperty("collaboratingSites")
  private List<String> collaboratingSites = new ArrayList<>();

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSR")
  private Boolean controlledAccessRequiredForGenomicSummaryResultsGSR;

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation")
  private String controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;

  @JsonProperty("alternativeDataSharingPlan")
  private Boolean alternativeDataSharingPlan;

  @JsonProperty("alternativeDataSharingPlanReasons")
  private List<AlternativeDataSharingPlanReason> alternativeDataSharingPlanReasons =
      new ArrayList<>();

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

  @JsonProperty("externalIdentifier")
  private String externalIdentifier;

  @JsonProperty("externalIdentifierType")
  private String externalIdentifierType;

  @JsonProperty("consentGroups")
  private List<ConsentGroupRequest> consentGroups = new ArrayList<>();

  @JsonProperty("assets")
  private Map<String, Object> assets = new HashMap<>();

  @JsonProperty("data")
  private Map<String, Object> data = new HashMap<>();

  @JsonProperty("studyName")
  public String getStudyName() {
    return studyName;
  }

  @JsonProperty("studyName")
  public void setStudyName(String studyName) {
    this.studyName = studyName;
  }

  @JsonProperty("studyType")
  public DatasetRegistrationSchemaV1.StudyType getStudyType() {
    return studyType;
  }

  @JsonProperty("studyType")
  public void setStudyType(DatasetRegistrationSchemaV1.StudyType studyType) {
    this.studyType = studyType;
  }

  @JsonProperty("studyDescription")
  public String getStudyDescription() {
    return studyDescription;
  }

  @JsonProperty("studyDescription")
  public void setStudyDescription(String studyDescription) {
    this.studyDescription = studyDescription;
  }

  @JsonProperty("dataTypes")
  public List<String> getDataTypes() {
    return dataTypes;
  }

  @JsonProperty("dataTypes")
  public void setDataTypes(List<String> dataTypes) {
    this.dataTypes = dataTypes;
  }

  @JsonProperty("phenotypeIndication")
  public String getPhenotypeIndication() {
    return phenotypeIndication;
  }

  @JsonProperty("phenotypeIndication")
  public void setPhenotypeIndication(String phenotypeIndication) {
    this.phenotypeIndication = phenotypeIndication;
  }

  @JsonProperty("species")
  public String getSpecies() {
    return species;
  }

  @JsonProperty("species")
  public void setSpecies(String species) {
    this.species = species;
  }

  @JsonProperty("piName")
  public String getPiName() {
    return piName;
  }

  @JsonProperty("piName")
  public void setPiName(String piName) {
    this.piName = piName;
  }

  @JsonProperty("piEmail")
  public String getPiEmail() {
    return piEmail;
  }

  @JsonProperty("piEmail")
  public void setPiEmail(String piEmail) {
    this.piEmail = piEmail;
  }

  @JsonProperty("dataCustodianEmail")
  public List<String> getDataCustodianEmail() {
    return dataCustodianEmail;
  }

  @JsonProperty("dataCustodianEmail")
  public void setDataCustodianEmail(List<String> dataCustodianEmail) {
    this.dataCustodianEmail = dataCustodianEmail;
  }

  @JsonProperty("publicVisibility")
  public Boolean getPublicVisibility() {
    return publicVisibility;
  }

  @JsonProperty("publicVisibility")
  public void setPublicVisibility(Boolean publicVisibility) {
    this.publicVisibility = publicVisibility;
  }

  @JsonProperty("throughBioId")
  public String getThroughBioId() {
    return throughBioId;
  }

  @JsonProperty("throughBioId")
  public void setThroughBioId(String throughBioId) {
    this.throughBioId = throughBioId;
  }

  @JsonProperty("nihAnvilUse")
  public DatasetRegistrationSchemaV1.NihAnvilUse getNihAnvilUse() {
    return nihAnvilUse;
  }

  @JsonProperty("nihAnvilUse")
  public void setNihAnvilUse(DatasetRegistrationSchemaV1.NihAnvilUse nihAnvilUse) {
    this.nihAnvilUse = nihAnvilUse;
  }

  @JsonProperty("submittingToAnvil")
  public Boolean getSubmittingToAnvil() {
    return submittingToAnvil;
  }

  @JsonProperty("submittingToAnvil")
  public void setSubmittingToAnvil(Boolean submittingToAnvil) {
    this.submittingToAnvil = submittingToAnvil;
  }

  @JsonProperty("dbGaPPhsID")
  public String getDbGaPPhsID() {
    return dbGaPPhsID;
  }

  @JsonProperty("dbGaPPhsID")
  public void setDbGaPPhsID(String dbGaPPhsID) {
    this.dbGaPPhsID = dbGaPPhsID;
  }

  @JsonProperty("dbGaPStudyRegistrationName")
  public String getDbGaPStudyRegistrationName() {
    return dbGaPStudyRegistrationName;
  }

  @JsonProperty("dbGaPStudyRegistrationName")
  public void setDbGaPStudyRegistrationName(String dbGaPStudyRegistrationName) {
    this.dbGaPStudyRegistrationName = dbGaPStudyRegistrationName;
  }

  @JsonProperty("embargoReleaseDate")
  public String getEmbargoReleaseDate() {
    return embargoReleaseDate;
  }

  @JsonProperty("embargoReleaseDate")
  public void setEmbargoReleaseDate(String embargoReleaseDate) {
    this.embargoReleaseDate = embargoReleaseDate;
  }

  @JsonProperty("sequencingCenter")
  public String getSequencingCenter() {
    return sequencingCenter;
  }

  @JsonProperty("sequencingCenter")
  public void setSequencingCenter(String sequencingCenter) {
    this.sequencingCenter = sequencingCenter;
  }

  @JsonProperty("piInstitution")
  public Integer getPiInstitution() {
    return piInstitution;
  }

  @JsonProperty("piInstitution")
  public void setPiInstitution(Integer piInstitution) {
    this.piInstitution = piInstitution;
  }

  @JsonProperty("nihGrantContractNumber")
  public String getNihGrantContractNumber() {
    return nihGrantContractNumber;
  }

  @JsonProperty("nihGrantContractNumber")
  public void setNihGrantContractNumber(String nihGrantContractNumber) {
    this.nihGrantContractNumber = nihGrantContractNumber;
  }

  @JsonProperty("nihICsSupportingStudy")
  public List<NihICsSupportingStudy> getNihICsSupportingStudy() {
    return nihICsSupportingStudy;
  }

  @JsonProperty("nihICsSupportingStudy")
  public void setNihICsSupportingStudy(List<NihICsSupportingStudy> nihICsSupportingStudy) {
    this.nihICsSupportingStudy = nihICsSupportingStudy;
  }

  @JsonProperty("nihProgramOfficerName")
  public String getNihProgramOfficerName() {
    return nihProgramOfficerName;
  }

  @JsonProperty("nihProgramOfficerName")
  public void setNihProgramOfficerName(String nihProgramOfficerName) {
    this.nihProgramOfficerName = nihProgramOfficerName;
  }

  @JsonProperty("nihInstitutionCenterSubmission")
  public DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission
      getNihInstitutionCenterSubmission() {
    return nihInstitutionCenterSubmission;
  }

  @JsonProperty("nihInstitutionCenterSubmission")
  public void setNihInstitutionCenterSubmission(
      DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission nihInstitutionCenterSubmission) {
    this.nihInstitutionCenterSubmission = nihInstitutionCenterSubmission;
  }

  @JsonProperty("nihGenomicProgramAdministratorName")
  public String getNihGenomicProgramAdministratorName() {
    return nihGenomicProgramAdministratorName;
  }

  @JsonProperty("nihGenomicProgramAdministratorName")
  public void setNihGenomicProgramAdministratorName(String nihGenomicProgramAdministratorName) {
    this.nihGenomicProgramAdministratorName = nihGenomicProgramAdministratorName;
  }

  @JsonProperty("multiCenterStudy")
  public Boolean getMultiCenterStudy() {
    return multiCenterStudy;
  }

  @JsonProperty("multiCenterStudy")
  public void setMultiCenterStudy(Boolean multiCenterStudy) {
    this.multiCenterStudy = multiCenterStudy;
  }

  @JsonProperty("collaboratingSites")
  public List<String> getCollaboratingSites() {
    return collaboratingSites;
  }

  @JsonProperty("collaboratingSites")
  public void setCollaboratingSites(List<String> collaboratingSites) {
    this.collaboratingSites = collaboratingSites;
  }

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSR")
  public Boolean getControlledAccessRequiredForGenomicSummaryResultsGSR() {
    return controlledAccessRequiredForGenomicSummaryResultsGSR;
  }

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSR")
  public void setControlledAccessRequiredForGenomicSummaryResultsGSR(
      Boolean controlledAccessRequiredForGenomicSummaryResultsGSR) {
    this.controlledAccessRequiredForGenomicSummaryResultsGSR =
        controlledAccessRequiredForGenomicSummaryResultsGSR;
  }

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation")
  public String getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation() {
    return controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;
  }

  @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation")
  public void setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation(
      String controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation) {
    this.controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation =
        controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation;
  }

  @JsonProperty("alternativeDataSharingPlan")
  public Boolean getAlternativeDataSharingPlan() {
    return alternativeDataSharingPlan;
  }

  @JsonProperty("alternativeDataSharingPlan")
  public void setAlternativeDataSharingPlan(Boolean alternativeDataSharingPlan) {
    this.alternativeDataSharingPlan = alternativeDataSharingPlan;
  }

  @JsonProperty("alternativeDataSharingPlanReasons")
  public List<AlternativeDataSharingPlanReason> getAlternativeDataSharingPlanReasons() {
    return alternativeDataSharingPlanReasons;
  }

  @JsonProperty("alternativeDataSharingPlanReasons")
  public void setAlternativeDataSharingPlanReasons(
      List<AlternativeDataSharingPlanReason> alternativeDataSharingPlanReasons) {
    this.alternativeDataSharingPlanReasons = alternativeDataSharingPlanReasons;
  }

  @JsonProperty("alternativeDataSharingPlanExplanation")
  public String getAlternativeDataSharingPlanExplanation() {
    return alternativeDataSharingPlanExplanation;
  }

  @JsonProperty("alternativeDataSharingPlanExplanation")
  public void setAlternativeDataSharingPlanExplanation(
      String alternativeDataSharingPlanExplanation) {
    this.alternativeDataSharingPlanExplanation = alternativeDataSharingPlanExplanation;
  }

  @JsonProperty("alternativeDataSharingPlanFileName")
  public String getAlternativeDataSharingPlanFileName() {
    return alternativeDataSharingPlanFileName;
  }

  @JsonProperty("alternativeDataSharingPlanFileName")
  public void setAlternativeDataSharingPlanFileName(String alternativeDataSharingPlanFileName) {
    this.alternativeDataSharingPlanFileName = alternativeDataSharingPlanFileName;
  }

  @JsonProperty("alternativeDataSharingPlanDataSubmitted")
  public DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted
      getAlternativeDataSharingPlanDataSubmitted() {
    return alternativeDataSharingPlanDataSubmitted;
  }

  @JsonProperty("alternativeDataSharingPlanDataSubmitted")
  public void setAlternativeDataSharingPlanDataSubmitted(
      DatasetRegistrationSchemaV1.AlternativeDataSharingPlanDataSubmitted
          alternativeDataSharingPlanDataSubmitted) {
    this.alternativeDataSharingPlanDataSubmitted = alternativeDataSharingPlanDataSubmitted;
  }

  @JsonProperty("alternativeDataSharingPlanDataReleased")
  public Boolean getAlternativeDataSharingPlanDataReleased() {
    return alternativeDataSharingPlanDataReleased;
  }

  @JsonProperty("alternativeDataSharingPlanDataReleased")
  public void setAlternativeDataSharingPlanDataReleased(
      Boolean alternativeDataSharingPlanDataReleased) {
    this.alternativeDataSharingPlanDataReleased = alternativeDataSharingPlanDataReleased;
  }

  @JsonProperty("alternativeDataSharingPlanTargetDeliveryDate")
  public String getAlternativeDataSharingPlanTargetDeliveryDate() {
    return alternativeDataSharingPlanTargetDeliveryDate;
  }

  @JsonProperty("alternativeDataSharingPlanTargetDeliveryDate")
  public void setAlternativeDataSharingPlanTargetDeliveryDate(
      String alternativeDataSharingPlanTargetDeliveryDate) {
    this.alternativeDataSharingPlanTargetDeliveryDate =
        alternativeDataSharingPlanTargetDeliveryDate;
  }

  @JsonProperty("alternativeDataSharingPlanTargetPublicReleaseDate")
  public String getAlternativeDataSharingPlanTargetPublicReleaseDate() {
    return alternativeDataSharingPlanTargetPublicReleaseDate;
  }

  @JsonProperty("alternativeDataSharingPlanTargetPublicReleaseDate")
  public void setAlternativeDataSharingPlanTargetPublicReleaseDate(
      String alternativeDataSharingPlanTargetPublicReleaseDate) {
    this.alternativeDataSharingPlanTargetPublicReleaseDate =
        alternativeDataSharingPlanTargetPublicReleaseDate;
  }

  @JsonProperty("alternativeDataSharingPlanAccessManagement")
  public DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement
      getAlternativeDataSharingPlanAccessManagement() {
    return alternativeDataSharingPlanAccessManagement;
  }

  @JsonProperty("alternativeDataSharingPlanAccessManagement")
  public void setAlternativeDataSharingPlanAccessManagement(
      DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement
          alternativeDataSharingPlanAccessManagement) {
    this.alternativeDataSharingPlanAccessManagement = alternativeDataSharingPlanAccessManagement;
  }

  @JsonProperty("externalIdentifier")
  public String getExternalIdentifier() {
    return externalIdentifier;
  }

  @JsonProperty("externalIdentifier")
  public void setExternalIdentifier(String externalIdentifier) {
    this.externalIdentifier = externalIdentifier;
  }

  @JsonProperty("externalIdentifierType")
  public String getExternalIdentifierType() {
    return externalIdentifierType;
  }

  @JsonProperty("externalIdentifierType")
  public void setExternalIdentifierType(String externalIdentifierType) {
    this.externalIdentifierType = externalIdentifierType;
  }

  @JsonProperty("consentGroups")
  public List<ConsentGroupRequest> getConsentGroups() {
    return consentGroups;
  }

  @JsonProperty("consentGroups")
  public void setConsentGroups(List<ConsentGroupRequest> consentGroups) {
    this.consentGroups = consentGroups;
  }

  @JsonProperty("assets")
  public Map<String, Object> getAssets() {
    return assets;
  }

  @JsonProperty("assets")
  public void setAssets(Map<String, Object> assets) {
    this.assets = assets;
  }

  @JsonProperty("data")
  public Map<String, Object> getData() {
    return data;
  }

  @JsonProperty("data")
  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
