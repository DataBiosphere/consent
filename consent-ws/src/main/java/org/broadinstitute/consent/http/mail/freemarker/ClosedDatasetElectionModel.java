package org.broadinstitute.consent.http.mail.freemarker;

public class ClosedDatasetElectionModel {

    private String darId;
    private String numberOfDatasets;
    private String dsElectionResult;

    public ClosedDatasetElectionModel(String darId, String numberOfDatasets, String dsElectionResult) {
        this.darId = darId;
        this.numberOfDatasets = numberOfDatasets;
        this.dsElectionResult = dsElectionResult;
    }

    public String getDarId() {
        return darId;
    }

    public String getNumberOfDatasets() {
        return numberOfDatasets;
    }

    public String getDsElectionResult() {
        return dsElectionResult;
    }

}
