package org.broadinstitute.consent.http.enumeration;

public enum AuthenticationType {

    BASIC("Basic"), BEARER("Bearer");

    private String value;

    AuthenticationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }



}