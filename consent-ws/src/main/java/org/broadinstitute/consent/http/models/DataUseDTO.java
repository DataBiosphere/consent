package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unused", "SameParameterValue"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataUseDTO {

    private static final Logger logger = Logger.getLogger(DataUseDTO.class);

    private Boolean generalUse;
    private Boolean hmbResearch;
    private List<String> diseaseRestrictions;
    private Boolean populationOriginsAncestry;
    private Boolean populationStructure;
    private Boolean commercialUse;
    private Boolean methodsResearch;
    private String aggregateResearch;
    private String controlSetOption;
    private String gender;
    private Boolean pediatric;
    private List<String> populationRestrictions;
    private Boolean otherRestrictions;
    private String dateRestriction;
    private Boolean recontactingDataSubjects;
    private String recontactMay;
    private String recontactMust;
    private String genomicPhenotypicData;
    private String cloudStorage;
    private Boolean ethicsApprovalRequired;
    private String geographicalRestrictions;
    private String other;
    private Boolean illegalBehavior;
    private Boolean addiction;
    private Boolean sexualDiseases;
    private Boolean stigmatizeDiseases;
    private Boolean vulnerablePopulations;
    private Boolean psychologicalTraits;
    private Boolean nonBiomedical;

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

    public Boolean getCommercialUse() {
        return commercialUse;
    }

    public void setCommercialUse(Boolean commercialUse) {
        this.commercialUse = commercialUse;
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

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

    public static Optional<DataUseDTO> parseDataUse(String str) {
        if (str == null || str.isEmpty()) {
            return Optional.empty();
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectReader reader = mapper.readerFor(DataUseDTO.class);
                return Optional.of(reader.readValue(str));
            } catch (IOException e) {
                logger.error(String.format("DataUseDTO parse exception on \"%s\"", str));
                return Optional.empty();
            }
        }
    }

}
