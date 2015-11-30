package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

import java.util.Arrays;

public class And extends UseRestriction {

    private String type = "and";

    private UseRestriction[] operands;

    public And() {
    }

    public And(UseRestriction... operands) {
        this.operands = operands;
    }

    public String getType() {
        return type;
    }

    public void setOperands(UseRestriction[] ops) {
        this.operands = ops.clone();
    }

    public UseRestriction[] getOperands() {
        return operands;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, operands);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof And &&
                Arrays.deepEquals(this.operands, ((And) o).operands);
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        for (UseRestriction child : operands) {
            if (!child.visit(visitor)) {
                return false;
            }
        }
        return true;
    }
}