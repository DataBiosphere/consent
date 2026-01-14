package org.broadinstitute.consent.http.service.ontology;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class OntologyTerm {

  String termId;
  String version;
  String ontology;
  List<String> synonyms;
  String label;
  String definition;
  Boolean usable;
  List<ParentTerm> parents;

  public OntologyTerm(String termId, String version, String ontology) {
    this.termId = termId;
    this.version = version;
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

  public String getTermId() {
    return termId;
  }

  public void addParent(String parent, String label, Integer position) {
    parents.add(new ParentTerm(parent, label, position));
  }

  public List<ParentTerm> getParents() {
    return parents;
  }

  @Override
  public String toString() {
    return GsonUtil.getInstance().toJson(this);
  }

  public JsonElement getJson() {
    return GsonUtil.getInstance().toJsonTree(this);
  }
}
