package org.broadinstitute.consent.http.models.validate;

import java.util.ArrayList;
import java.util.Collection;

public class ValidateResponse {

    private boolean valid = false;
    private String useRestriction;
    private Collection<String> errors = new ArrayList<>();

    public ValidateResponse() {
    }

    public ValidateResponse(boolean valid, String useRestriction, Collection<String> errors) {
        this.valid = valid;
        this.useRestriction = useRestriction;
        this.errors = errors;
    }

    public ValidateResponse(boolean valid, String useRestriction) {
        this.valid = valid;
        this.useRestriction = useRestriction;
        this.errors = new ArrayList<>();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUseRestriction() {
        return useRestriction;
    }

    public void setUseRestriction(String useRestriction) {
        this.useRestriction = useRestriction;
    }

    public Collection<String> getErrors() {
        return errors;
    }

    public void setErrors(Collection<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addErrors(Collection<String> errors) {
        this.errors.addAll(errors);
    }
}
