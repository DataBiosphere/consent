package org.broadinstitute.consent.http.models.grammar;

import com.google.common.base.Objects;

public class Not extends UseRestriction {

    private String type = "not";

    private UseRestriction operand;

    public Not() {
    }

    public Not(UseRestriction operand) {
        this.operand = operand;
    }

    public String getType() {
        return type;
    }

    public UseRestriction getOperand() {
        return operand;
    }

    public void setOperand(UseRestriction op) {
        operand = op;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, operand);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Not &&
                Objects.equal(this.operand, ((Not) o).operand);
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return visitor.visit(operand);
    }
}