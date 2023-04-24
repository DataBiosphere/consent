package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonInclude(Include.NON_NULL)
public class DataAccessRequestData {

    /**
     * These properties are deprecated and should no longer be used.
     * In many cases, they represent user properties, consent related properties,
     * deprecated properties, or duplicate existing DAR fields.
     * See <a href="https://broadworkbench.atlassian.net/browse/DUOS-728">DUOS-728</a> for more info.
     */
    public static final List<String> DEPRECATED_PROPS = Arrays
        .asList("referenceId", "investigator",
            "institution", "department", "address1", "address2", "city", "zipcode", "zipCode",
            "state", "country", "researcher", "userId", "isThePi", "havePi", "piEmail",
            "profileName", "pubmedId", "scientificUrl", "eraExpiration", "academicEmail",
            "eraAuthorized", "nihUsername", "linkedIn", "orcid", "researcherGate", "datasetDetail",
            "datasets", "datasetId", "validRestriction", "restriction", "translatedUseRestriction",
            "createDate", "sortDate", "additionalEmail", "checkNotifications", "partialDarCode" );

    @Deprecated
    private String referenceId;
    private String projectTitle;
    private Boolean checkCollaborator;
    private Boolean checkNihDataOnly;
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
    private Object restriction;
    @SerializedName(value = "validRestriction", alternate = "valid_restriction")
    private Boolean validRestriction;
    private String translatedUseRestriction;
    @Deprecated
    private Long createDate;
    @Deprecated
    private Long sortDate;
    @Deprecated
    @SerializedName(value = "datasetIds", alternate = {"datasetId", "datasetid"})
    private List<Integer> datasetIds;

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
    private Boolean dsAcknowledgment;
    private Boolean gsoAcknowledgment;
    private Boolean pubAcknowledgment;
    private String piName;

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

    public Boolean getCheckNihDataOnly() {
        return checkNihDataOnly;
    }

    public void setCheckNihDataOnly(Boolean checkNihDataOnly) {
        this.checkNihDataOnly = checkNihDataOnly;
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
        if (Objects.isNull(ontologies)) {
            return Collections.emptyList();
        }
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
        if (Objects.isNull(datasets)) {
            return Collections.emptyList();
        }
        return datasets;
    }

    public void setDatasets(List<DatasetEntry> datasets) {
        this.datasets = datasets;
    }

    @Deprecated
    public String getDarCode() {
        return darCode;
    }

    @Deprecated
    public void setDarCode(String darCode) {
        this.darCode = darCode;
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

    /**
     * Used for the initial population of datasets a DAR is associated to.
     * Intended to be used solely for simpler construction from the UI.
     *
     * @return List of dataset ids associated to the DAR.
     */
    public List<Integer> getDatasetIds() {
        if (Objects.isNull(datasetIds)) {
            return Collections.emptyList();
        }
        return datasetIds;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        if (Objects.isNull(labCollaborators)) {
            return Collections.emptyList();
        }
        return labCollaborators;
    }

    public void setLabCollaborators(
        List<Collaborator> labCollaborators) {
        this.labCollaborators = labCollaborators;
    }

    public List<Collaborator> getInternalCollaborators() {
        if (Objects.isNull(internalCollaborators)) {
            return Collections.emptyList();
        }
        return internalCollaborators;
    }

    public void setInternalCollaborators(
        List<Collaborator> internalCollaborators) {
        this.internalCollaborators = internalCollaborators;
    }

    public List<Collaborator> getExternalCollaborators() {
        if (Objects.isNull(externalCollaborators)) {
            return Collections.emptyList();
        }
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

    public void setDSAcknowledgment(Boolean dsAcknowledgment) {
        this.dsAcknowledgment = dsAcknowledgment;
    }

    public Boolean getDSAcknowledgment() {
        return dsAcknowledgment;
    }

    public void setGSOAcknowledgment(Boolean gsoAcknowledgment) {
        this.gsoAcknowledgment = gsoAcknowledgment;
    }

    public Boolean getGSOAcknowledgment() {
        return gsoAcknowledgment;
    }

    public void setPubAcknowledgment(Boolean pubAcknowledgment) {
        this.pubAcknowledgment = pubAcknowledgment;
    }

    public Boolean getPubAcknowledgment() {
        return pubAcknowledgment;
    }

    public String getPiName() {
        return piName;
    }

    public void setPiName(String piName) {
        this.piName = piName;
    }

    // Validate all ontology entries
    private static void validateOntologyEntries(DataAccessRequestData data) {
        if (Objects.nonNull(data)
            && !data.getOntologies().isEmpty()) {
            List<OntologyEntry> filteredEntries =
                data.getOntologies().stream()
                    .filter(Objects::nonNull)
                    .filter(e -> Objects.nonNull(e.getId()))
                    .filter(e -> Objects.nonNull(e.getLabel()))
                    .collect(Collectors.toList());
            if (filteredEntries.isEmpty()) {
                data.setOntologies(Collections.emptyList());
            } else {
                data.setOntologies(filteredEntries);
            }
        }
    }
}
