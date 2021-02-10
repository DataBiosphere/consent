package org.broadinstitute.consent.http.models.darsummary;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DARModalDetailsDTO {

    private String darCode;
    private String principalInvestigator;
    private String researcherName = "";
    private String institutionName;
    private String projectTitle;
    private String status;
    private String rationale;
    private String department;
    private String city;
    private String country;
    private String nihUsername;
    private Boolean haveNihUsername;
    private List<SummaryItem> researchType;
    private List<String> diseases;
    private List<SummaryItem> purposeStatements;
    private boolean isThereDiseases;
    private boolean isTherePurposeStatements;
    private boolean sensitivePopulation = false;
    private boolean requiresManualReview = false;
    private Integer userId;
    private String needDOApproval = "";
    private List<DataSet> datasets;
    private List<UserProperty> researcherProperties;
    private String rus;

    public DARModalDetailsDTO() {}

    public String getNeedDOApproval() {
        return needDOApproval;
    }

    public DARModalDetailsDTO setNeedDOApproval(String needDOApproval) {
        this.needDOApproval = needDOApproval;
        return this;
    }

    public String getResearcherName() {
        return researcherName;
    }

    public DARModalDetailsDTO setResearcherName(User owner, String principalInvestigator) {
        if (owner == null) {
            return this;
        }
        if (owner.getDisplayName().equals(principalInvestigator)) {
            researcherName = principalInvestigator;
        } else {
            researcherName = owner.getDisplayName();
        }
        return this;
    }

    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }

    private DARModalDetailsDTO manualReviewIsTrue(){
        this.requiresManualReview = true;
        return this;
    }
    private DARModalDetailsDTO sensitivePopulationIsTrue(){
        this.sensitivePopulation = true;
        return this;
    }

    public boolean isSensitivePopulation() {
        return sensitivePopulation;
    }

    public List<SummaryItem> generatePurposeStatementsSummary(Document darDocument) {
        List<SummaryItem> psList = new ArrayList<>();
        if(darDocument.getBoolean(DarConstants.FOR_PROFIT)){
            psList.add(new SummaryItem(SummaryConstants.PS_FOR_PROFIT, false));
        }
        if(darDocument.getBoolean(DarConstants.ONE_GENDER)) {
            if (darDocument.getString(DarConstants.GENDER).equalsIgnoreCase("F")) {
                psList.add(new SummaryItem("Gender: ", SummaryConstants.PS_GENDER_FEMALE, false));
            } else {
                psList.add(new SummaryItem("Gender: ", SummaryConstants.PS_GENDER_MALE, false));
            }
        }
        if(darDocument.getBoolean(DarConstants.PEDIATRIC)){
            psList.add(new SummaryItem("Pediatric: ", SummaryConstants.PS_PEDIATRIC_STUDY, false));
        }
        if(darDocument.getBoolean(DarConstants.ILLEGAL_BEHAVE)){
            psList.add(new SummaryItem("Illegal Behavior: ", SummaryConstants.PS_ILLEGAL_BEHAVIOR_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean(DarConstants.ADDICTION)){
            psList.add(new SummaryItem("Addiction: ", SummaryConstants.PS_ADDICTIONS_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean(DarConstants.SEXUAL_DISEASES)){
            psList.add(new SummaryItem("Sexual Diseases: ", SummaryConstants.PS_SEXUAL_DISEASES_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean(DarConstants.STIGMATIZED_DISEASES)){
            psList.add(new SummaryItem("Stigmatized Diseases: ", SummaryConstants.PS_STIGMATIZING_ILLNESSES_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean(DarConstants.VULNERABLE_POP)){
            psList.add(new SummaryItem("Vulnerable Population: ", SummaryConstants.PS_VULNERABLE_POPULATION_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean(DarConstants.POP_MIGRATION)){
            psList.add(new SummaryItem("Population Migration: ", SummaryConstants.PS_POPULATION_MIGRATION_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean(DarConstants.PSYCH_TRAITS)){
            psList.add(new SummaryItem("Psychological Traits: ", SummaryConstants.PS_PSYCOLOGICAL_TRAITS_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean(DarConstants.NOT_HEALTH)){
            psList.add(new SummaryItem("Not Health Related: ", SummaryConstants.PS_NOT_HEALT_RELATED_STUDY
                    , true));
            sensitivePopulationIsTrue();
        }
        if(!CollectionUtils.isEmpty(psList)){
            setIsTherePurposeStatements(true);
        }
        return psList;
    }

    private List<String> generateDiseasesSummary(Document darDocument) {
        List<Map<String, String>> ontologies = (List<Map<String, String>>) darDocument.get(DarConstants.ONTOLOGIES);
        List<String> diseases = new ArrayList<>();
        if(!CollectionUtils.isEmpty(ontologies)) {
            setIsThereDiseases(true);
            for (Map<String, String> ontology : ontologies) {
                diseases.add(ontology.get("label"));
            }
        }
        return diseases;
    }

    private List<SummaryItem> generateResearchTypeSummary(Document darDocument) {
        List<SummaryItem> researchList = new ArrayList<>();
        if(darDocument.containsKey(DarConstants.DISEASES) && darDocument.getBoolean(DarConstants.DISEASES)){
            researchList.add(new SummaryItem(SummaryConstants.RT_DISEASE_RELATED, SummaryConstants.RT_DISEASE_RELATED_DETAIL, false));
        }
        if(darDocument.containsKey(DarConstants.METHODS) && darDocument.getBoolean(DarConstants.METHODS)){
            researchList.add(new SummaryItem(SummaryConstants.RT_METHODS_DEVELOPMENT, SummaryConstants.RT_METHODS_DEVELOPMENT_DETAIL, false));
        }
        if(darDocument.containsKey(DarConstants.CONTROLS) && darDocument.getBoolean(DarConstants.CONTROLS)){
            researchList.add(new SummaryItem(SummaryConstants.RT_CONTROLS, SummaryConstants.RT_CONTROLS_DETAIL, false));
        }
        if(darDocument.containsKey(DarConstants.POPULATION) && darDocument.getBoolean(DarConstants.POPULATION)){
            researchList.add(new SummaryItem(SummaryConstants.RT_POPULATION, SummaryConstants.RT_POPULATION_DETAIL, true));
            manualReviewIsTrue();
        }
        if(darDocument.containsKey(DarConstants.HMB) && darDocument.getBoolean(DarConstants.HMB)){
            researchList.add(new SummaryItem(SummaryConstants.RT_HEALTH_BIOMEDICAL, SummaryConstants.RT_HEALTH_BIOMEDICAL_DETAIL, false));
        }
        if(darDocument.containsKey(DarConstants.POA) && darDocument.getBoolean(DarConstants.POA)){
            researchList.add(new SummaryItem(SummaryConstants.RT_POPULATION_ORIGINS, SummaryConstants.RT_POPULATION_ORIGINS_DETAIL, true));
            manualReviewIsTrue();
        }
        if(darDocument.containsKey(DarConstants.OTHER) && darDocument.getBoolean(DarConstants.OTHER)){
            researchList.add(new SummaryItem(SummaryConstants.RT_OTHER, darDocument.getString(DarConstants.OTHER_TEXT), true));
            manualReviewIsTrue();
        }
        return researchList;
    }

    public DARModalDetailsDTO setDarCode(String darCode) {
        this.darCode = darCode;
        return this;
    }

    public String getDarCode() {
        return darCode;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public DARModalDetailsDTO setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
        return this;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public DARModalDetailsDTO setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
        return this;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public DARModalDetailsDTO setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
        return this;
    }

    public List<SummaryItem> getResearchType() {
        return researchType;
    }

    public DARModalDetailsDTO setResearchType(Document dar) {
        this.researchType = generateResearchTypeSummary(dar);
        return this;
    }

    public List<String> getDiseases() {
        return diseases;
    }

    public DARModalDetailsDTO setDiseases(Document darDocument) {
        this.diseases = generateDiseasesSummary(darDocument);
        return this;
    }

    public List<SummaryItem> getPurposeStatements() {
        return purposeStatements;
    }

    public DARModalDetailsDTO setPurposeStatements(Document darDocuement) {
        this.purposeStatements = generatePurposeStatementsSummary(darDocuement);
        return this;
    }

    public boolean isThereDiseases() {
        return isThereDiseases;
    }

    public DARModalDetailsDTO setIsThereDiseases(boolean isThereDiseases) {
        this.isThereDiseases = isThereDiseases;
        return this;
    }

    public boolean isTherePurposeStatements() {
        return isTherePurposeStatements;
    }

    public DARModalDetailsDTO setIsTherePurposeStatements(boolean isTherePurposeStatements) {
        this.isTherePurposeStatements = isTherePurposeStatements;
        return this;
    }

    public List<DataSet> getDatasets() {
        return datasets;
    }

    public DARModalDetailsDTO setDatasets(List<DataSet> datasets) {
        this.datasets = datasets;
        return this;
    }

    public List<UserProperty> getResearcherProperties() {
        return researcherProperties;
    }

    public DARModalDetailsDTO setResearcherProperties(List<UserProperty> researcherProperties) {
        this.researcherProperties = researcherProperties;
        return this;
    }

    public String getRus() {
        return rus;
    }

    public DARModalDetailsDTO setRus(String rus) {
        this.rus = rus;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public DARModalDetailsDTO setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getRationale() {
        return rationale;
    }

    public DARModalDetailsDTO setRationale(String rationale) {
        this.rationale = rationale;
        return this;
    }

    public Integer getUserId() {
        return userId;
    }

    public DARModalDetailsDTO setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    public String getDepartment() {
        return department;
    }

    public DARModalDetailsDTO setDepartment(String department) {
        this.department = department;
        return this;
    }

    public String getCity() {
        return city;
    }

    public DARModalDetailsDTO setCity(String city) {
        this.city = city;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public DARModalDetailsDTO setCountry(String country) {
        this.country = country;
        return this;
    }


    public Boolean getHaveNihUsername() {
        return haveNihUsername;
    }

    public DARModalDetailsDTO setHaveNihUsername(Boolean haveNihUsername) {
        this.haveNihUsername = haveNihUsername;
        return this;
    }

    public String getNihUsername() {
        return nihUsername;
    }

    public DARModalDetailsDTO setNihUsername(String nihUsername) {
        this.nihUsername = nihUsername;
        return this;
    }

}
