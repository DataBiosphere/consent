package org.broadinstitute.consent.http.mail.freemarker;

import java.util.List;

public class DelegateResponsibilitiesModel {

    private String userName;
    private String newRole;
    private String serverUrl;
    private List<VoteAndElectionModel> delegatedVotes;

    public DelegateResponsibilitiesModel(String userName, String newRoleName, String serverUrl, List<VoteAndElectionModel> delegatedVotes) {
        this.userName = userName;
        this.newRole = newRoleName;
        this.serverUrl = serverUrl;
        this.delegatedVotes = delegatedVotes;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNewRole() {
        return newRole;
    }

    public void setNewRole(String newRole) {
        this.newRole = newRole;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public List<VoteAndElectionModel> getDelegatedVotes() {
        return delegatedVotes;
    }

    public void setDelegatedVotes(List<VoteAndElectionModel> delegatedVotes) {
        this.delegatedVotes = delegatedVotes;
    }
}