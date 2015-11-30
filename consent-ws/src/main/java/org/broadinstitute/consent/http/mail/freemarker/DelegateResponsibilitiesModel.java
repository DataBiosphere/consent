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

    public String getNewRole() {
        return newRole;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public List<VoteAndElectionModel> getDelegatedVotes() {
        return delegatedVotes;
    }

}