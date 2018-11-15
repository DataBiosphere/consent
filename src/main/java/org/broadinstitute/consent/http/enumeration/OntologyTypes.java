package org.broadinstitute.consent.http.enumeration;

import java.util.List;

public enum OntologyTypes {

    DISEASE("disease"), ORGANIZATION("organization");
    private String value;

    OntologyTypes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (OntologyTypes e : OntologyTypes.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

    public static boolean contains(List<String> valueList) {
        for (String value : valueList) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(String value) {
        for (OntologyTypes c : OntologyTypes.values()) {
            if (c.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

}
