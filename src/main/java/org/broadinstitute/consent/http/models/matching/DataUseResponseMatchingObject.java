package org.broadinstitute.consent.http.models.matching;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DataUseResponseMatchingObject {

    @JsonProperty
    public boolean result;

    @JsonProperty
    public DataUseRequestMatchingObject matchPair;

    @JsonProperty
    public List<String> failureReasons;

    public DataUseResponseMatchingObject() {
    }

    public DataUseResponseMatchingObject(boolean result, DataUseRequestMatchingObject matchPair, List<String> failureReasons) {
        this.result = result;
        this.matchPair = matchPair;
        this.failureReasons = failureReasons;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public DataUseRequestMatchingObject getMatchPair() {
        return matchPair;
    }

    public void setMatchPair(DataUseRequestMatchingObject matchPair) {
        this.matchPair = matchPair;
    }

    public List<String> getFailureReasons() {
        return failureReasons;
    }

    public void setFailureReasons(List<String> failureReasons) {
        this.failureReasons = failureReasons;
    }
}
