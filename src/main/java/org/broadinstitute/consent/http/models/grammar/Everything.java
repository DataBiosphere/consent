package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

public class Everything extends UseRestriction {

    private String type = "everything";

    public Everything() {
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
        return o instanceof Everything;
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return true;
    }
}