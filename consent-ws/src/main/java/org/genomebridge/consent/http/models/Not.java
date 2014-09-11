/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.genomebridge.consent.http.models;

public class Not extends UseRestriction {

    private UseRestriction operand;

    public Not() {}

    public Not(UseRestriction operand) {
        this.operand = operand;
    }

    public UseRestriction getOperand() { return operand; }
    public void setOperand(UseRestriction op) { operand = op; }

    public String toString() {
        return String.format("{ \"type\": \"not\", \"operand\": %s }", operand.toString());
    }

    public int hashCode() { return 37 * (operand.hashCode() + 1); }

    public boolean equals(Object o) {
        return (o instanceof Not) && ((Not)o).operand.equals(operand);
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return visitor.visit(operand);
    }
}