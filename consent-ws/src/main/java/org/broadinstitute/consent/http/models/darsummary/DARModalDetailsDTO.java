package org.broadinstitute.consent.http.models.darsummary;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
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
    private List<SummaryItem> researchType;
    private List<String> diseases;
    private List<SummaryItem> purposeStatements;
    private boolean isThereDiseases;
    private boolean isTherePurposeStatements;
    private boolean sensitivePopulation = false;
    private boolean requiresManualReview = false;

    @JsonProperty
    private Map<String, String> datasetDetail;
    private String needDOApproval = "";

    public DARModalDetailsDTO(Document darDocument, DACUser owner, ElectionAPI electionAPI, String status, String rationale){
        this(darDocument);
        setNeedDOApproval(electionAPI.darDatasetElectionStatus((darDocument.get(DarConstants.ID).toString())));
        setResearcherName(owner, darDocument.getString(DarConstants.INVESTIGATOR));
        setStatus(status);
        setRationale(rationale);
    }

    public DARModalDetailsDTO(Document darDocument){
        setDarCode(darDocument.getString(DarConstants.DAR_CODE));
        setPrincipalInvestigator(darDocument.getString(DarConstants.INVESTIGATOR));
        setInstitutionName(this.institutionName = darDocument.getString(DarConstants.INSTITUTION));
        setProjectTitle(this.projectTitle = darDocument.getString(DarConstants.PROJECT_TITLE));
        setIsThereDiseases(false);
        setIsTherePurposeStatements(false);
        setResearchType(generateResearchTypeSummary(darDocument));
        setDiseases(generateDiseasesSummary(darDocument));
        setPurposeStatements(generatePurposeStatementsSummary(darDocument));
        setDatasetDetail((ArrayList<Document>) darDocument.get(DarConstants.DATASET_DETAIL));
    }

    public String getNeedDOApproval() {
        return needDOApproval;
    }

    public void setNeedDOApproval(String needDOApproval) {
        this.needDOApproval = needDOApproval;
    }

    public String getResearcherName() {
        return researcherName;
    }

    public void setResearcherName(DACUser owner, String principalInvestigator) {
        if(owner.getDisplayName().equals(principalInvestigator)){
            researcherName = principalInvestigator;
        } else {
            researcherName = owner.getDisplayName();
        }
    }

    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }

    private void manualReviewIsTrue(){
        this.requiresManualReview = true;
    }
    private void sensitivePopulationIsTrue(){
        this.sensitivePopulation = true;
    }

    public boolean isSensitivePopulation() {
        return sensitivePopulation;
    }

    private List<SummaryItem> generatePurposeStatementsSummary(Document darDocument) {
        List<SummaryItem> psList = new ArrayList<>();
        if(darDocument.getBoolean("forProfit")){
            psList.add(new SummaryItem(SummaryConstants.PS_FOR_PROFIT, false));
        }
        if(darDocument.getBoolean("onegender")) {
            if (darDocument.getString("gender").equals("F")) {
                psList.add(new SummaryItem("Gender: ", SummaryConstants.PS_GENDER_FEMALE, false));
            } else {
                psList.add(new SummaryItem("Gender: ", SummaryConstants.PS_GENDER_MALE, false));
            }
        }
        if(darDocument.getBoolean("pediatric")){
            psList.add(new SummaryItem("Pediatric: ", SummaryConstants.PS_PEDIATRIC_STUDY, false));
        }
        if(darDocument.getBoolean("illegalbehave")){
            psList.add(new SummaryItem("Illegal Behavior: ", SummaryConstants.PS_ILLEGAL_BEHAVIOR_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("addiction")){
            psList.add(new SummaryItem("Addiction: ", SummaryConstants.PS_ADDICTIONS_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("sexualdiseases")){
            psList.add(new SummaryItem("Sexual Diseases: ", SummaryConstants.PS_SEXUAL_DISEASES_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("stigmatizediseases")){
            psList.add(new SummaryItem("Stigmatized Diseases: ", SummaryConstants.PS_STIGMATIZING_ILLNESSES_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("vulnerablepop")){
            psList.add(new SummaryItem("Vulnerable Population: ", SummaryConstants.PS_VULNERABLE_POPULATION_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("popmigration")){
            psList.add(new SummaryItem("population Migration: ", SummaryConstants.PS_POPULATION_MIGRATION_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("psychtraits")){
            psList.add(new SummaryItem("Psycological Traits: ", SummaryConstants.PS_PSYCOLOGICAL_TRAITS_STUDY, true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("nothealth")){
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
        List<Map<String, String>> ontologies = (List<Map<String, String>>) darDocument.get("ontologies");
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
        if(darDocument.containsKey("diseases") && darDocument.getBoolean("diseases")){
            researchList.add(new SummaryItem(SummaryConstants.RT_DISEASE_RELATED, SummaryConstants.RT_DISEASE_RELATED_DETAIL));
        }
        if(darDocument.containsKey("methods") && darDocument.getBoolean("methods")){
            researchList.add(new SummaryItem(SummaryConstants.RT_METHODS_DEVELOPMENT, SummaryConstants.RT_METHODS_DEVELOPMENT_DETAIL));
        }
        if(darDocument.containsKey("controls") && darDocument.getBoolean("controls")){
            researchList.add(new SummaryItem(SummaryConstants.RT_CONTROLS, SummaryConstants.RT_CONTROLS_DETAIL));
        }
        if(darDocument.containsKey("population") && darDocument.getBoolean("population")){
            researchList.add(new SummaryItem(SummaryConstants.RT_POPULATION, SummaryConstants.RT_POPULATION_DETAIL));
            manualReviewIsTrue();
        }
        if(darDocument.containsKey("other") && darDocument.getBoolean("other")){
            researchList.add(new SummaryItem(SummaryConstants.RT_OTHER, darDocument.getString("othertext")));
            manualReviewIsTrue();
        }
        return researchList;
    }

    public void setDarCode(String darCode) {
        this.darCode = darCode;
    }

    public String getDarCode() {
        return darCode;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public List<SummaryItem> getResearchType() {
        return researchType;
    }

    public void setResearchType(List<SummaryItem> researchType) {
        this.researchType = researchType;
    }

    public List<String> getDiseases() {
        return diseases;
    }

    public void setDiseases(List<String> diseases) {
        this.diseases = diseases;
    }

    public List<SummaryItem> getPurposeStatements() {
        return purposeStatements;
    }

    public void setPurposeStatements(List<SummaryItem> purposeStatements) {
        this.purposeStatements = purposeStatements;
    }

    public boolean isThereDiseases() {
        return isThereDiseases;
    }

    public void setIsThereDiseases(boolean isThereDiseases) {
        this.isThereDiseases = isThereDiseases;
    }

    public boolean isTherePurposeStatements() {
        return isTherePurposeStatements;
    }

    public void setIsTherePurposeStatements(boolean isTherePurposeStatements) {
        this.isTherePurposeStatements = isTherePurposeStatements;
    }

    public void setDatasetDetail(ArrayList<Document> datasetDetail) {
        Map<String, String> datasetDetailMap = new HashMap<>();
        datasetDetail.forEach((doc) -> datasetDetailMap.put(doc.getString(DarConstants.DATASET_ID),doc.getString("name")));
        this.datasetDetail = datasetDetailMap;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}

