package org.broadinstitute.consent.http.enumeration;

public enum DarCollectionStatus {
    UNREVIEWED("Unreviewed"),
    IN_PROCESS("In Process"),
    COMPLETE("Complete"),
    DRAFT("Draft"),
    CANCELED("Canceled");

    private final String value;

    DarCollectionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
