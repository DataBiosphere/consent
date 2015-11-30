package org.broadinstitute.consent.http.models.matching;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseMatchingObject {

    @JsonProperty
    public boolean result;

    @JsonProperty
    public RequestMatchingObject matchPair;

    public ResponseMatchingObject() {
    }

    public ResponseMatchingObject(boolean result, RequestMatchingObject matchPair) {
        this.result = result;
        this.matchPair = matchPair;
    }

    @JsonProperty
    public boolean isResult() {
        return result;
    }

    @JsonProperty
    public void setMatch(boolean result) {
        this.result = result;
    }

    @JsonProperty
    public RequestMatchingObject getMatchPair() {
        return matchPair;
    }

    @JsonProperty
    public void setMatchPair(RequestMatchingObject matchPair) {
        this.matchPair = matchPair;
    }
}
