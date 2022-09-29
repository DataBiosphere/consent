package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@Generated("jsonschema2pojo")
public enum AlternativeDataSharingPlanDataSubmitted {

    Within_3_months_of_the_last_data_generated_or_last_clinical_visit("Within 3 months of the last data generated or last clinical visit"),
    By_batches_over_study_timeline_e_g_based_on_clinical_trial_enrollment_benchmarks("By batches over Study Timeline (e.g. based on clinical trial enrollment benchmarks)");
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