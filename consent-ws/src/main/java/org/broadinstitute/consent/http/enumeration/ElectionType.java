package org.broadinstitute.consent.http.enumeration;

public enum ElectionType {

    DATA_ACCESS("DataAccess"),
    TRANSLATE_DUL("TranslateDUL"),
    RP("RP"),
    DATA_SET("DataSet");

    private String value;

    ElectionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (ElectionType e : ElectionType.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

}
