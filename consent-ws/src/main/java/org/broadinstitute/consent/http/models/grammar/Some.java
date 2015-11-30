package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

public class Some extends UseRestriction {

    private String type = "some";

    private String property;

    private UseRestriction target;

    public Some() {
    }

    public Some(String prop, UseRestriction obj) {
        this.property = prop;
        this.target = obj;
    }

    public String getType() {
        return type;
    }

    public void setProperty(String p) {
        property = p;
    }

    public String getProperty() {
        return property;
    }

    public UseRestriction getTarget() {
        return target;
    }

    public void setTarget(UseRestriction r) {
        target = r;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, target);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Some &&
                Objects.equal(this.property, ((Some) o).property) &&
                Objects.equal(this.target, ((Some) o).target);
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return target.visit(visitor);
    }

}