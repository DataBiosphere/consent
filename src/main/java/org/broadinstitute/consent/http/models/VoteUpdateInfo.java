package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VoteUpdateInfo {

    @JsonProperty
    private Boolean vote;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private List<Integer> voteIds;
}
