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

import com.google.common.base.Objects;

import java.util.Arrays;

public class And extends UseRestriction {

    private String type = "and";

    private UseRestriction[] operands;

    public And() {}

    public And(UseRestriction... operands) { this.operands = operands; }

    public String getType() { return type; }

    public void setOperands(UseRestriction[] ops) { this.operands = ops.clone(); }
    public UseRestriction[] getOperands() { return operands; }

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
        for(UseRestriction child : operands) {
            if(!child.visit(visitor)) {
                return false;
            }
        }
        return true;
    }
}