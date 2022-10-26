package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.processing.Generated;
import java.util.HashMap;
import java.util.Map;

@Generated("jsonschema2pojo")
public enum NihICsSupportingStudy {

    NCI("NCI"),
    NEI("NEI"),
    NHLBI("NHLBI"),
    NHGRI("NHGRI"),
    NIA("NIA"),
    NIAAA("NIAAA"),
    NIAID("NIAID"),
    NIAMS("NIAMS"),
    NIBIB("NIBIB"),
    NICHD("NICHD"),
    NIDCD("NIDCD"),
    NIDCR("NIDCR"),
    NIDDK("NIDDK"),
    NIDA("NIDA"),
    NIEHS("NIEHS"),
    NIGMS("NIGMS"),
    NIMH("NIMH"),
    NIMHD("NIMHD"),
    NINDS("NINDS"),
    NINR("NINR"),
    NLM("NLM"),
    CC("CC"),
    CIT("CIT"),
    CSR("CSR"),
    FIC("FIC"),
    NCATS("NCATS"),
    NCCIH("NCCIH");
    private final String value;
    private final static Map<String, NihICsSupportingStudy> CONSTANTS = new HashMap<String, NihICsSupportingStudy>();

    static {
        for (NihICsSupportingStudy c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    NihICsSupportingStudy(String value) {
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
    public static NihICsSupportingStudy fromValue(String value) {
        NihICsSupportingStudy constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
