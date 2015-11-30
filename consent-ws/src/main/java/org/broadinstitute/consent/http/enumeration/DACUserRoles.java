package org.broadinstitute.consent.http.enumeration;

public enum DACUserRoles {

    MEMBER("MEMBER"), CHAIRPERSON("CHAIRPERSON"), ALUMNI("ALUMNI"), ADMIN("ADMIN"), RESEARCHER("RESEARCHER"), DATAOWNER("DATAOWNER");

    private String value;

    DACUserRoles(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (DACUserRoles e : DACUserRoles.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

}
