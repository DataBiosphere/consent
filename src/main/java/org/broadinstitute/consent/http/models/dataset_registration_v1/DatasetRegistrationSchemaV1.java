package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Dataset Registration Schema
 * <p>
 * Dynamically generated java class from jsonschema2pojo
 * <p>
 * See: https://github.com/joelittlejohn/jsonschema2pojo
 * <code>jsonschema2pojo --source src/main/resources/dataset-registration-schema_v1.json --target java-gen</code>
 * <p>
 * Also see https://jsonschemalint.com/#/version/draft-04/markup/json for validating json.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "studyName",
    "studyType",
    "studyDescription",
    "dataTypes",
    "fileTypes",
    "phenotypeIndication",
    "species",
    "piName",
    "dataSubmitterUserId",
    "dataCustodianEmail",
    "publicVisibility",
    "nihAnvilUse",
    "submittingToAnvil",
    "dbGaPPhsID",
    "dbGaPStudyRegistrationName",
    "embargoReleaseDate",
    "sequencingCenter",
    "piEmail",
    "piInstitution",
    "nihGrantContractNumber",
    "nihICsSupportingStudy",
    "nihProgramOfficerName",
    "nihInstitutionCenterSubmission",
    "nihGenomicProgramAdministratorName",
    "multiCenterStudy",
    "collaboratingSites",
    "controlledAccessRequiredForGenomicSummaryResultsGSR",
    "controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation",
    "alternativeDataSharingPlan",
    "alternativeDataSharingPlanReasons",
    "alternativeDataSharingPlanExplanation",
    "alternativeDataSharingPlanFileName",
    "alternativeDataSharingPlanDataSubmitted",
    "alternativeDataSharingPlanDataReleased",
    "alternativeDataSharingPlanTargetDeliveryDate",
    "alternativeDataSharingPlanTargetPublicReleaseDate",
    "alternativeDataSharingPlanControlledOpenAccess",
    "dataAccessCommitteeId",
    "consentGroups"
})
@Generated("jsonschema2pojo")
public class DatasetRegistrationSchemaV1 {

    /**
     * The study name
     * (Required)
     *
     */
    @JsonProperty("studyName")
    @JsonPropertyDescription("The study name")
    private String studyName;
    /**
     * The study type
     * (Required)
     *
     */
    @JsonProperty("studyType")
    @JsonPropertyDescription("The study type")
    private DatasetRegistrationSchemaV1 .StudyType studyType;
    /**
     * Description of the study
     * (Required)
     *
     */
    @JsonProperty("studyDescription")
    @JsonPropertyDescription("Description of the study")
    private String studyDescription;
    /**
     * All data types that study encompasses
     * (Required)
     *
     */
    @JsonProperty("dataTypes")
    @JsonPropertyDescription("All data types that study encompasses")
    private List<String> dataTypes = new ArrayList<String>();
    /**
     * List of File Types
     * (Required)
     *
     */
    @JsonProperty("fileTypes")
    @JsonPropertyDescription("List of File Types")
    private List<FileTypeObject> fileTypes = new ArrayList<FileTypeObject>();
    /**
     * Phenotype/Indication Studied
     * (Required)
     *
     */
    @JsonProperty("phenotypeIndication")
    @JsonPropertyDescription("Phenotype/Indication Studied")
    private String phenotypeIndication;
    /**
     * Species
     * (Required)
     *
     */
    @JsonProperty("species")
    @JsonPropertyDescription("Species")
    private String species;
    /**
     * Principal Investigator Name
     * (Required)
     *
     */
    @JsonProperty("piName")
    @JsonPropertyDescription("Principal Investigator Name")
    private String piName;
    /**
     * The user creating the dataset submission
     * (Required)
     *
     */
    @JsonProperty("dataSubmitterUserId")
    @JsonPropertyDescription("The user creating the dataset submission")
    private Double dataSubmitterUserId;
    /**
     * Data Custodian Email
     * (Required)
     *
     */
    @JsonProperty("dataCustodianEmail")
    @JsonPropertyDescription("Data Custodian Email")
    private List<String> dataCustodianEmail = new ArrayList<String>();
    /**
     * Public Visibility of this study
     * (Required)
     *
     */
    @JsonProperty("publicVisibility")
    @JsonPropertyDescription("Public Visibility of this study")
    private Boolean publicVisibility;
    @JsonProperty("nihAnvilUse")
    private List<NihAnvilUse> nihAnvilUse = new ArrayList<NihAnvilUse>();
    /**
     * Are you planning to submit to AnVIL?
     *
     */
    @JsonProperty("submittingToAnvil")
    @JsonPropertyDescription("Are you planning to submit to AnVIL?")
    private Boolean submittingToAnvil;
    /**
     * dbGaP phs ID
     *
     */
    @JsonProperty("dbGaPPhsID")
    @JsonPropertyDescription("dbGaP phs ID")
    private String dbGaPPhsID;
    /**
     * dbGaP Study Registration Name
     *
     */
    @JsonProperty("dbGaPStudyRegistrationName")
    @JsonPropertyDescription("dbGaP Study Registration Name")
    private String dbGaPStudyRegistrationName;
    /**
     * Embargo Release Date
     *
     */
    @JsonProperty("embargoReleaseDate")
    @JsonPropertyDescription("Embargo Release Date")
    private String embargoReleaseDate;
    /**
     * Sequencing Center
     *
     */
    @JsonProperty("sequencingCenter")
    @JsonPropertyDescription("Sequencing Center")
    private String sequencingCenter;
    /**
     * Principal Investigator Email
     *
     */
    @JsonProperty("piEmail")
    @JsonPropertyDescription("Principal Investigator Email")
    private String piEmail;
    /**
     * Principal Investigator Institution
     *
     */
    @JsonProperty("piInstitution")
    @JsonPropertyDescription("Principal Investigator Institution")
    private Double piInstitution;
    /**
     * NIH Grant or Contract Number
     *
     */
    @JsonProperty("nihGrantContractNumber")
    @JsonPropertyDescription("NIH Grant or Contract Number")
    private String nihGrantContractNumber;
    /**
     * NIH ICs Supporting the Study
     *
     */
    @JsonProperty("nihICsSupportingStudy")
    @JsonPropertyDescription("NIH ICs Supporting the Study")
    private DatasetRegistrationSchemaV1 .NihICsSupportingStudy nihICsSupportingStudy;
    /**
     * NIH Program Officer Name
     *
     */
    @JsonProperty("nihProgramOfficerName")
    @JsonPropertyDescription("NIH Program Officer Name")
    private String nihProgramOfficerName;
    /**
     * NIH Institution/Center for Submission
     *
     */
    @JsonProperty("nihInstitutionCenterSubmission")
    @JsonPropertyDescription("NIH Institution/Center for Submission")
    private DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission nihInstitutionCenterSubmission;
    /**
     * NIH Genomic Program Administrator Name
     *
     */
    @JsonProperty("nihGenomicProgramAdministratorName")
    @JsonPropertyDescription("NIH Genomic Program Administrator Name")
    private String nihGenomicProgramAdministratorName;
    /**
     * Is this a multi-center study?
     *
     */
    @JsonProperty("multiCenterStudy")
    @JsonPropertyDescription("Is this a multi-center study?")
    private Boolean multiCenterStudy;
    /**
     * What are the collaborating sites?
     *
     */
    @JsonProperty("collaboratingSites")
    @JsonPropertyDescription("What are the collaborating sites?")
    private List<String> collaboratingSites = new ArrayList<String>();
    /**
     * Is controlled access required for genomic summary results (GSR)?
     *
     */
    @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSR")
    @JsonPropertyDescription("Is controlled access required for genomic summary results (GSR)?")
    private Boolean controlledAccessRequiredForGenomicSummaryResultsGSR;
    /**
     * If no, explain why controlled access is required for GSR
     *
     */
    @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation")
    @JsonPropertyDescription("If no, explain why controlled access is required for GSR")
    private String controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation;
    /**
     * Are you requesting an Alternative Data Sharing Plan for samples that cannot be shared through a public repository or database?
     *
     */
    @JsonProperty("alternativeDataSharingPlan")
    @JsonPropertyDescription("Are you requesting an Alternative Data Sharing Plan for samples that cannot be shared through a public repository or database?")
    private Boolean alternativeDataSharingPlan;
    /**
     * Please mark the reasons for which you are requesting an Alternative Data Sharing Plan (check all that apply)
     *
     */
    @JsonProperty("alternativeDataSharingPlanReasons")
    @JsonPropertyDescription("Please mark the reasons for which you are requesting an Alternative Data Sharing Plan (check all that apply)")
    private List<AlternativeDataSharingPlanReason> alternativeDataSharingPlanReasons = new ArrayList<AlternativeDataSharingPlanReason>();
    /**
     * Explanation of Request
     *
     */
    @JsonProperty("alternativeDataSharingPlanExplanation")
    @JsonPropertyDescription("Explanation of Request")
    private String alternativeDataSharingPlanExplanation;
    /**
     * Upload your alternative sharing plan (file upload)
     *
     */
    @JsonProperty("alternativeDataSharingPlanFileName")
    @JsonPropertyDescription("Upload your alternative sharing plan (file upload)")
    private String alternativeDataSharingPlanFileName;
    /**
     * Upload your alternative sharing plan (file upload)
     *
     */
    @JsonProperty("alternativeDataSharingPlanDataSubmitted")
    @JsonPropertyDescription("Upload your alternative sharing plan (file upload)")
    private List<AlternativeDataSharingPlanDataSubmitted> alternativeDataSharingPlanDataSubmitted = new ArrayList<AlternativeDataSharingPlanDataSubmitted>();
    /**
     * Data to be released will meet the timeframes specified in the NHGRI Guidance for Data Submission and Data Release
     *
     */
    @JsonProperty("alternativeDataSharingPlanDataReleased")
    @JsonPropertyDescription("Data to be released will meet the timeframes specified in the NHGRI Guidance for Data Submission and Data Release")
    private Boolean alternativeDataSharingPlanDataReleased;
    /**
     * Target Delivery Date
     *
     */
    @JsonProperty("alternativeDataSharingPlanTargetDeliveryDate")
    @JsonPropertyDescription("Target Delivery Date")
    private String alternativeDataSharingPlanTargetDeliveryDate;
    /**
     * Target Public Release Date
     *
     */
    @JsonProperty("alternativeDataSharingPlanTargetPublicReleaseDate")
    @JsonPropertyDescription("Target Public Release Date")
    private String alternativeDataSharingPlanTargetPublicReleaseDate;
    /**
     * Does the data need to be managed under Controlled or Open Access?
     *
     */
    @JsonProperty("alternativeDataSharingPlanControlledOpenAccess")
    @JsonPropertyDescription("Does the data need to be managed under Controlled or Open Access?")
    private DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess alternativeDataSharingPlanControlledOpenAccess;
    /**
     * Data Access Committee ID
     * (Required)
     *
     */
    @JsonProperty("dataAccessCommitteeId")
    @JsonPropertyDescription("Data Access Committee ID")
    private Double dataAccessCommitteeId;
    /**
     * Consent Groups
     * (Required)
     *
     */
    @JsonProperty("consentGroups")
    @JsonPropertyDescription("Consent Groups")
    private List<ConsentGroup> consentGroups = new ArrayList<ConsentGroup>();

