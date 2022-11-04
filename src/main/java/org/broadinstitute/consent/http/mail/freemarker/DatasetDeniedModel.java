package org.broadinstitute.consent.http.mail.freemarker;

public class DatasetDeniedModel {

    private String dataSubmitterName;
    private String datasetName;
    private String dacName;

    public DatasetDeniedModel(String dataSubmitterName, String datasetName, String dacName) {
        this.dataSubmitterName = dataSubmitterName;
        this.datasetName = datasetName;
        this.dacName = dacName;
    }

    public String getDataSubmitterName() {
        return dataSubmitterName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getDacName() {
        return dacName;
    }
}
