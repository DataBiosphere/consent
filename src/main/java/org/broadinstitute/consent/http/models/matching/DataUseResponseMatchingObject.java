package org.broadinstitute.consent.http.models.matching;

import java.util.List;

public class DataUseResponseMatchingObject {

    public boolean result;

    public DataUseRequestMatchingObject matchPair;

    public List<String> failureReasons;

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
