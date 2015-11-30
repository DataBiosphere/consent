package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

import java.util.Arrays;

public class Or extends UseRestriction {

    private String type = "or";

    private UseRestriction[] operands;

    public Or() {
    }

    public Or(UseRestriction... operands) {
        this.operands = operands;
        if (operands.length < 2) {
            throw new IllegalArgumentException("Disjunction must have at least two operands");
        }
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
        return o instanceof Or &&
                Arrays.deepEquals(this.operands, ((Or) o).operands);
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