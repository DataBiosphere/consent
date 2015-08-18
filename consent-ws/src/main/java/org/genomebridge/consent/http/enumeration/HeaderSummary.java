package org.genomebridge.consent.http.enumeration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
