package org.genomebridge.consent.http.enumeration;

public enum HeaderSummary {



    CONSENT("Consent"),
    STRUCT_LIMITATIONS("Structured Limitations"),
    DATE("Date "),
    CHAIRPERSON("Chairperson"),
    FINAL_DECISION("Final Decision"),
    FINAL_DECISION_RATIONALE("Final Decision Rationale"),
    USER_VOTE_RATIONALE("User, Vote , Rationale"),

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
    FINAL_RATIONALE_DAR("Final Rationale  on DAR"),
    FINAL_DECISION_RP("Final Decision on RP"),
    FINAL_RATIONALE_RP("Final Rationale  on RP"),
    FINAL_DECISION_DUL("Final Decision on DUL"),
    FINAL_RATIONALE_DUL("Final Rationale  on DUL");

    private String value;

    HeaderSummary(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}