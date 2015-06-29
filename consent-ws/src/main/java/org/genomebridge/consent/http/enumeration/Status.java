package org.genomebridge.consent.http.enumeration;

public enum Status {

    OPEN("Open"), CLOSED("Closed"), CANCELED("Canceled");

    private String value;

    Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (Status e : Status.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

    public static String getValues() {
        StringBuilder values = new StringBuilder();
        for (Status e : Status.values()) {
            values.append(e.getValue());
            values.append(",");
        }
        String valuesResult = values.toString();
        //removing the las ","
        return valuesResult.substring(0, valuesResult.length() - 1);
    }

}
