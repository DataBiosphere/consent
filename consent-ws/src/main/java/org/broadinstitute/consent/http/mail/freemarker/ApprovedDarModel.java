package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.models.DataSet;

import java.util.List;

public class ApprovedDarModel {

    private String userName;

    private String referenceId;

    private List<DataSet> dsl;

    private String serverUrl;

    private int days;

    public ApprovedDarModel(String user, String referenceId, List<DataSet> dataSetList, String serverUrl, Integer days) {
        this.userName = user;
        this.referenceId = referenceId;
        this.serverUrl = serverUrl;
        this.dsl = dataSetList;
        this.days = days;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String user) {
        this.userName = user;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public List<DataSet> getDsl() {
        return dsl;
    }

    public void setDsl(List<DataSet> dsl) {
        this.dsl = dsl;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
