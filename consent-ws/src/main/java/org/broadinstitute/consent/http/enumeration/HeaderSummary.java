package org.broadinstitute.consent.http.enumeration;

public enum HeaderSummary {



    CONSENT("Consent"),
    STRUCT_LIMITATIONS("Structured Limitations"),
    DATE("Date "),
    CHAIRPERSON("Chairperson"),
    FINAL_DECISION("Final Decision"),
    FINAL_DECISION_RATIONALE("Final Decision Rationale / Comments"),
    USER("User"),
    VOTE("Vote"),
    RATIONALE("Rationale / Comments"),

    DATA_REQUEST_ID("Data Request ID"),
    VAULT_DECISION("Vault Decision"),
    VAULT_VS_DAC_AGREEMENT("Vault vs DAC Agreement"),
    CHAIRPERSON_FEEDBACK("Chairperson feedback on Vault Decision"),
    RESEARCHER("Principal Investigator"),
    PROJECT_TITLE("Project title"),
    DATASET_ID("Dataset ID"),
    DATA_ACCESS_SUBM_DATE("Data Access Request submission date"),
    DAC_MEMBERS("DAC Member"),
    REQUIRE_MANUAL_REVIEW("Require Manual Review"),
    FINAL_DECISION_DAR("Final Decision on DAR"),
    FINAL_RATIONALE_DAR("Final Rationale / Comments on DAR"),
    FINAL_DECISION_RP("Final Decision on RP"),
    FINAL_RATIONALE_RP("Final Rationale / Comments on RP"),
    FINAL_DECISION_DUL("Final Decision on DUL"),
    FINAL_RATIONALE_DUL("Final Rationale / Comments on DUL "),

    DATASET_NAME("Dataset Name"),
    DATASET_FINAL_STATUS("Dataset Final Status"),
    DATA_OWNER_NAME("Data Owner Name"),
    DATA_OWNER_EMAIL("Data Owner Email"),
    DATA_OWNER_VOTE("Data Owner Vote"),
    DATA_OWNER_COMMENT("Data Owner Comment");

    private String value;

    HeaderSummary(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
