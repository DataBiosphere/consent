package org.broadinstitute.consent.http.mail.freemarker;

import java.util.Date;

public class NewHelpReportTemplate {

       /* This model works for templates: new-case. */

    private String userName;

    private String subject;

    private String description;

    private Date createDate;

    private String serverUrl;

    public NewHelpReportTemplate(String userName, String subject, String description, Date createDate, String serverUrl) {
        this.description = description;
        this.subject = subject;
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.createDate = createDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
