package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@Generated("jsonschema2pojo")
public enum AlternativeDataSharingPlanReason {

    Legal_Restrictions("Legal Restrictions"),
    Informed_consent_processes_are_inadequate_to_support_data_for_sharing_for_the_following_reasons("Informed consent processes are inadequate to support data for sharing for the following reasons:"),
    The_consent_forms_are_unavailable_or_non_existent_for_samples_collected_after_january_25_2015("The consent forms are unavailable or non-existent for samples collected after January 25, 2015"),
    The_consent_process_did_not_specifically_address_future_use_or_broad_data_sharing_for_samples_collected_after_january_25_2015("The consent process did not specifically address future use or broad data sharing for samples collected after January 25, 2015"),
    The_consent_process_inadequately_addresses_risks_related_to_future_use_or_broad_data_sharing_for_samples_collected_after_january_25_2015("The consent process inadequately addresses risks related to future use or broad data sharing for samples collected after January 25, 2015"),
    The_consent_process_specifically_precludes_future_use_or_broad_data_sharing_including_a_statement_that_use_of_data_will_be_limited_to_the_original_researchers("The consent process specifically precludes future use or broad data sharing (including a statement that use of data will be limited to the original researchers)"),
    Other_informed_consent_limitations_or_concerns("Other informed consent limitations or concerns"),
    Other("Other");
    private final String value;
    private final static Map<String, AlternativeDataSharingPlanReason> CONSTANTS = new HashMap<String, AlternativeDataSharingPlanReason>();

    static {
        for (AlternativeDataSharingPlanReason c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    AlternativeDataSharingPlanReason(String value) {
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
    public static AlternativeDataSharingPlanReason fromValue(String value) {
        AlternativeDataSharingPlanReason constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
