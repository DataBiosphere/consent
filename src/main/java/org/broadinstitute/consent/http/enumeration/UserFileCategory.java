package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum UserFileCategory {
    IRB_COLLABORATION_LETTER("irbCollaborationLetter"),
    DATA_USE_LETTER("dataUseLetter"),
    ALTERNATIVE_DATA_SHARING_PLAN("alternativeDataSharingPlan");

    private final String value;

    UserFileCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static List<String> getValues() {
        return Stream.of(UserFileCategory.values()).map(UserFileCategory::getValue).collect(Collectors.toList());
    }

    public static Boolean containsValue(String value) {
        for (UserFileCategory researcherField : UserFileCategory.values()) {
            if (researcherField.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static UserFileCategory findValue(String value) {
        for (UserFileCategory cat : UserFileCategory.values()) {
            if (cat.getValue().equals(value)) {
                return cat;
            }
        }
        return null;
    }
}
