package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * ConsentAssociation Representation object.
 * <p/>
 * <P>Represents a set of associations to a single "type" of objects.  For example, the "samples" associated
 * with an individual consent.
 * <p/>
 * Created by egolin on 9/15/14.
 */
public class ConsentAssociation {

    @JsonProperty
    private String associationType;

    private HashSet<String> elementSet;   // We store the elements in a set to remove duplicates
    // NOTE:  HashSet not synchronized, so don't share access to these objects

    @SuppressWarnings("unused")
    public ConsentAssociation() {
        // Jackson deserialization
    }

    public ConsentAssociation(String atype, List<String> ids) {
        this.associationType = atype;
        this.elementSet = new HashSet<>(ids);
    }


    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType(String atype) {
        this.associationType = atype;
    }

    @JsonProperty
    public ArrayList<String> getElements() {
        return new ArrayList<>(elementSet);
    }

    @JsonProperty
    public void setElements(List<String> ids) {
        elementSet = new HashSet<>(ids);
    }

    public boolean isAssociationType(String atype) {
        return (associationType != null) && associationType.equals(atype);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConsentAssociation &&
                Objects.equal(this.associationType, ((ConsentAssociation) o).associationType) &&
                Objects.equal(this.elementSet, ((ConsentAssociation) o).elementSet);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(associationType, elementSet);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
