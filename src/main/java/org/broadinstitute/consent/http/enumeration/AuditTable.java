package org.broadinstitute.consent.http.enumeration;

public enum AuditTable {

    CONSENT_ASSOCIATIONS("consentassociations"), CONSENT("consents");

    private String value;

    AuditTable(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
