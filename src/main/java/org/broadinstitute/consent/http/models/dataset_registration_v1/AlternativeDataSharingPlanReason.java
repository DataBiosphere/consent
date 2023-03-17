package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum AlternativeDataSharingPlanReason {

    LEGAL_RESTRICTIONS("Legal Restrictions"),
    INFORMED_CONSENT_PROCESSES_ARE_INADEQUATE_TO_SUPPORT_DATA_FOR_SHARING_FOR_THE_FOLLOWING_REASONS("Informed consent processes are inadequate to support data for sharing for the following reasons:"),
    THE_CONSENT_FORMS_ARE_UNAVAILABLE_OR_NON_EXISTENT_FOR_SAMPLES_COLLECTED_AFTER_JANUARY_25_2015("The consent forms are unavailable or non-existent for samples collected after January 25, 2015"),
    THE_CONSENT_PROCESS_DID_NOT_SPECIFICALLY_ADDRESS_FUTURE_USE_OR_BROAD_DATA_SHARING_FOR_SAMPLES_COLLECTED_AFTER_JANUARY_25_2015("The consent process did not specifically address future use or broad data sharing for samples collected after January 25, 2015"),
    THE_CONSENT_PROCESS_INADEQUATELY_ADDRESSES_RISKS_RELATED_TO_FUTURE_USE_OR_BROAD_DATA_SHARING_FOR_SAMPLES_COLLECTED_AFTER_JANUARY_25_2015("The consent process inadequately addresses risks related to future use or broad data sharing for samples collected after January 25, 2015"),
    THE_CONSENT_PROCESS_SPECIFICALLY_PRECLUDES_FUTURE_USE_OR_BROAD_DATA_SHARING_INCLUDING_A_STATEMENT_THAT_USE_OF_DATA_WILL_BE_LIMITED_TO_THE_ORIGINAL_RESEARCHERS("The consent process specifically precludes future use or broad data sharing (including a statement that use of data will be limited to the original researchers)"),
    OTHER_INFORMED_CONSENT_LIMITATIONS_OR_CONCERNS("Other informed consent limitations or concerns"),
    OTHER("Other");
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
