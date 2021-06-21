package org.broadinstitute.consent.http.models.darsummary;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.models.UserProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public DARModalDetailsDTO setResearcherName(String researcherName) {
      this.researcherName = researcherName;
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

    public List<SummaryItem> generatePurposeStatementsSummary(DataAccessRequest dar) {
        List<SummaryItem> psList = new ArrayList<>();
        if (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) {

            if (isTrue(dar.getData().getForProfit())) {
                psList.add(new SummaryItem(SummaryConstants.PS_FOR_PROFIT, false));
            }
            if (isTrue(dar.getData().getOneGender())) {
                if (dar.getData().getGender().equalsIgnoreCase("F")) {
                    psList.add(new SummaryItem("Gender: ", SummaryConstants.PS_GENDER_FEMALE, false));
                } else {
                    psList.add(new SummaryItem("Gender: ", SummaryConstants.PS_GENDER_MALE, false));
                }
            }
            if (isTrue(dar.getData().getPediatric())) {
                psList.add(new SummaryItem("Pediatric: ", SummaryConstants.PS_PEDIATRIC_STUDY, false));
            }
            if (isTrue(dar.getData().getIllegalBehavior())) {
                psList.add(new SummaryItem("Illegal Behavior: ", SummaryConstants.PS_ILLEGAL_BEHAVIOR_STUDY, true));
                sensitivePopulationIsTrue();
            }
            if (isTrue(dar.getData().getAddiction())) {
                psList.add(new SummaryItem("Addiction: ", SummaryConstants.PS_ADDICTIONS_STUDY, true));
                sensitivePopulationIsTrue();
            }
            if (isTrue(dar.getData().getSexualDiseases())) {
                psList.add(new SummaryItem("Sexual Diseases: ", SummaryConstants.PS_SEXUAL_DISEASES_STUDY, true));
                sensitivePopulationIsTrue();
            }
            if (isTrue(dar.getData().getStigmatizedDiseases())) {
                psList.add(new SummaryItem("Stigmatized Diseases: ", SummaryConstants.PS_STIGMATIZING_ILLNESSES_STUDY, true));
                sensitivePopulationIsTrue();
            }
            if (isTrue(dar.getData().getVulnerablePopulation())) {
                psList.add(new SummaryItem("Vulnerable Population: ", SummaryConstants.PS_VULNERABLE_POPULATION_STUDY, true));
                sensitivePopulationIsTrue();
            }
            if (isTrue(dar.getData().getPopulationMigration())) {
                psList.add(new SummaryItem("Population Migration: ", SummaryConstants.PS_POPULATION_MIGRATION_STUDY, true));
                sensitivePopulationIsTrue();
            }
            if (isTrue(dar.getData().getPsychiatricTraits())) {
                psList.add(new SummaryItem("Psychological Traits: ", SummaryConstants.PS_PSYCOLOGICAL_TRAITS_STUDY, true));
                sensitivePopulationIsTrue();
            }
            if (isTrue(dar.getData().getNotHealth())) {
                psList.add(new SummaryItem("Not Health Related: ", SummaryConstants.PS_NOT_HEALT_RELATED_STUDY
                  , true));
                sensitivePopulationIsTrue();
            }
        }
        if(!CollectionUtils.isEmpty(psList)){
            setIsTherePurposeStatements(true);
        }
        return psList;
    }

    private List<String> generateDiseasesSummary(DataAccessRequest dar) {
        List<String> diseases = new ArrayList<>();
        if (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) {
            List<OntologyEntry> ontologies =  dar.getData().getOntologies();
            if(!CollectionUtils.isEmpty(ontologies)) {
                setIsThereDiseases(true);
                for (OntologyEntry ontology : ontologies) {
                    diseases.add(ontology.getLabel());
                }
            }
        }
        return diseases;
    }

    private List<SummaryItem> generateResearchTypeSummary(DataAccessRequest dar) {
        List<SummaryItem> researchList = new ArrayList<>();
        if (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) {
            if (isTrue(dar.getData().getDiseases())) {
                researchList.add(new SummaryItem(SummaryConstants.RT_DISEASE_RELATED, SummaryConstants.RT_DISEASE_RELATED_DETAIL, false));
            }
            if (isTrue(dar.getData().getMethods())) {
                researchList.add(new SummaryItem(SummaryConstants.RT_METHODS_DEVELOPMENT, SummaryConstants.RT_METHODS_DEVELOPMENT_DETAIL, false));
            }
            if (isTrue(dar.getData().getControls())) {
                researchList.add(new SummaryItem(SummaryConstants.RT_CONTROLS, SummaryConstants.RT_CONTROLS_DETAIL, false));
            }
            if (isTrue(dar.getData().getPopulation())) {
                researchList.add(new SummaryItem(SummaryConstants.RT_POPULATION, SummaryConstants.RT_POPULATION_DETAIL, true));
                manualReviewIsTrue();
            }
            if (isTrue(dar.getData().getHmb())) {
                researchList.add(new SummaryItem(SummaryConstants.RT_HEALTH_BIOMEDICAL, SummaryConstants.RT_HEALTH_BIOMEDICAL_DETAIL, false));
            }
            if (isTrue(dar.getData().getPoa())) {
                researchList.add(new SummaryItem(SummaryConstants.RT_POPULATION_ORIGINS, SummaryConstants.RT_POPULATION_ORIGINS_DETAIL, true));
                manualReviewIsTrue();
            }
            if (isTrue(dar.getData().getOther())) {
                researchList.add(new SummaryItem(SummaryConstants.RT_OTHER, dar.getData().getOtherText(), true));
                manualReviewIsTrue();
            }
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

    public DARModalDetailsDTO setResearchType(DataAccessRequest dar) {
        this.researchType = generateResearchTypeSummary(dar);
        return this;
    }

    public List<String> getDiseases() {
        return diseases;
    }

    public DARModalDetailsDTO setDiseases(DataAccessRequest darDocument) {
        this.diseases = generateDiseasesSummary(darDocument);
        return this;
    }

    public List<SummaryItem> getPurposeStatements() {
        return purposeStatements;
    }

    public DARModalDetailsDTO setPurposeStatements(DataAccessRequest darDocuement) {
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

    private boolean isTrue(Boolean obj) {
        return BooleanUtils.isTrue(obj);
    }

}
