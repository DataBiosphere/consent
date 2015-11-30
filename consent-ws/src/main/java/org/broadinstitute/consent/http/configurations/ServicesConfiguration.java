package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class ServicesConfiguration {

    @NotNull
    public String matchURL;

    @NotNull
    public String localURL;

    @NotNull
    public String translateURL;

    public String getMatchURL() {
        return matchURL;
    }

    public void setMatchURL(String matchURL) {
        this.matchURL = matchURL;
    }

    public String getLocalURL() {
        return localURL;
    }

    public void setLocalURL(String localURL) {
        this.localURL = localURL;
    }

    public String getTranslateURL() {
        return translateURL;
    }

    public void setTranslateURL(String translateURL) {
        this.translateURL = translateURL;
    }

}
