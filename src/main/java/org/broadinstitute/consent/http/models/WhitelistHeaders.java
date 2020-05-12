package org.broadinstitute.consent.http.models;

public enum WhitelistHeaders {

    ORGANIZATION("Organization"),
    COMMONS_ID("Commons Id"),
    NAME("Name"),
    EMAIL("Email"),
    SIGNING_OFFICIAL_NAME("Signing Official Name"),
    SIGNING_OFFICIAL_EMAIL("Signing Official Email"),
    IT_DIRECTOR_NAME("IT Director Name"),
    IT_DIRECTOR_EMAIL("IT Director Email");

    String value;

    WhitelistHeaders(String value) {
        this.setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
