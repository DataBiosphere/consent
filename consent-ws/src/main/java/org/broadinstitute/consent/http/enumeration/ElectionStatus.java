package org.broadinstitute.consent.http.enumeration;

public enum ElectionStatus {

    OPEN("Open"), CLOSED("Closed"), CANCELED("Canceled"), FINAL("Final"), PENDING_APPROVAL("PendingApproval");

    private String value;

    ElectionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (ElectionStatus e : ElectionStatus.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

    public static String getValues() {
        StringBuilder values = new StringBuilder();
        for (ElectionStatus e : ElectionStatus.values()) {
            values.append(e.getValue());
            values.append(",");
        }
        String valuesResult = values.toString();
        //removing the las ","
        return valuesResult.substring(0, valuesResult.length() - 1);
    }

}