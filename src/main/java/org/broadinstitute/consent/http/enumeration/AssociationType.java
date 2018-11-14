package org.broadinstitute.consent.http.enumeration;

public enum AssociationType {

    SAMPLE("sample"), SAMPLESET("sampleSet"), WORKSPACE("workspace");

    private String value;

    AssociationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
}
