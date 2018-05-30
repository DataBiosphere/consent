package org.broadinstitute.consent.http.enumeration;

public enum ElectionFields {

    ID("electionId"),
    TYPE("electionType"),
    STATUS("status"),
    CREATE_DATE("createDate"),
    REFERENCE_ID("referenceId"),
    LAST_UPDATE("lastUpdate"),
    FINAL_ACCESS_VOTE("finalAccessVote"),
    FINAL_VOTE("finalVote"),
    FINAL_RATIONALE("finalRationale"),
    FINAL_VOTE_DATE("finalVoteDate"),
    DATA_USE_LETTER("dataUseLetter"),
    DATASET_ID("datasetId"),
    USE_RESTRICTION("useRestriction"),
    TRANSLATED_USE_RESTRICTION("translatedUseRestriction"),
    VERSION("version"),
    ARCHIVED("archived"),
    DUL_NAME("dulName");

    private String value;

    ElectionFields(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
