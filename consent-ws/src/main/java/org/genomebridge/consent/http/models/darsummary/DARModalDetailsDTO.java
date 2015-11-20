package org.genomebridge.consent.http.models.darsummary;

import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DARModalDetailsDTO {

    private String principalInvestigator;
    private String institutionName;
    private String projectTitle;
    private List<SummaryItem> researchType;
    private List<String> diseases;
    private List<SummaryItem> purposeStatements;
    private boolean isThereDiseases;
    private boolean isTherePurposeStatements;
    private boolean sensitivePopulation = false;
    private boolean requiresManualReview = false;

    public DARModalDetailsDTO(Document darDocument){
        setPrincipalInvestigator(darDocument.getString("investigator"));
        setInstitutionName(this.institutionName = darDocument.getString("institution"));
        setProjectTitle(this.projectTitle = darDocument.getString("projectTitle"));
        setIsThereDiseases(false);
        setIsTherePurposeStatements(false);
        setResearchType(generateResearchTypeSummary(darDocument));
        setDiseases(generateDiseasesSummary(darDocument));
        setPurposeStatements(generatePurposeStatementsSummary(darDocument));
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
            psList.add(new SummaryItem("The dataset will be used in a study related to a commercial purpose.", false));
        }
        if(darDocument.getBoolean("onegender")) {
            if (darDocument.getString("gender").equals("F")) {
                psList.add(new SummaryItem("The dataset will be used for the study of females", false));
            } else {
                psList.add(new SummaryItem("The dataset will be used for the study of males.", false));
            }
        }
        if(darDocument.getBoolean("pediatric")){
            psList.add(new SummaryItem("The dataset will  be used for the study of children.", false));
        }
        if(darDocument.getBoolean("illegalbehave")){
            psList.add(new SummaryItem("The dataset will be used for the study of illegal behaviors (violence, domestic abuse, prostitution, sexual victimization).", true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("addiction")){
            psList.add(new SummaryItem("The dataset will be used for the study of alcohol or drug abuse, or abuse of other addictive products.", true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("sexualdiseases")){
            psList.add(new SummaryItem("The dataset will be used for the study of sexual preferences or sexually transmitted diseases.", true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("stigmatizediseases")){
            psList.add(new SummaryItem("The dataset will be used for the study of stigmatizing illnesses.", true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("vulnerablepop")){
            psList.add(new SummaryItem("The dataset will be used for a study targeting a vulnerable population as defined in 456 CFR (children, prisoners, pregnant women, mentally disabled persons, or [SIGNIFICANTLY] economically or educationally disadvantaged persons).", true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("popmigration")){
            psList.add(new SummaryItem("The dataset will be used for the study of Population Origins/Migration patterns.", true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("psychtraits")){
            psList.add(new SummaryItem("The dataset will be used for the study of psychological traits, including intelligence, attention, emotion.", true));
            sensitivePopulationIsTrue();
        }
        if(darDocument.getBoolean("nothealth")){
            psList.add(new SummaryItem("The dataset will be used for the research that correlates  ethnicity, race, or gender with genotypic or other phenotypic variables, for purposes beyond biomedical or health-related research, or in ways may not be easily related to Health.", true));
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
        if(darDocument.containsKey("diseases")){
            researchList.add(new SummaryItem("Disease-related studies:", " The primary purpose of the research is to learn more about a particular disease or disorder, a trait, or a set of related conditions."));
        }
        if(darDocument.containsKey("methods")){
            researchList.add(new SummaryItem("Methods development and validation studies:", " The primary purpose of the research is to develop and/or validate new methods for analyzing or interpreting data. Data will be used for developing and/or validating new methods."));
        }
        if(darDocument.containsKey("controls")){
            researchList.add(new SummaryItem("Controls:", " The reason for this request is to increase the number of controls available for a comparison group."));
        }
        if(darDocument.containsKey("population")){
            researchList.add(new SummaryItem("Population structure or normal variation studies:", " The primary purpose of the research is to understand variation in the general population."));
        }
        if(darDocument.containsKey("other")){
            researchList.add(new SummaryItem("Other: ", darDocument.getString("othertext")));
            manualReviewIsTrue();
        }
        return researchList;
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
}
