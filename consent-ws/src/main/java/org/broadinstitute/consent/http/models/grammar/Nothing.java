package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

public class Nothing extends UseRestriction {

    private String type = "nothing";

    public Nothing() {
    }

    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Nothing;
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return true;
    }
}