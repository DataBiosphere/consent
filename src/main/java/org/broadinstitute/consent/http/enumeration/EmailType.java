package org.broadinstitute.consent.http.enumeration;

public enum EmailType {

    COLLECT(1),
    REMINDER(3),
    NEW_DAR(4),
    DISABLED_DATASET(5),
    CLOSED_DATASET_ELECTION(6),
    NEW_CASE(7),
    DATA_CUSTODIAN_APPROVAL(8),
    RESEARCHER_DAR_APPROVED(9),
    ADMIN_FLAGGED_DAR_APPROVED(10),
    DAR_CANCEL(11),
    DELEGATE_RESPONSIBILITIES(12),
    NEW_RESEARCHER(13),
    RESEARCHER_APPROVED(14)
    ;

    private final Integer typeInt;

    EmailType(Integer typeInt) {
        this.typeInt = typeInt;
    }

    public Integer getTypeInt() {
        return typeInt;
    }
}
