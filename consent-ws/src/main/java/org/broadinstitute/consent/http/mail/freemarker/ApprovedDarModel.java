package org.broadinstitute.consent.http.mail.freemarker;

import java.util.List;
import org.broadinstitute.consent.http.models.darsummary.SummaryItem;

public class ApprovedDarModel {

    private String userName;
    private String date;
    private String referenceId;
    private String requesterName;
    private String requesterInstitute;
    private String translatedUseRestriction;
    private String researchPurpose;
    private List<SummaryItem> typeOfResearch;
    private String diseaseArea;
    private List<String> checkedSentences;
    private List<DataSetPIMailModel> dsl;
    private String serverUrl;
    private String days;

    public ApprovedDarModel(String userName, String date, String referenceId, String requesterName, String requesterInstitute, String researchPurpose,
                            List<SummaryItem> typeOfResearch, String diseaseArea, List<String> checkedSentences, String translatedUseRestriction,
                            List<DataSetPIMailModel> dsl, String serverUrl, String days) {
        this.userName = userName;
        this.referenceId = referenceId;
        this.date = date;
        this.requesterName = requesterName;
        this.requesterInstitute = requesterInstitute;
        this.researchPurpose = researchPurpose;
        this.typeOfResearch = typeOfResearch;
        this.translatedUseRestriction = translatedUseRestriction;
        this.diseaseArea = diseaseArea;
        this.checkedSentences = checkedSentences;
        this.dsl = dsl;
        this.serverUrl = serverUrl;
        this.days = days;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterInstitute() {
        return requesterInstitute;
    }

    public void setRequesterInstitute(String requesterInstitute) {
        this.requesterInstitute = requesterInstitute;
    }

    public String getResearchPurpose() {
        return researchPurpose;
    }

    public void setResearchPurpose(String researchPurpose) {
        this.researchPurpose = researchPurpose;
    }

    public List<SummaryItem> getTypeOfResearch() {
        return typeOfResearch;
    }

    public void setTypeOfResearch(List<SummaryItem> typeOfResearch) {
        this.typeOfResearch = typeOfResearch;
    }

    public String getDiseaseArea() {
        return diseaseArea;
    }

    public void setDiseaseArea(String diseaseArea) {
        this.diseaseArea = diseaseArea;
    }

    public List<String> getCheckedSentences() {
        return checkedSentences;
    }

    public void setCheckedSentences(List<String> checkedSentences) {
        this.checkedSentences = checkedSentences;
    }

    public List<DataSetPIMailModel> getDsl() {
        return dsl;
    }

    public void setDsl(List<DataSetPIMailModel> dsl) {
        this.dsl = dsl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public String getTranslatedUseRestriction() {
        return translatedUseRestriction;
    }

    public void setTranslatedUseRestriction(String translatedUseRestriction) {
        this.translatedUseRestriction = translatedUseRestriction;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
