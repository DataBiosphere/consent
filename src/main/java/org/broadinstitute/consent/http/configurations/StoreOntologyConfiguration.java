package org.broadinstitute.consent.http.configurations;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreOntologyConfiguration {

    @NotNull
    public String bucketSubdirectory;

    @NotNull
    public String configurationFileName;

    public String getBucketSubdirectory() {
        return bucketSubdirectory;
    }

    public void setBucketSubdirectory(String bucketSubdirectory) {
        this.bucketSubdirectory = bucketSubdirectory;
    }

    public String getConfigurationFileName() {
        return configurationFileName;
    }

    public void setConfigurationFileName(String configurationFileName) {
        this.configurationFileName = configurationFileName;
    }
}
