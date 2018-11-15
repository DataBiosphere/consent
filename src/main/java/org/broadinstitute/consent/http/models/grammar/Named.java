package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

public class Named extends UseRestriction {

    private String type = "named";

    private String name;

    public Named() {
    }

    public Named(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, name);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Named &&
                Objects.equal(this.name, ((Named) o).name);
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return true;
    }
}