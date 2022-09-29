package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@Generated("jsonschema2pojo")
public enum DataLocation {

    AN_VIL_WORKSPACE("AnVIL Workspace"),
    TERRA_WORKSPACE("Terra Workspace"),
    TDR_LOCATION("TDR Location"),
    NOT_DETERMINED("Not Determined");
    private final String value;
    private final static Map<String, DataLocation> CONSTANTS = new HashMap<String, DataLocation>();

    static {
        for (DataLocation c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    DataLocation(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static DataLocation fromValue(String value) {
        DataLocation constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}