package org.broadinstitute.consent.http.enumeration;

public enum SupportRequestType {

    QUESTION("question"), INCIDENT("incident"), PROBLEM("problem"), TASK("task");

    private final String value;

    SupportRequestType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
