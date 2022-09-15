package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@Generated("jsonschema2pojo")
public enum NihAnvilUse {

    I_DID("I did"),
    I_WILL("I will"),
    NO("No");
    private final String value;
    private final static Map<String, NihAnvilUse> CONSTANTS = new HashMap<String, NihAnvilUse>();

    static {
        for (NihAnvilUse c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    NihAnvilUse(String value) {
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
    public static NihAnvilUse fromValue(String value) {
        NihAnvilUse constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
