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

    public static String getValue(String value) {
        for (AssociationType e : AssociationType.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

    public static String getValues() {
        StringBuilder values = new StringBuilder();
        for (AssociationType e : AssociationType.values()) {
            values.append(e.getValue());
            values.append(",");
        }
        String valuesResult = values.toString();
        //removing the las ","
        return valuesResult.substring(0, valuesResult.length() - 1);
    }
}
