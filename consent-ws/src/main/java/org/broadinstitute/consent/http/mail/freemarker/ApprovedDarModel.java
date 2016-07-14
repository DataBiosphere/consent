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

    public String getReferenceId() {
        return referenceId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterInstitute() {
        return requesterInstitute;
    }

    public String getResearchPurpose() {
        return researchPurpose;
    }

    public List<SummaryItem> getTypeOfResearch() {
        return typeOfResearch;
    }

    public String getDiseaseArea() {
        return diseaseArea;
    }

    public List<String> getCheckedSentences() {
        return checkedSentences;
    }

    public List<DataSetPIMailModel> getDsl() {
        return dsl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getDays() {
        return days;
    }

    public String getTranslatedUseRestriction() {
        return translatedUseRestriction;
    }

    public String getDate() {
        return date;
    }

}
