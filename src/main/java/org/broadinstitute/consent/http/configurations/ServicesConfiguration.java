package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class ServicesConfiguration {

    @NotNull
    private String ontologyURL;

    @NotNull
    private String localURL;

    @NotNull
    private String samUrl;

    private final String MATCH = "match";

    private final String VALIDATE_USE_RESTRICTION = "validate/userestriction";

    public String getOntologyURL() {
        return ontologyURL;
    }

    public void setOntologyURL(String ontologyURL) {
        this.ontologyURL = ontologyURL;
    }

    public String getLocalURL() {
        return localURL;
    }

    public void setLocalURL(String localURL) {
        this.localURL = localURL;
    }

    public String getMatchURL() {
        return getOntologyURL() + MATCH;
    }

    public String getValidateUseRestrictionURL() {
        return getOntologyURL() + VALIDATE_USE_RESTRICTION;
    }

    public String getDARTranslateUrl() {
        return getOntologyURL() + "schemas/data-use/dar/translate";
    }

    public String getSamUrl() {
        return samUrl;
    }

    public void setSamUrl(String samUrl) {
        this.samUrl = samUrl;
    }
}
