package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Error {

    @JsonProperty
    private String message;

    @JsonProperty
    private Integer code;

    public Error() {
    }

    public Error(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String error) {
        this.message = error;
    }


}