    /**
     * The study name
     * (Required)
     *
     */
    @JsonProperty("studyName")
    public String getStudyName() {
        return studyName;
    }

    /**
     * The study name
     * (Required)
     *
     */
    @JsonProperty("studyName")
    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    /**
     * The study type
     * (Required)
     *
     */
    @JsonProperty("studyType")
    public DatasetRegistrationSchemaV1 .StudyType getStudyType() {
        return studyType;
    }

    /**
     * The study type
     * (Required)
     *
     */
    @JsonProperty("studyType")
    public void setStudyType(DatasetRegistrationSchemaV1 .StudyType studyType) {
        this.studyType = studyType;
    }

    /**
     * Description of the study
     * (Required)
     *
     */
    @JsonProperty("studyDescription")
    public String getStudyDescription() {
        return studyDescription;
    }

    /**
     * Description of the study
     * (Required)
     *
     */
    @JsonProperty("studyDescription")
    public void setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
    }

    /**
     * All data types that study encompasses
     * (Required)
     *
     */
    @JsonProperty("dataTypes")
    public List<String> getDataTypes() {
        return dataTypes;
    }

    /**
     * All data types that study encompasses
     * (Required)
     *
     */
    @JsonProperty("dataTypes")
    public void setDataTypes(List<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    /**
     * List of File Types
     * (Required)
     *
     */
    @JsonProperty("fileTypes")
    public List<FileTypeObject> getFileTypes() {
        return fileTypes;
    }

    /**
     * List of File Types
     * (Required)
     *
     */
    @JsonProperty("fileTypes")
    public void setFileTypes(List<FileTypeObject> fileTypes) {
        this.fileTypes = fileTypes;
    }

    /**
     * Phenotype/Indication Studied
     * (Required)
     *
     */
    @JsonProperty("phenotypeIndication")
    public String getPhenotypeIndication() {
        return phenotypeIndication;
    }

    /**
     * Phenotype/Indication Studied
     * (Required)
     *
     */
    @JsonProperty("phenotypeIndication")
    public void setPhenotypeIndication(String phenotypeIndication) {
        this.phenotypeIndication = phenotypeIndication;
    }

    /**
     * Species
     * (Required)
     *
     */
    @JsonProperty("species")
    public String getSpecies() {
        return species;
    }

    /**
     * Species
     * (Required)
     *
     */
    @JsonProperty("species")
    public void setSpecies(String species) {
        this.species = species;
    }

    /**
     * Principal Investigator Name
     * (Required)
     *
     */
    @JsonProperty("piName")
    public String getPiName() {
        return piName;
    }

    /**
     * Principal Investigator Name
     * (Required)
     *
     */
    @JsonProperty("piName")
    public void setPiName(String piName) {
        this.piName = piName;
    }

    /**
     * The user creating the dataset submission
     * (Required)
     *
     */
    @JsonProperty("dataSubmitterUserId")
    public Double getDataSubmitterUserId() {
        return dataSubmitterUserId;
    }

    /**
     * The user creating the dataset submission
     * (Required)
     *
     */
    @JsonProperty("dataSubmitterUserId")
    public void setDataSubmitterUserId(Double dataSubmitterUserId) {
        this.dataSubmitterUserId = dataSubmitterUserId;
    }

    /**
     * Data Custodian Email
     * (Required)
     *
     */
    @JsonProperty("dataCustodianEmail")
    public List<String> getDataCustodianEmail() {
        return dataCustodianEmail;
    }

    /**
     * Data Custodian Email
     * (Required)
     *
     */
    @JsonProperty("dataCustodianEmail")
    public void setDataCustodianEmail(List<String> dataCustodianEmail) {
        this.dataCustodianEmail = dataCustodianEmail;
    }

    /**
     * Public Visibility of this study
     * (Required)
     *
     */
    @JsonProperty("publicVisibility")
    public Boolean getPublicVisibility() {
        return publicVisibility;
    }

    /**
     * Public Visibility of this study
     * (Required)
     *
     */
    @JsonProperty("publicVisibility")
    public void setPublicVisibility(Boolean publicVisibility) {
        this.publicVisibility = publicVisibility;
    }

    @JsonProperty("nihAnvilUse")
    public List<NihAnvilUse> getNihAnvilUse() {
        return nihAnvilUse;
    }

    @JsonProperty("nihAnvilUse")
    public void setNihAnvilUse(List<NihAnvilUse> nihAnvilUse) {
        this.nihAnvilUse = nihAnvilUse;
    }

    /**
     * Are you planning to submit to AnVIL?
     *
     */
    @JsonProperty("submittingToAnvil")
    public Boolean getSubmittingToAnvil() {
        return submittingToAnvil;
    }

    /**
     * Are you planning to submit to AnVIL?
     *
     */
    @JsonProperty("submittingToAnvil")
    public void setSubmittingToAnvil(Boolean submittingToAnvil) {
        this.submittingToAnvil = submittingToAnvil;
    }

    /**
     * dbGaP phs ID
     *
     */
    @JsonProperty("dbGaPPhsID")
    public String getDbGaPPhsID() {
        return dbGaPPhsID;
    }

    /**
     * dbGaP phs ID
     *
     */
    @JsonProperty("dbGaPPhsID")
    public void setDbGaPPhsID(String dbGaPPhsID) {
        this.dbGaPPhsID = dbGaPPhsID;
    }

    /**
     * dbGaP Study Registration Name
     *
     */
    @JsonProperty("dbGaPStudyRegistrationName")
    public String getDbGaPStudyRegistrationName() {
        return dbGaPStudyRegistrationName;
    }

    /**
     * dbGaP Study Registration Name
     *
     */
    @JsonProperty("dbGaPStudyRegistrationName")
    public void setDbGaPStudyRegistrationName(String dbGaPStudyRegistrationName) {
        this.dbGaPStudyRegistrationName = dbGaPStudyRegistrationName;
    }

    /**
     * Embargo Release Date
     *
     */
    @JsonProperty("embargoReleaseDate")
    public String getEmbargoReleaseDate() {
        return embargoReleaseDate;
    }

    /**
     * Embargo Release Date
     *
     */
    @JsonProperty("embargoReleaseDate")
    public void setEmbargoReleaseDate(String embargoReleaseDate) {
        this.embargoReleaseDate = embargoReleaseDate;
    }

    /**
     * Sequencing Center
     *
     */
    @JsonProperty("sequencingCenter")
    public String getSequencingCenter() {
        return sequencingCenter;
    }

    /**
     * Sequencing Center
     *
     */
    @JsonProperty("sequencingCenter")
    public void setSequencingCenter(String sequencingCenter) {
        this.sequencingCenter = sequencingCenter;
    }

    /**
     * Principal Investigator Email
     *
     */
    @JsonProperty("piEmail")
    public String getPiEmail() {
        return piEmail;
    }

    /**
     * Principal Investigator Email
     *
     */
    @JsonProperty("piEmail")
    public void setPiEmail(String piEmail) {
        this.piEmail = piEmail;
    }

    /**
     * Principal Investigator Institution
     *
     */
    @JsonProperty("piInstitution")
    public Double getPiInstitution() {
        return piInstitution;
    }

    /**
     * Principal Investigator Institution
     *
     */
    @JsonProperty("piInstitution")
    public void setPiInstitution(Double piInstitution) {
        this.piInstitution = piInstitution;
    }

    /**
     * NIH Grant or Contract Number
     *
     */
    @JsonProperty("nihGrantContractNumber")
    public String getNihGrantContractNumber() {
        return nihGrantContractNumber;
    }

    /**
     * NIH Grant or Contract Number
     *
     */
    @JsonProperty("nihGrantContractNumber")
    public void setNihGrantContractNumber(String nihGrantContractNumber) {
        this.nihGrantContractNumber = nihGrantContractNumber;
    }

    /**
     * NIH ICs Supporting the Study
     *
     */
    @JsonProperty("nihICsSupportingStudy")
    public DatasetRegistrationSchemaV1 .NihICsSupportingStudy getNihICsSupportingStudy() {
        return nihICsSupportingStudy;
    }

    /**
     * NIH ICs Supporting the Study
     *
     */
    @JsonProperty("nihICsSupportingStudy")
    public void setNihICsSupportingStudy(DatasetRegistrationSchemaV1 .NihICsSupportingStudy nihICsSupportingStudy) {
        this.nihICsSupportingStudy = nihICsSupportingStudy;
    }

    /**
     * NIH Program Officer Name
     *
     */
    @JsonProperty("nihProgramOfficerName")
    public String getNihProgramOfficerName() {
        return nihProgramOfficerName;
    }

    /**
     * NIH Program Officer Name
     *
     */
    @JsonProperty("nihProgramOfficerName")
    public void setNihProgramOfficerName(String nihProgramOfficerName) {
        this.nihProgramOfficerName = nihProgramOfficerName;
    }

    /**
     * NIH Institution/Center for Submission
     *
     */
    @JsonProperty("nihInstitutionCenterSubmission")
    public DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission getNihInstitutionCenterSubmission() {
        return nihInstitutionCenterSubmission;
    }

    /**
     * NIH Institution/Center for Submission
     *
     */
    @JsonProperty("nihInstitutionCenterSubmission")
    public void setNihInstitutionCenterSubmission(DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission nihInstitutionCenterSubmission) {
        this.nihInstitutionCenterSubmission = nihInstitutionCenterSubmission;
    }

    /**
     * NIH Genomic Program Administrator Name
     *
     */
    @JsonProperty("nihGenomicProgramAdministratorName")
    public String getNihGenomicProgramAdministratorName() {
        return nihGenomicProgramAdministratorName;
    }

    /**
     * NIH Genomic Program Administrator Name
     *
     */
    @JsonProperty("nihGenomicProgramAdministratorName")
    public void setNihGenomicProgramAdministratorName(String nihGenomicProgramAdministratorName) {
        this.nihGenomicProgramAdministratorName = nihGenomicProgramAdministratorName;
    }

    /**
     * Is this a multi-center study?
     *
     */
    @JsonProperty("multiCenterStudy")
    public Boolean getMultiCenterStudy() {
        return multiCenterStudy;
    }

    /**
     * Is this a multi-center study?
     *
     */
    @JsonProperty("multiCenterStudy")
    public void setMultiCenterStudy(Boolean multiCenterStudy) {
        this.multiCenterStudy = multiCenterStudy;
    }

    /**
     * What are the collaborating sites?
     *
     */
    @JsonProperty("collaboratingSites")
    public List<String> getCollaboratingSites() {
        return collaboratingSites;
    }

    /**
     * What are the collaborating sites?
     *
     */
    @JsonProperty("collaboratingSites")
    public void setCollaboratingSites(List<String> collaboratingSites) {
        this.collaboratingSites = collaboratingSites;
    }

    /**
     * Is controlled access required for genomic summary results (GSR)?
     *
     */
    @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSR")
    public Boolean getControlledAccessRequiredForGenomicSummaryResultsGSR() {
        return controlledAccessRequiredForGenomicSummaryResultsGSR;
    }

    /**
     * Is controlled access required for genomic summary results (GSR)?
     *
     */
    @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSR")
    public void setControlledAccessRequiredForGenomicSummaryResultsGSR(Boolean controlledAccessRequiredForGenomicSummaryResultsGSR) {
        this.controlledAccessRequiredForGenomicSummaryResultsGSR = controlledAccessRequiredForGenomicSummaryResultsGSR;
    }

    /**
     * If no, explain why controlled access is required for GSR
     *
     */
    @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation")
    public String getControlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation() {
        return controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation;
    }

    /**
     * If no, explain why controlled access is required for GSR
     *
     */
    @JsonProperty("controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation")
    public void setControlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation(String controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation) {
        this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation = controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation;
    }

    /**
     * Are you requesting an Alternative Data Sharing Plan for samples that cannot be shared through a public repository or database?
     *
     */
    @JsonProperty("alternativeDataSharingPlan")
    public Boolean getAlternativeDataSharingPlan() {
        return alternativeDataSharingPlan;
    }

    /**
     * Are you requesting an Alternative Data Sharing Plan for samples that cannot be shared through a public repository or database?
     *
     */
    @JsonProperty("alternativeDataSharingPlan")
    public void setAlternativeDataSharingPlan(Boolean alternativeDataSharingPlan) {
        this.alternativeDataSharingPlan = alternativeDataSharingPlan;
    }

    /**
     * Please mark the reasons for which you are requesting an Alternative Data Sharing Plan (check all that apply)
     *
     */
    @JsonProperty("alternativeDataSharingPlanReasons")
    public List<AlternativeDataSharingPlanReason> getAlternativeDataSharingPlanReasons() {
        return alternativeDataSharingPlanReasons;
    }

    /**
     * Please mark the reasons for which you are requesting an Alternative Data Sharing Plan (check all that apply)
     *
     */
    @JsonProperty("alternativeDataSharingPlanReasons")
    public void setAlternativeDataSharingPlanReasons(List<AlternativeDataSharingPlanReason> alternativeDataSharingPlanReasons) {
        this.alternativeDataSharingPlanReasons = alternativeDataSharingPlanReasons;
    }

    /**
     * Explanation of Request
     *
     */
    @JsonProperty("alternativeDataSharingPlanExplanation")
    public String getAlternativeDataSharingPlanExplanation() {
        return alternativeDataSharingPlanExplanation;
    }

    /**
     * Explanation of Request
     *
     */
    @JsonProperty("alternativeDataSharingPlanExplanation")
    public void setAlternativeDataSharingPlanExplanation(String alternativeDataSharingPlanExplanation) {
        this.alternativeDataSharingPlanExplanation = alternativeDataSharingPlanExplanation;
    }

    /**
     * Upload your alternative sharing plan (file upload)
     *
     */
    @JsonProperty("alternativeDataSharingPlanFileName")
    public String getAlternativeDataSharingPlanFileName() {
        return alternativeDataSharingPlanFileName;
    }

    /**
     * Upload your alternative sharing plan (file upload)
     *
     */
    @JsonProperty("alternativeDataSharingPlanFileName")
    public void setAlternativeDataSharingPlanFileName(String alternativeDataSharingPlanFileName) {
        this.alternativeDataSharingPlanFileName = alternativeDataSharingPlanFileName;
    }

    /**
     * Upload your alternative sharing plan (file upload)
     *
     */
    @JsonProperty("alternativeDataSharingPlanDataSubmitted")
    public List<AlternativeDataSharingPlanDataSubmitted> getAlternativeDataSharingPlanDataSubmitted() {
        return alternativeDataSharingPlanDataSubmitted;
    }

    /**
     * Upload your alternative sharing plan (file upload)
     *
     */
    @JsonProperty("alternativeDataSharingPlanDataSubmitted")
    public void setAlternativeDataSharingPlanDataSubmitted(List<AlternativeDataSharingPlanDataSubmitted> alternativeDataSharingPlanDataSubmitted) {
        this.alternativeDataSharingPlanDataSubmitted = alternativeDataSharingPlanDataSubmitted;
    }

    /**
     * Data to be released will meet the timeframes specified in the NHGRI Guidance for Data Submission and Data Release
     *
     */
    @JsonProperty("alternativeDataSharingPlanDataReleased")
    public Boolean getAlternativeDataSharingPlanDataReleased() {
        return alternativeDataSharingPlanDataReleased;
    }

    /**
     * Data to be released will meet the timeframes specified in the NHGRI Guidance for Data Submission and Data Release
     *
     */
    @JsonProperty("alternativeDataSharingPlanDataReleased")
    public void setAlternativeDataSharingPlanDataReleased(Boolean alternativeDataSharingPlanDataReleased) {
        this.alternativeDataSharingPlanDataReleased = alternativeDataSharingPlanDataReleased;
    }

    /**
     * Target Delivery Date
     *
     */
    @JsonProperty("alternativeDataSharingPlanTargetDeliveryDate")
    public String getAlternativeDataSharingPlanTargetDeliveryDate() {
        return alternativeDataSharingPlanTargetDeliveryDate;
    }

    /**
     * Target Delivery Date
     *
     */
    @JsonProperty("alternativeDataSharingPlanTargetDeliveryDate")
    public void setAlternativeDataSharingPlanTargetDeliveryDate(String alternativeDataSharingPlanTargetDeliveryDate) {
        this.alternativeDataSharingPlanTargetDeliveryDate = alternativeDataSharingPlanTargetDeliveryDate;
    }

    /**
     * Target Public Release Date
     *
     */
    @JsonProperty("alternativeDataSharingPlanTargetPublicReleaseDate")
    public String getAlternativeDataSharingPlanTargetPublicReleaseDate() {
        return alternativeDataSharingPlanTargetPublicReleaseDate;
    }

    /**
     * Target Public Release Date
     *
     */
    @JsonProperty("alternativeDataSharingPlanTargetPublicReleaseDate")
    public void setAlternativeDataSharingPlanTargetPublicReleaseDate(String alternativeDataSharingPlanTargetPublicReleaseDate) {
        this.alternativeDataSharingPlanTargetPublicReleaseDate = alternativeDataSharingPlanTargetPublicReleaseDate;
    }

    /**
     * Does the data need to be managed under Controlled or Open Access?
     *
     */
    @JsonProperty("alternativeDataSharingPlanControlledOpenAccess")
    public DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess getAlternativeDataSharingPlanControlledOpenAccess() {
        return alternativeDataSharingPlanControlledOpenAccess;
    }

    /**
     * Does the data need to be managed under Controlled or Open Access?
     *
     */
    @JsonProperty("alternativeDataSharingPlanControlledOpenAccess")
    public void setAlternativeDataSharingPlanControlledOpenAccess(DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess alternativeDataSharingPlanControlledOpenAccess) {
        this.alternativeDataSharingPlanControlledOpenAccess = alternativeDataSharingPlanControlledOpenAccess;
    }

    /**
     * Data Access Committee ID
     * (Required)
     *
     */
    @JsonProperty("dataAccessCommitteeId")
    public Double getDataAccessCommitteeId() {
        return dataAccessCommitteeId;
    }

    /**
     * Data Access Committee ID
     * (Required)
     *
     */
    @JsonProperty("dataAccessCommitteeId")
    public void setDataAccessCommitteeId(Double dataAccessCommitteeId) {
        this.dataAccessCommitteeId = dataAccessCommitteeId;
    }

    /**
     * Consent Groups
     * (Required)
     *
     */
    @JsonProperty("consentGroups")
    public List<ConsentGroup> getConsentGroups() {
        return consentGroups;
    }

    /**
     * Consent Groups
     * (Required)
     *
     */
    @JsonProperty("consentGroups")
    public void setConsentGroups(List<ConsentGroup> consentGroups) {
        this.consentGroups = consentGroups;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DatasetRegistrationSchemaV1 .class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("studyName");
        sb.append('=');
        sb.append(((this.studyName == null)?"<null>":this.studyName));
        sb.append(',');
        sb.append("studyType");
        sb.append('=');
        sb.append(((this.studyType == null)?"<null>":this.studyType));
        sb.append(',');
        sb.append("studyDescription");
        sb.append('=');
        sb.append(((this.studyDescription == null)?"<null>":this.studyDescription));
        sb.append(',');
        sb.append("dataTypes");
        sb.append('=');
        sb.append(((this.dataTypes == null)?"<null>":this.dataTypes));
        sb.append(',');
        sb.append("fileTypes");
        sb.append('=');
        sb.append(((this.fileTypes == null)?"<null>":this.fileTypes));
        sb.append(',');
        sb.append("phenotypeIndication");
        sb.append('=');
        sb.append(((this.phenotypeIndication == null)?"<null>":this.phenotypeIndication));
        sb.append(',');
        sb.append("species");
        sb.append('=');
        sb.append(((this.species == null)?"<null>":this.species));
        sb.append(',');
        sb.append("piName");
        sb.append('=');
        sb.append(((this.piName == null)?"<null>":this.piName));
        sb.append(',');
        sb.append("dataSubmitterUserId");
        sb.append('=');
        sb.append(((this.dataSubmitterUserId == null)?"<null>":this.dataSubmitterUserId));
        sb.append(',');
        sb.append("dataCustodianEmail");
        sb.append('=');
        sb.append(((this.dataCustodianEmail == null)?"<null>":this.dataCustodianEmail));
        sb.append(',');
        sb.append("publicVisibility");
        sb.append('=');
        sb.append(((this.publicVisibility == null)?"<null>":this.publicVisibility));
        sb.append(',');
        sb.append("nihAnvilUse");
        sb.append('=');
        sb.append(((this.nihAnvilUse == null)?"<null>":this.nihAnvilUse));
        sb.append(',');
        sb.append("submittingToAnvil");
        sb.append('=');
        sb.append(((this.submittingToAnvil == null)?"<null>":this.submittingToAnvil));
        sb.append(',');
        sb.append("dbGaPPhsID");
        sb.append('=');
        sb.append(((this.dbGaPPhsID == null)?"<null>":this.dbGaPPhsID));
        sb.append(',');
        sb.append("dbGaPStudyRegistrationName");
        sb.append('=');
        sb.append(((this.dbGaPStudyRegistrationName == null)?"<null>":this.dbGaPStudyRegistrationName));
        sb.append(',');
        sb.append("embargoReleaseDate");
        sb.append('=');
        sb.append(((this.embargoReleaseDate == null)?"<null>":this.embargoReleaseDate));
        sb.append(',');
        sb.append("sequencingCenter");
        sb.append('=');
        sb.append(((this.sequencingCenter == null)?"<null>":this.sequencingCenter));
        sb.append(',');
        sb.append("piEmail");
        sb.append('=');
        sb.append(((this.piEmail == null)?"<null>":this.piEmail));
        sb.append(',');
        sb.append("piInstitution");
        sb.append('=');
        sb.append(((this.piInstitution == null)?"<null>":this.piInstitution));
        sb.append(',');
        sb.append("nihGrantContractNumber");
        sb.append('=');
        sb.append(((this.nihGrantContractNumber == null)?"<null>":this.nihGrantContractNumber));
        sb.append(',');
        sb.append("nihICsSupportingStudy");
        sb.append('=');
        sb.append(((this.nihICsSupportingStudy == null)?"<null>":this.nihICsSupportingStudy));
        sb.append(',');
        sb.append("nihProgramOfficerName");
        sb.append('=');
        sb.append(((this.nihProgramOfficerName == null)?"<null>":this.nihProgramOfficerName));
        sb.append(',');
        sb.append("nihInstitutionCenterSubmission");
        sb.append('=');
        sb.append(((this.nihInstitutionCenterSubmission == null)?"<null>":this.nihInstitutionCenterSubmission));
        sb.append(',');
        sb.append("nihGenomicProgramAdministratorName");
        sb.append('=');
        sb.append(((this.nihGenomicProgramAdministratorName == null)?"<null>":this.nihGenomicProgramAdministratorName));
        sb.append(',');
        sb.append("multiCenterStudy");
        sb.append('=');
        sb.append(((this.multiCenterStudy == null)?"<null>":this.multiCenterStudy));
        sb.append(',');
        sb.append("collaboratingSites");
        sb.append('=');
        sb.append(((this.collaboratingSites == null)?"<null>":this.collaboratingSites));
        sb.append(',');
        sb.append("controlledAccessRequiredForGenomicSummaryResultsGSR");
        sb.append('=');
        sb.append(((this.controlledAccessRequiredForGenomicSummaryResultsGSR == null)?"<null>":this.controlledAccessRequiredForGenomicSummaryResultsGSR));
        sb.append(',');
        sb.append("controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation");
        sb.append('=');
        sb.append(((this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation == null)?"<null>":this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation));
        sb.append(',');
        sb.append("alternativeDataSharingPlan");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlan == null)?"<null>":this.alternativeDataSharingPlan));
        sb.append(',');
        sb.append("alternativeDataSharingPlanReasons");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanReasons == null)?"<null>":this.alternativeDataSharingPlanReasons));
        sb.append(',');
        sb.append("alternativeDataSharingPlanExplanation");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanExplanation == null)?"<null>":this.alternativeDataSharingPlanExplanation));
        sb.append(',');
        sb.append("alternativeDataSharingPlanFileName");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanFileName == null)?"<null>":this.alternativeDataSharingPlanFileName));
        sb.append(',');
        sb.append("alternativeDataSharingPlanDataSubmitted");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanDataSubmitted == null)?"<null>":this.alternativeDataSharingPlanDataSubmitted));
        sb.append(',');
        sb.append("alternativeDataSharingPlanDataReleased");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanDataReleased == null)?"<null>":this.alternativeDataSharingPlanDataReleased));
        sb.append(',');
        sb.append("alternativeDataSharingPlanTargetDeliveryDate");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanTargetDeliveryDate == null)?"<null>":this.alternativeDataSharingPlanTargetDeliveryDate));
        sb.append(',');
        sb.append("alternativeDataSharingPlanTargetPublicReleaseDate");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanTargetPublicReleaseDate == null)?"<null>":this.alternativeDataSharingPlanTargetPublicReleaseDate));
        sb.append(',');
        sb.append("alternativeDataSharingPlanControlledOpenAccess");
        sb.append('=');
        sb.append(((this.alternativeDataSharingPlanControlledOpenAccess == null)?"<null>":this.alternativeDataSharingPlanControlledOpenAccess));
        sb.append(',');
        sb.append("dataAccessCommitteeId");
        sb.append('=');
        sb.append(((this.dataAccessCommitteeId == null)?"<null>":this.dataAccessCommitteeId));
        sb.append(',');
        sb.append("consentGroups");
        sb.append('=');
        sb.append(((this.consentGroups == null)?"<null>":this.consentGroups));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.nihGenomicProgramAdministratorName == null)? 0 :this.nihGenomicProgramAdministratorName.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanFileName == null)? 0 :this.alternativeDataSharingPlanFileName.hashCode()));
        result = ((result* 31)+((this.studyName == null)? 0 :this.studyName.hashCode()));
        result = ((result* 31)+((this.piEmail == null)? 0 :this.piEmail.hashCode()));
        result = ((result* 31)+((this.dataSubmitterUserId == null)? 0 :this.dataSubmitterUserId.hashCode()));
        result = ((result* 31)+((this.publicVisibility == null)? 0 :this.publicVisibility.hashCode()));
        result = ((result* 31)+((this.dataCustodianEmail == null)? 0 :this.dataCustodianEmail.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanDataSubmitted == null)? 0 :this.alternativeDataSharingPlanDataSubmitted.hashCode()));
        result = ((result* 31)+((this.submittingToAnvil == null)? 0 :this.submittingToAnvil.hashCode()));
        result = ((result* 31)+((this.dbGaPStudyRegistrationName == null)? 0 :this.dbGaPStudyRegistrationName.hashCode()));
        result = ((result* 31)+((this.collaboratingSites == null)? 0 :this.collaboratingSites.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanTargetDeliveryDate == null)? 0 :this.alternativeDataSharingPlanTargetDeliveryDate.hashCode()));
        result = ((result* 31)+((this.nihProgramOfficerName == null)? 0 :this.nihProgramOfficerName.hashCode()));
        result = ((result* 31)+((this.dataTypes == null)? 0 :this.dataTypes.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanTargetPublicReleaseDate == null)? 0 :this.alternativeDataSharingPlanTargetPublicReleaseDate.hashCode()));
        result = ((result* 31)+((this.nihGrantContractNumber == null)? 0 :this.nihGrantContractNumber.hashCode()));
        result = ((result* 31)+((this.studyType == null)? 0 :this.studyType.hashCode()));
        result = ((result* 31)+((this.phenotypeIndication == null)? 0 :this.phenotypeIndication.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanControlledOpenAccess == null)? 0 :this.alternativeDataSharingPlanControlledOpenAccess.hashCode()));
        result = ((result* 31)+((this.sequencingCenter == null)? 0 :this.sequencingCenter.hashCode()));
        result = ((result* 31)+((this.multiCenterStudy == null)? 0 :this.multiCenterStudy.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanDataReleased == null)? 0 :this.alternativeDataSharingPlanDataReleased.hashCode()));
        result = ((result* 31)+((this.consentGroups == null)? 0 :this.consentGroups.hashCode()));
        result = ((result* 31)+((this.studyDescription == null)? 0 :this.studyDescription.hashCode()));
        result = ((result* 31)+((this.nihAnvilUse == null)? 0 :this.nihAnvilUse.hashCode()));
        result = ((result* 31)+((this.dataAccessCommitteeId == null)? 0 :this.dataAccessCommitteeId.hashCode()));
        result = ((result* 31)+((this.nihICsSupportingStudy == null)? 0 :this.nihICsSupportingStudy.hashCode()));
        result = ((result* 31)+((this.fileTypes == null)? 0 :this.fileTypes.hashCode()));
        result = ((result* 31)+((this.piName == null)? 0 :this.piName.hashCode()));
        result = ((result* 31)+((this.dbGaPPhsID == null)? 0 :this.dbGaPPhsID.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlan == null)? 0 :this.alternativeDataSharingPlan.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanReasons == null)? 0 :this.alternativeDataSharingPlanReasons.hashCode()));
        result = ((result* 31)+((this.species == null)? 0 :this.species.hashCode()));
        result = ((result* 31)+((this.embargoReleaseDate == null)? 0 :this.embargoReleaseDate.hashCode()));
        result = ((result* 31)+((this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation == null)? 0 :this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation.hashCode()));
        result = ((result* 31)+((this.alternativeDataSharingPlanExplanation == null)? 0 :this.alternativeDataSharingPlanExplanation.hashCode()));
        result = ((result* 31)+((this.nihInstitutionCenterSubmission == null)? 0 :this.nihInstitutionCenterSubmission.hashCode()));
        result = ((result* 31)+((this.controlledAccessRequiredForGenomicSummaryResultsGSR == null)? 0 :this.controlledAccessRequiredForGenomicSummaryResultsGSR.hashCode()));
        result = ((result* 31)+((this.piInstitution == null)? 0 :this.piInstitution.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DatasetRegistrationSchemaV1) == false) {
            return false;
        }
        DatasetRegistrationSchemaV1 rhs = ((DatasetRegistrationSchemaV1) other);
        return ((((((((((((((((((((((((((((((((((((((((this.nihGenomicProgramAdministratorName == rhs.nihGenomicProgramAdministratorName)||((this.nihGenomicProgramAdministratorName!= null)&&this.nihGenomicProgramAdministratorName.equals(rhs.nihGenomicProgramAdministratorName)))&&((this.alternativeDataSharingPlanFileName == rhs.alternativeDataSharingPlanFileName)||((this.alternativeDataSharingPlanFileName!= null)&&this.alternativeDataSharingPlanFileName.equals(rhs.alternativeDataSharingPlanFileName))))&&((this.studyName == rhs.studyName)||((this.studyName!= null)&&this.studyName.equals(rhs.studyName))))&&((this.piEmail == rhs.piEmail)||((this.piEmail!= null)&&this.piEmail.equals(rhs.piEmail))))&&((this.dataSubmitterUserId == rhs.dataSubmitterUserId)||((this.dataSubmitterUserId!= null)&&this.dataSubmitterUserId.equals(rhs.dataSubmitterUserId))))&&((this.publicVisibility == rhs.publicVisibility)||((this.publicVisibility!= null)&&this.publicVisibility.equals(rhs.publicVisibility))))&&((this.dataCustodianEmail == rhs.dataCustodianEmail)||((this.dataCustodianEmail!= null)&&this.dataCustodianEmail.equals(rhs.dataCustodianEmail))))&&((this.alternativeDataSharingPlanDataSubmitted == rhs.alternativeDataSharingPlanDataSubmitted)||((this.alternativeDataSharingPlanDataSubmitted!= null)&&this.alternativeDataSharingPlanDataSubmitted.equals(rhs.alternativeDataSharingPlanDataSubmitted))))&&((this.submittingToAnvil == rhs.submittingToAnvil)||((this.submittingToAnvil!= null)&&this.submittingToAnvil.equals(rhs.submittingToAnvil))))&&((this.dbGaPStudyRegistrationName == rhs.dbGaPStudyRegistrationName)||((this.dbGaPStudyRegistrationName!= null)&&this.dbGaPStudyRegistrationName.equals(rhs.dbGaPStudyRegistrationName))))&&((this.collaboratingSites == rhs.collaboratingSites)||((this.collaboratingSites!= null)&&this.collaboratingSites.equals(rhs.collaboratingSites))))&&((this.alternativeDataSharingPlanTargetDeliveryDate == rhs.alternativeDataSharingPlanTargetDeliveryDate)||((this.alternativeDataSharingPlanTargetDeliveryDate!= null)&&this.alternativeDataSharingPlanTargetDeliveryDate.equals(rhs.alternativeDataSharingPlanTargetDeliveryDate))))&&((this.nihProgramOfficerName == rhs.nihProgramOfficerName)||((this.nihProgramOfficerName!= null)&&this.nihProgramOfficerName.equals(rhs.nihProgramOfficerName))))&&((this.dataTypes == rhs.dataTypes)||((this.dataTypes!= null)&&this.dataTypes.equals(rhs.dataTypes))))&&((this.alternativeDataSharingPlanTargetPublicReleaseDate == rhs.alternativeDataSharingPlanTargetPublicReleaseDate)||((this.alternativeDataSharingPlanTargetPublicReleaseDate!= null)&&this.alternativeDataSharingPlanTargetPublicReleaseDate.equals(rhs.alternativeDataSharingPlanTargetPublicReleaseDate))))&&((this.nihGrantContractNumber == rhs.nihGrantContractNumber)||((this.nihGrantContractNumber!= null)&&this.nihGrantContractNumber.equals(rhs.nihGrantContractNumber))))&&((this.studyType == rhs.studyType)||((this.studyType!= null)&&this.studyType.equals(rhs.studyType))))&&((this.phenotypeIndication == rhs.phenotypeIndication)||((this.phenotypeIndication!= null)&&this.phenotypeIndication.equals(rhs.phenotypeIndication))))&&((this.alternativeDataSharingPlanControlledOpenAccess == rhs.alternativeDataSharingPlanControlledOpenAccess)||((this.alternativeDataSharingPlanControlledOpenAccess!= null)&&this.alternativeDataSharingPlanControlledOpenAccess.equals(rhs.alternativeDataSharingPlanControlledOpenAccess))))&&((this.sequencingCenter == rhs.sequencingCenter)||((this.sequencingCenter!= null)&&this.sequencingCenter.equals(rhs.sequencingCenter))))&&((this.multiCenterStudy == rhs.multiCenterStudy)||((this.multiCenterStudy!= null)&&this.multiCenterStudy.equals(rhs.multiCenterStudy))))&&((this.alternativeDataSharingPlanDataReleased == rhs.alternativeDataSharingPlanDataReleased)||((this.alternativeDataSharingPlanDataReleased!= null)&&this.alternativeDataSharingPlanDataReleased.equals(rhs.alternativeDataSharingPlanDataReleased))))&&((this.consentGroups == rhs.consentGroups)||((this.consentGroups!= null)&&this.consentGroups.equals(rhs.consentGroups))))&&((this.studyDescription == rhs.studyDescription)||((this.studyDescription!= null)&&this.studyDescription.equals(rhs.studyDescription))))&&((this.nihAnvilUse == rhs.nihAnvilUse)||((this.nihAnvilUse!= null)&&this.nihAnvilUse.equals(rhs.nihAnvilUse))))&&((this.dataAccessCommitteeId == rhs.dataAccessCommitteeId)||((this.dataAccessCommitteeId!= null)&&this.dataAccessCommitteeId.equals(rhs.dataAccessCommitteeId))))&&((this.nihICsSupportingStudy == rhs.nihICsSupportingStudy)||((this.nihICsSupportingStudy!= null)&&this.nihICsSupportingStudy.equals(rhs.nihICsSupportingStudy))))&&((this.fileTypes == rhs.fileTypes)||((this.fileTypes!= null)&&this.fileTypes.equals(rhs.fileTypes))))&&((this.piName == rhs.piName)||((this.piName!= null)&&this.piName.equals(rhs.piName))))&&((this.dbGaPPhsID == rhs.dbGaPPhsID)||((this.dbGaPPhsID!= null)&&this.dbGaPPhsID.equals(rhs.dbGaPPhsID))))&&((this.alternativeDataSharingPlan == rhs.alternativeDataSharingPlan)||((this.alternativeDataSharingPlan!= null)&&this.alternativeDataSharingPlan.equals(rhs.alternativeDataSharingPlan))))&&((this.alternativeDataSharingPlanReasons == rhs.alternativeDataSharingPlanReasons)||((this.alternativeDataSharingPlanReasons!= null)&&this.alternativeDataSharingPlanReasons.equals(rhs.alternativeDataSharingPlanReasons))))&&((this.species == rhs.species)||((this.species!= null)&&this.species.equals(rhs.species))))&&((this.embargoReleaseDate == rhs.embargoReleaseDate)||((this.embargoReleaseDate!= null)&&this.embargoReleaseDate.equals(rhs.embargoReleaseDate))))&&((this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation == rhs.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation)||((this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation!= null)&&this.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation.equals(rhs.controlledAccessRequiredForGenomicSummaryResultsGSRNotRequiredExplanation))))&&((this.alternativeDataSharingPlanExplanation == rhs.alternativeDataSharingPlanExplanation)||((this.alternativeDataSharingPlanExplanation!= null)&&this.alternativeDataSharingPlanExplanation.equals(rhs.alternativeDataSharingPlanExplanation))))&&((this.nihInstitutionCenterSubmission == rhs.nihInstitutionCenterSubmission)||((this.nihInstitutionCenterSubmission!= null)&&this.nihInstitutionCenterSubmission.equals(rhs.nihInstitutionCenterSubmission))))&&((this.controlledAccessRequiredForGenomicSummaryResultsGSR == rhs.controlledAccessRequiredForGenomicSummaryResultsGSR)||((this.controlledAccessRequiredForGenomicSummaryResultsGSR!= null)&&this.controlledAccessRequiredForGenomicSummaryResultsGSR.equals(rhs.controlledAccessRequiredForGenomicSummaryResultsGSR))))&&((this.piInstitution == rhs.piInstitution)||((this.piInstitution!= null)&&this.piInstitution.equals(rhs.piInstitution))));
    }


    /**
     * Does the data need to be managed under Controlled or Open Access?
     *
     */
    @Generated("jsonschema2pojo")
    public enum AlternativeDataSharingPlanControlledOpenAccess {

        CONTROLLED_ACCESS("Controlled Access"),
        OPEN_ACCESS("Open Access");
        private final String value;
        private final static Map<String, DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess> CONSTANTS = new HashMap<String, DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess>();

        static {
            for (DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        AlternativeDataSharingPlanControlledOpenAccess(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess fromValue(String value) {
            DatasetRegistrationSchemaV1 .AlternativeDataSharingPlanControlledOpenAccess constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * NIH ICs Supporting the Study
     *
     */
    @Generated("jsonschema2pojo")
    public enum NihICsSupportingStudy {

        NCI("NCI"),
        NEI("NEI"),
        NHLBI("NHLBI"),
        NHGRI("NHGRI"),
        NIA("NIA"),
        NIAAA("NIAAA"),
        NIAID("NIAID"),
        NIAMS("NIAMS"),
        NIBIB("NIBIB"),
        NICHD("NICHD"),
        NIDCD("NIDCD"),
        NIDCR("NIDCR"),
        NIDDK("NIDDK"),
        NIDA("NIDA"),
        NIEHS("NIEHS"),
        NIGMS("NIGMS"),
        NIMH("NIMH"),
        NIMHD("NIMHD"),
        NINDS("NINDS"),
        NINR("NINR"),
        NLM("NLM"),
        CC("CC"),
        CIT("CIT"),
        CSR("CSR"),
        FIC("FIC"),
        NCATS("NCATS"),
        NCCIH("NCCIH");
        private final String value;
        private final static Map<String, DatasetRegistrationSchemaV1 .NihICsSupportingStudy> CONSTANTS = new HashMap<String, DatasetRegistrationSchemaV1 .NihICsSupportingStudy>();

        static {
            for (DatasetRegistrationSchemaV1 .NihICsSupportingStudy c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        NihICsSupportingStudy(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static DatasetRegistrationSchemaV1 .NihICsSupportingStudy fromValue(String value) {
            DatasetRegistrationSchemaV1 .NihICsSupportingStudy constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * NIH Institution/Center for Submission
     *
     */
    @Generated("jsonschema2pojo")
    public enum NihInstitutionCenterSubmission {

        NCI("NCI"),
        NEI("NEI"),
        NHLBI("NHLBI"),
        NHGRI("NHGRI"),
        NIA("NIA"),
        NIAAA("NIAAA"),
        NIAID("NIAID"),
        NIAMS("NIAMS"),
        NIBIB("NIBIB"),
        NICHD("NICHD"),
        NIDCD("NIDCD"),
        NIDCR("NIDCR"),
        NIDDK("NIDDK"),
        NIDA("NIDA"),
        NIEHS("NIEHS"),
        NIGMS("NIGMS"),
        NIMH("NIMH"),
        NIMHD("NIMHD"),
        NINDS("NINDS"),
        NINR("NINR"),
        NLM("NLM"),
        CC("CC"),
        CIT("CIT"),
        CSR("CSR"),
        FIC("FIC"),
        NCATS("NCATS"),
        NCCIH("NCCIH");
        private final String value;
        private final static Map<String, DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission> CONSTANTS = new HashMap<String, DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission>();

        static {
            for (DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        NihInstitutionCenterSubmission(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission fromValue(String value) {
            DatasetRegistrationSchemaV1 .NihInstitutionCenterSubmission constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * The study type
     *
     */
    @Generated("jsonschema2pojo")
    public enum StudyType {

        OBSERVATIONAL("Observational"),
        INTERVENTIONAL("Interventional"),
        DESCRIPTIVE("Descriptive"),
        ANALYTICAL("Analytical"),
        PROSPECTIVE("Prospective"),
        RETROSPECTIVE("Retrospective"),
        CASE_REPORT("Case report"),
        CASE_SERIES("Case series"),
        CROSS_SECTIONAL("Cross-sectional"),
        COHORT_STUDY("Cohort study");
        private final String value;
        private final static Map<String, DatasetRegistrationSchemaV1 .StudyType> CONSTANTS = new HashMap<String, DatasetRegistrationSchemaV1 .StudyType>();

        static {
            for (DatasetRegistrationSchemaV1 .StudyType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        StudyType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static DatasetRegistrationSchemaV1 .StudyType fromValue(String value) {
            DatasetRegistrationSchemaV1 .StudyType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
