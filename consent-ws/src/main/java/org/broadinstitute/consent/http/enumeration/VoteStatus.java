package org.broadinstitute.consent.http.enumeration;

public enum VoteStatus {

    PENDING("pending"), EDITABLE("editable"), URGENT("urgent"), CLOSED("closed");

    private String value;

    VoteStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (VoteStatus e : VoteStatus.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

    public static String getValues() {
        StringBuilder values = new StringBuilder();
        for (VoteStatus e : VoteStatus.values()) {
            values.append(e.getValue());
            values.append(",");
        }
        String valuesResult = values.toString();
        //removing the las ","
        return valuesResult.substring(0, valuesResult.length() - 1);
    }

}
