package org.broadinstitute.consent.http.mail.freemarker;

public class NewResearcherModel {

    private final String researcherName;

    public NewResearcherModel(String researcherName) {
        this.researcherName = researcherName;
    }

    public String getResearcherName() {
        return researcherName;
    }

}
