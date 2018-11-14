package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

public class Only extends UseRestriction {

    private String type = "only";
    private String property;
    private UseRestriction target;

    public Only() {
    }

    public Only(String prop, UseRestriction obj) {
        this.property = prop;
        this.target = obj;
    }

    public String getType() {
        return type;
    }

    public String getProperty() {
        return property;
    }

    public UseRestriction getTarget() {
        return target;
    }

    public void setProperty(String p) {
        property = p;
    }

    public void setTarget(UseRestriction r) {
        target = r;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, property, target);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Only &&
                Objects.equal(this.property, ((Only) o).property) &&
                Objects.equal(this.target, ((Only) o).target);
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return target.visit(visitor);
    }
}