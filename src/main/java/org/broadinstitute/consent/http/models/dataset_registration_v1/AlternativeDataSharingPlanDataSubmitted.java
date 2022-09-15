package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@Generated("jsonschema2pojo")
public enum AlternativeDataSharingPlanDataSubmitted {

    WITHIN_3_MONTHS_OF_THE_LAST_DATA_GENERATED_OR_LAST_CLINICAL_VISIT("Within 3 months of the last data generated or last clinical visit"),
    BY_BATCHES_OVER_STUDY_TIMELINE_E_G_BASED_ON_CLINICAL_TRIAL_ENROLLMENT_BENCHMARKS("By batches over Study Timeline (e.g. based on clinical trial enrollment benchmarks)");
    private final String value;
    private final static Map<String, AlternativeDataSharingPlanDataSubmitted> CONSTANTS = new HashMap<String, AlternativeDataSharingPlanDataSubmitted>();

    static {
        for (AlternativeDataSharingPlanDataSubmitted c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    AlternativeDataSharingPlanDataSubmitted(String value) {
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
    public static AlternativeDataSharingPlanDataSubmitted fromValue(String value) {
        AlternativeDataSharingPlanDataSubmitted constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
