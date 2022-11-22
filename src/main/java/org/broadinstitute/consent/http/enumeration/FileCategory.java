package org.broadinstitute.consent.http.enumeration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FileCategory {
    IRB_COLLABORATION_LETTER("irbCollaborationLetter"),
    DATA_USE_LETTER("dataUseLetter"),
    ALTERNATIVE_DATA_SHARING_PLAN("alternativeDataSharingPlan"),
    NIH_INSTITUTIONAL_CERTIFICATION("nihInstitutionalCertification");

    private final String value;

    FileCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static List<String> getValues() {
        return Stream.of(FileCategory.values()).map(FileCategory::getValue).collect(Collectors.toList());
    }

    public static Boolean containsValue(String value) {
        for (FileCategory researcherField : FileCategory.values()) {
            if (researcherField.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static FileCategory findValue(String value) {
        for (FileCategory cat : FileCategory.values()) {
            if (cat.getValue().equals(value)) {
                return cat;
            }
        }
        return null;
    }
}
