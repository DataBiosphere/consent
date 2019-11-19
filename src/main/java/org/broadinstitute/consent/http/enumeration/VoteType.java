package org.broadinstitute.consent.http.enumeration;

public enum VoteType {

    DAC("DAC"), FINAL("FINAL"), AGREEMENT("AGREEMENT"), CHAIRPERSON("CHAIRPERSON"), DATA_OWNER("DATA_OWNER");

    private String value;

    VoteType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (VoteType e : VoteType.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }


}
