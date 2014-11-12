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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * ConsentAssociation Representation object.
 *
 * <P>Represents a set of associations to a single "type" of objects.  For example, the "samples" associated
 * with an individual consent.
 *
 * Created by egolin on 9/15/14.
 */
public class ConsentAssociation {
    @JsonProperty
    private String associationType;

    private HashSet<String> elementSet;   // We store the elements in a set to remove duplicates
                                        // NOTE:  HashSet not synchronized, so don't share access to these objects

    private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

    public ConsentAssociation() {
        // Jackson deserialization
    }

    public ConsentAssociation(String atype, List<String> ids) {
        this.associationType = atype;
        this.elementSet = new HashSet<String>(ids);
    }


    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType(String atype) {
        this.associationType = atype;
    }

    @JsonProperty
    public ArrayList<String> getElements() {
        return new ArrayList<String>(elementSet);
    }

    @JsonProperty
    public void setElements(List<String> ids) {
        elementSet = new HashSet<String>(ids);
    }

    public boolean isAssociationType(String atype) {
        return (associationType!=null) ? associationType.equals(atype) : false;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ConsentAssociation)) { return false; }
        // o cannot be null
        ConsentAssociation ca = (ConsentAssociation)o;
        return (((associationType!=null) ? associationType.equals(ca.associationType) : ca.associationType==null)
                 && (elementSet!= null) ? elementSet.equals(ca.elementSet) : ca.elementSet==null);
    }

    @Override
    public int hashCode() {
        int hash = 31;
        if (associationType != null)
            hash = hash + associationType.hashCode();
        if (elementSet != null)
            hash = hash*31 + elementSet.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "[JsonProcessingException caught: '" + e.getMessage() + "']";
        }
    }
}
