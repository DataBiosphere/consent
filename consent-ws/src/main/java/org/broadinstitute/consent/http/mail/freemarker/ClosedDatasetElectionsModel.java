package org.broadinstitute.consent.http.mail.freemarker;

import java.util.List;

public class ClosedDatasetElectionsModel {

    private String serverUrl;
    private List<ClosedDatasetElectionModel> closedElections;

    public ClosedDatasetElectionsModel(String serverUrl, List<ClosedDatasetElectionModel> closedElections) {
        this.serverUrl = serverUrl;
        this.closedElections = closedElections;
    }

    public List<ClosedDatasetElectionModel> getClosedElections() {
        return closedElections;
    }

    public void setClosedElections(List<ClosedDatasetElectionModel> closedElections) {
        this.closedElections = closedElections;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
