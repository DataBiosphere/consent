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

public class Only extends UseRestriction {

    private String type = "only";
    private String property;
    private UseRestriction target;

    public Only() {}

    public Only(String prop, UseRestriction obj) {
        this.property = prop;
        this.target = obj;
    }

    public String getType() { return type; }

    public String getProperty() { return property; }
    public UseRestriction getTarget() { return target; }

    public void setProperty(String p) { property = p; }
    public void setTarget(UseRestriction r) { target = r; }

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