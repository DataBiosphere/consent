package org.genomebridge.consent.http.enumeration;


public enum HeaderSummary {

    CASEID("Case ID"),
    USER("User"),
    VOTE("Vote"),
    RATIONALE("Rationale"),
    FINAL_VOTE("Final Vote"),
    FINAL_RATIONALE("Final Rationale"),
    SDUL("sDUL");

    private String value;

    HeaderSummary(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
