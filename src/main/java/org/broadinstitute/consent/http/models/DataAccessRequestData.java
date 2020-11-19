package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataAccessRequestData {

    /**
     * These properties are deprecated and should no longer be used.
     * In many cases, they represent user properties, consent related properties,
     * or duplicate existing DAR fields.
     * See https://broadinstitute.atlassian.net/browse/DUOS-728 for more info.
     */
    public static final List<String> DEPRECATED_PROPS = Arrays
        .asList("referenceId", "investigator",
            "institution", "department", "address1", "address2", "city", "zipcode", "zipCode",
            "state", "country", "researcher", "userId", "isThePi", "havePi", "piEmail",
            "profileName", "pubmedId", "scientificUrl", "eraExpiration", "academicEmail",
            "eraAuthorized", "nihUsername", "linkedIn", "orcid", "researcherGate", "datasetDetail",
            "datasets", "datasetId", "validRestriction", "restriction", "translatedUseRestriction",
            "createDate", "sortDate");

    // prefix for partialDarCode, should be pulled by functions that generate/update ONLY
    // since class is used within both drafts and submitted dars, it's best to control its implementation on the outer function call
    public static final String partialDarCodePrefix = "temp_DAR_";

    private String referenceId;
    private String investigator;
    private String institution;
    private String department;
    private String division;
    private String address1;
    private String address2;
    private String city;
    @SerializedName(value = "zipCode", alternate = "zipcode")
    private String zipCode;
    private String state;
    private String country;
    private String projectTitle;
    private Boolean checkCollaborator;
    private String researcher;
    private Integer userId;
    private String isThePi;
    private String havePi;
    private String piEmail;
    private String profileName;
    private String pubmedId;
    private String scientificUrl;
    private Boolean eraExpiration;
    private String academicEmail;
    private Boolean eraAuthorized;
    private String nihUsername;
    private String linkedIn;
    private String orcid;
    private String researcherGate;

    private String rus;
    @SerializedName(value = "nonTechRus", alternate = "non_tech_rus")
    private String nonTechRus;
    private Boolean diseases;
    private Boolean methods;
    private Boolean controls;
    private Boolean population;
    private Boolean other;
    private String otherText;
    private List<OntologyEntry> ontologies;
    private Boolean forProfit;
    @SerializedName(value = "oneGender", alternate = "onegender")
    private Boolean oneGender;
    private String gender;
    private Boolean pediatric;
    @SerializedName(value = "illegalBehavior", alternate = "illegalbehave")
    private Boolean illegalBehavior;
    private Boolean addiction;
    @SerializedName(value = "sexualDiseases", alternate = "sexualdiseases")
    private Boolean sexualDiseases;
    @SerializedName(value = "stigmatizedDiseases", alternate = "stigmatizediseases")
    private Boolean stigmatizedDiseases;
    @SerializedName(value = "vulnerablePopulation", alternate = "vulnerablepop")
    private Boolean vulnerablePopulation;
    @SerializedName(value = "populationMigration", alternate = "popmigration")
    private Boolean populationMigration;
    @SerializedName(value = "psychiatricTraits", alternate = "psychtraits")
    private Boolean psychiatricTraits;
    @SerializedName(value = "notHealth", alternate = "nothealth")
    private Boolean notHealth;
    private Boolean hmb;
    private String status;
    private Boolean poa;
    private List<DatasetEntry> datasets;
    @SerializedName(value = "darCode", alternate = "dar_code")
    private String darCode;
    @SerializedName(value = "partialDarCode", alternate = "partial_dar_code")
    private String partialDarCode;
    private Object restriction;
    @SerializedName(value = "validRestriction", alternate = "valid_restriction")
    private Boolean validRestriction;
    private String translatedUseRestriction;
    private Long createDate;
    private Long sortDate;
    @SerializedName(value = "datasetIds", alternate = {"datasetId", "datasetid"})
    private List<Integer> datasetIds;
    private List<DatasetDetailEntry> datasetDetail;

    private Boolean anvilUse;
    private Boolean cloudUse;
    private Boolean localUse;
    private String cloudProvider;
    private String cloudProviderType;
    private String cloudProviderDescription;
    private Boolean geneticStudiesOnly;
    private Boolean irb;
    private String irbDocumentLocation;
    private String irbDocumentName;
    private String irbProtocolExpiration;
    private String itDirector;
    private String signingOfficial;
    private Boolean publication;
    private Boolean collaboration;
    private String collaborationLetterLocation;
    private String collaborationLetterName;
    private Boolean forensicActivities;
    private Boolean sharingDistribution;
    private List<Collaborator> labCollaborators;
    private List<Collaborator> internalCollaborators;
    private List<Collaborator> externalCollaborators;
    private Boolean dsAcknowledgement;
    private Boolean gsoAcknowledgement;
    private Boolean pubAcknowledgement;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static DataAccessRequestData fromString(String jsonString) {
        DataAccessRequestData data = new Gson().fromJson(jsonString, DataAccessRequestData.class);
        validateOntologyEntries(data);
        return data;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getInvestigator() {
        return investigator;
    }

    public void setInvestigator(String investigator) {
        this.investigator = investigator;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public Boolean getCheckCollaborator() {
        return checkCollaborator;
    }

    public void setCheckCollaborator(Boolean checkCollaborator) {
        this.checkCollaborator = checkCollaborator;
    }

    public String getResearcher() {
        return researcher;
    }

    public void setResearcher(String researcher) {
        this.researcher = researcher;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getIsThePi() {
        return isThePi;
    }

    public void setIsThePi(String isThePi) {
        this.isThePi = isThePi;
    }

    public String getHavePi() {
        return havePi;
    }

    public void setHavePi(String havePi) {
        this.havePi = havePi;
    }

    public String getPiEmail() {
        return piEmail;
    }

    public void setPiEmail(String piEmail) {
        this.piEmail = piEmail;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getPubmedId() {
        return pubmedId;
    }

    public void setPubmedId(String pubmedId) {
        this.pubmedId = pubmedId;
    }

    public String getScientificUrl() {
        return scientificUrl;
    }

    public void setScientificUrl(String scientificUrl) {
        this.scientificUrl = scientificUrl;
    }

    public Boolean getEraExpiration() {
        return eraExpiration;
    }

    public void setEraExpiration(Boolean eraExpiration) {
        this.eraExpiration = eraExpiration;
    }

    public String getAcademicEmail() {
        return academicEmail;
    }

    public void setAcademicEmail(String academicEmail) {
        this.academicEmail = academicEmail;
    }

    public Boolean getEraAuthorized() {
        return eraAuthorized;
    }

    public void setEraAuthorized(Boolean eraAuthorized) {
        this.eraAuthorized = eraAuthorized;
    }

    public String getRus() {
        return rus;
    }

    public void setRus(String rus) {
        this.rus = rus;
    }

    public String getNonTechRus() {
        return nonTechRus;
    }

    public void setNonTechRus(String nonTechRus) {
        this.nonTechRus = nonTechRus;
    }

    public Boolean getDiseases() {
        return diseases;
    }

    public void setDiseases(Boolean diseases) {
        this.diseases = diseases;
    }

    public Boolean getMethods() {
        return methods;
    }

    public void setMethods(Boolean methods) {
        this.methods = methods;
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

    public Boolean getOther() {
        return other;
    }

    public void setOther(Boolean other) {
        this.other = other;
    }

    public String getOtherText() {
        return otherText;
    }

    public void setOtherText(String otherText) {
        this.otherText = otherText;
    }

    public List<OntologyEntry> getOntologies() {
        return ontologies;
    }

    public void setOntologies(List<OntologyEntry> ontologies) {
        this.ontologies = ontologies;
    }

    public Boolean getForProfit() {
        return forProfit;
    }

    public void setForProfit(Boolean forProfit) {
        this.forProfit = forProfit;
    }

    public Boolean getOneGender() {
        return oneGender;
    }

    public void setOneGender(Boolean oneGender) {
        this.oneGender = oneGender;
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

    public Boolean getStigmatizedDiseases() {
        return stigmatizedDiseases;
    }

    public void setStigmatizedDiseases(Boolean stigmatizedDiseases) {
        this.stigmatizedDiseases = stigmatizedDiseases;
    }

    public Boolean getVulnerablePopulation() {
        return vulnerablePopulation;
    }

    public void setVulnerablePopulation(Boolean vulnerablePopulation) {
        this.vulnerablePopulation = vulnerablePopulation;
    }

    public Boolean getPopulationMigration() {
        return populationMigration;
    }

    public void setPopulationMigration(Boolean populationMigration) {
        this.populationMigration = populationMigration;
    }

    public Boolean getPsychiatricTraits() {
        return psychiatricTraits;
    }

    public void setPsychiatricTraits(Boolean psychiatricTraits) {
        this.psychiatricTraits = psychiatricTraits;
    }

    public Boolean getNotHealth() {
        return notHealth;
    }

    public void setNotHealth(Boolean notHealth) {
        this.notHealth = notHealth;
    }

    public Boolean getHmb() {
        return hmb;
    }

    public void setHmb(Boolean hmb) {
        this.hmb = hmb;
    }

    public List<DatasetEntry> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<DatasetEntry> datasets) {
        this.datasets = datasets;
    }

    public String getDarCode() {
        return darCode;
    }

    public void setDarCode(String darCode) {
        this.darCode = darCode;
    }

    public String getPartialDarCode() {
        return partialDarCode;
    }

    public void setPartialDarCode(String partialDarCode) {
        this.partialDarCode = partialDarCode;
    }

    public Object getRestriction() {
        return restriction;
    }

    public void setRestriction(Object restriction) {
        this.restriction = restriction;
    }

    public Boolean getValidRestriction() {
        return validRestriction;
    }

    public void setValidRestriction(Boolean validRestriction) {
        this.validRestriction = validRestriction;
    }

    public String getTranslatedUseRestriction() {
        return translatedUseRestriction;
    }

    public void setTranslatedUseRestriction(String translatedUseRestriction) {
        this.translatedUseRestriction = translatedUseRestriction;
    }

    public Long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    public Long getSortDate() {
        return sortDate;
    }

    public void setSortDate(Long sortDate) {
        this.sortDate = sortDate;
    }

    public List<Integer> getDatasetIds() {
        return datasetIds;
    }

    public void setDatasetIds(List<Integer> datasetIds) {
        this.datasetIds = datasetIds;
    }

    public List<DatasetDetailEntry> getDatasetDetail() {
        return datasetDetail;
    }

    public void setDatasetDetail(List<DatasetDetailEntry> datasetDetail) {
        this.datasetDetail = datasetDetail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNihUsername() {
        return nihUsername;
    }

    public void setNihUsername(String nihUsername) {
        this.nihUsername = nihUsername;
    }

    public String getLinkedIn() {
        return linkedIn;
    }

    public void setLinkedIn(String linkedIn) {
        this.linkedIn = linkedIn;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getResearcherGate() {
        return researcherGate;
    }

    public void setResearcherGate(String researcherGate) {
        this.researcherGate = researcherGate;
    }

    public Boolean getPoa() {
        return poa;
    }

    public void setPoa(Boolean poa) {
        this.poa = poa;
    }

    public Boolean getCloudUse() {
        return cloudUse;
    }

    public void setCloudUse(Boolean cloudUse) {
        this.cloudUse = cloudUse;
    }

    public Boolean getAnvilUse() {
        return anvilUse;
    }

    public void setAnvilUse(Boolean anvilUse) {
        this.anvilUse = anvilUse;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getCloudProviderType() {
        return cloudProviderType;
    }

    public void setCloudProviderType(String cloudProviderType) {
        this.cloudProviderType = cloudProviderType;
    }

    public Boolean getGeneticStudiesOnly() {
        return geneticStudiesOnly;
    }

    public void setGeneticStudiesOnly(Boolean geneticStudiesOnly) {
        this.geneticStudiesOnly = geneticStudiesOnly;
    }

    public Boolean getIrb() {
        return irb;
    }

    public void setIrb(Boolean irb) {
        this.irb = irb;
    }

    public String getIrbDocumentLocation() {
        return irbDocumentLocation;
    }

    public void setIrbDocumentLocation(String irbDocumentLocation) {
        this.irbDocumentLocation = irbDocumentLocation;
    }

    public String getIrbDocumentName() {
        return irbDocumentName;
    }

    public void setIrbDocumentName(String irbDocumentName) {
        this.irbDocumentName = irbDocumentName;
    }

    public String getIrbProtocolExpiration() {
        return irbProtocolExpiration;
    }

    public void setIrbProtocolExpiration(String irbProtocolExpiration) {
        this.irbProtocolExpiration = irbProtocolExpiration;
    }

    public Boolean getPublication() {
        return publication;
    }

    public void setPublication(Boolean publication) {
        this.publication = publication;
    }

    public Boolean getCollaboration() {
        return collaboration;
    }

    public void setCollaboration(Boolean collaboration) {
        this.collaboration = collaboration;
    }

    public String getCollaborationLetterLocation() {
        return collaborationLetterLocation;
    }

    public void setCollaborationLetterLocation(String collaborationLetterLocation) {
        this.collaborationLetterLocation = collaborationLetterLocation;
    }

    public String getCollaborationLetterName() {
        return collaborationLetterName;
    }

    public void setCollaborationLetterName(String collaborationLetterName) {
        this.collaborationLetterName = collaborationLetterName;
    }

    public Boolean getForensicActivities() {
        return forensicActivities;
    }

    public void setForensicActivities(Boolean forensicActivities) {
        this.forensicActivities = forensicActivities;
    }

    public Boolean getSharingDistribution() {
        return sharingDistribution;
    }

    public void setSharingDistribution(Boolean sharingDistribution) {
        this.sharingDistribution = sharingDistribution;
    }

    public List<Collaborator> getLabCollaborators() {
        return labCollaborators;
    }

    public void setLabCollaborators(
        List<Collaborator> labCollaborators) {
        this.labCollaborators = labCollaborators;
    }

    public List<Collaborator> getInternalCollaborators() {
        return internalCollaborators;
    }

    public void setInternalCollaborators(
        List<Collaborator> internalCollaborators) {
        this.internalCollaborators = internalCollaborators;
    }

    public List<Collaborator> getExternalCollaborators() {
        return externalCollaborators;
    }

    public void setExternalCollaborators(
        List<Collaborator> externalCollaborators) {
        this.externalCollaborators = externalCollaborators;
    }

    public Boolean getLocalUse() {
        return localUse;
    }

    public void setLocalUse(Boolean localUse) {
        this.localUse = localUse;
    }

    public String getCloudProviderDescription() {
        return cloudProviderDescription;
    }

    public void setCloudProviderDescription(String cloudProviderDescription) {
        this.cloudProviderDescription = cloudProviderDescription;
    }

    public String getItDirector() {
        return itDirector;
    }

    public void setItDirector(String itDirector) {
        this.itDirector = itDirector;
    }

    public String getSigningOfficial() {
        return signingOfficial;
    }

    public void setSigningOfficial(String signingOfficial) {
        this.signingOfficial = signingOfficial;
    }

    public void setDSAcknowledgement(Boolean dsAcknowledgement) {
        this.dsAcknowledgement = dsAcknowledgement;
    }

    public Boolean getDSAcknowledgement() {
        return dsAcknowledgement;
    }

    public void setGSOAcknowledgement(Boolean gsoAcknowledgement) {
        this.gsoAcknowledgement = gsoAcknowledgement;
    }

    public Boolean getGSOAcknowledgement() {
        return gsoAcknowledgement;
    }

    public void setPubAcknowledgement(Boolean pubAcknowledgement) {
        this.pubAcknowledgement = pubAcknowledgement;
    }

    public Boolean getPubAcknowledgement() {
        return pubAcknowledgement;
    }

    // Validate all ontology entries
    private static void validateOntologyEntries(DataAccessRequestData data) {
        if (Objects.nonNull(data) &&
            Objects.nonNull(data.getOntologies())
            && !data.getOntologies().isEmpty()) {
            List<OntologyEntry> filteredEntries =
                data.getOntologies().stream()
                    .filter(Objects::nonNull)
                    .filter(e -> Objects.nonNull(e.getId()))
                    .filter(e -> Objects.nonNull(e.getLabel()))
                    .filter(e -> Objects.nonNull(e.getDefinition()))
                    .collect(Collectors.toList());
            if (filteredEntries.isEmpty()) {
                data.setOntologies(Collections.emptyList());
            } else {
                data.setOntologies(filteredEntries);
            }
        }
    }
}
