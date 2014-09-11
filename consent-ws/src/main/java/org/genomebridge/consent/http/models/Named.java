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

public class Named extends UseRestriction {
    private String name;

    public Named() {}
    public Named(String name) { this.name = name; }

    public String toString() {
        return String.format("{ \"type\": \"named\", \"name\": \"%s\" }", name);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int hashCode() { return name.hashCode(); }

    public boolean equals(Object o) {
        if(!(o instanceof Named)) { return false; }
        Named n = (Named)o;
        return name.equals(n.name);
    }

    public boolean visitAndContinue(UseRestrictionVisitor visitor) {
        return true;
    }
}