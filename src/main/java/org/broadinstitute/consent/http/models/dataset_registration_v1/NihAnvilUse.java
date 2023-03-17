package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum NihAnvilUse {

    I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY("I am NHGRI funded and I have a dbGaP PHS ID already"),
    I_AM_NHGRI_FUNDED_AND_I_DO_NOT_HAVE_A_DB_GA_P_PHS_ID("I am NHGRI funded and I do not have a dbGaP PHS ID"),
    I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL("I am not NHGRI funded but I am seeking to submit data to AnVIL"),
    I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL("I am not NHGRI funded and do not plan to store data in AnVIL");
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
