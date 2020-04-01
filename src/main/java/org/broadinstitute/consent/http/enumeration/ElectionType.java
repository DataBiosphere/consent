package org.broadinstitute.consent.http.enumeration;

import java.util.EnumSet;
import java.util.Optional;

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

    public static ElectionType getFromValue(String value) {
        Optional<ElectionType> type = EnumSet.allOf(ElectionType.class).
                stream().
                filter(t -> t.getValue().equalsIgnoreCase(value)).
                findFirst();
        return type.orElse(null);
    }

}
