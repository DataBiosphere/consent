package org.broadinstitute.consent.http.enumeration;

public enum TranslateType {

    PURPOSE("purpose"), SAMPLESET("sampleset");

    private String value;

    TranslateType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (TranslateType e : TranslateType.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }


}
