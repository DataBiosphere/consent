package org.broadinstitute.consent.http.models.ontology;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class Term {

    String id;
    String ontology;
    List<String> synonyms;
    String label;
    String definition;
    Boolean usable;
    List<Parent> parents;

    public Term(String id, String ontology) {
        this.id = id;
        this.ontology = ontology;
        this.synonyms = new ArrayList<>();
        this.usable = true;
        this.parents = new ArrayList<>();
    }

    public void addSynonym(String synonym) {
        synonyms.add(synonym);
    }

    public void addLabel(String label) {
        this.label = label;
    }

    public void addDefinition(String definition) {
        this.definition = definition;
    }

    public void setUsable(boolean useable) {
        this.usable = useable;
    }

    public String getId() { return id; }

    public void addParent(String parent, Integer position) {
        parents.add(new Parent(parent, position));
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    class Parent {
        String id;
        Integer order;
        Parent(String id, Integer order) {
            this.id = id;
            this.order = order;
        }
    }

}
