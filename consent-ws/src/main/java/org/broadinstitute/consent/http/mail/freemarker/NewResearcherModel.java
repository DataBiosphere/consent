package org.broadinstitute.consent.http.mail.freemarker;

public class NewResearcherModel {

    String adminName;
    String researcherName;
    String redirectToURL;

    public NewResearcherModel(String adminName, String researcherName, String redirectToURL) {
        this.adminName = adminName;
        this.researcherName = researcherName;
        this.redirectToURL = redirectToURL;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getResearcherName() {
        return researcherName;
    }

    public void setResearcherName(String researcherName) {
        this.researcherName = researcherName;
    }

    public String getRedirectToURL() {
        return redirectToURL;
    }

    public void setRedirectToURL(String redirectToURL) {
        this.redirectToURL = redirectToURL;
    }
}
