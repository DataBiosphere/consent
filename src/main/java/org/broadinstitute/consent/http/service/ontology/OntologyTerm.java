package org.broadinstitute.consent.http.service.ontology;

import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

@SuppressWarnings("unused") // Getters are used in JDBI @BindMethods
public class OntologyTerm {

  String id;
  String version;
  String ontology;
  List<String> synonyms;
  String label;
  String definition;
  Boolean usable;
  String oboId;
  List<ParentTerm> parents;

  public OntologyTerm(String id, String version, String ontology) {
    this.id = id;
    this.version = version;
    this.ontology = ontology;
    this.synonyms = new ArrayList<>();
    this.usable = true;
    this.parents = new ArrayList<>();
  }

  public String id() {
    return id;
  }

  public String version() {
    return version;
  }

  public String ontology() {
    return ontology;
  }

  public String synonyms() {
    return String.join(" ", synonyms);
  }

  public String label() {
    return label;
  }

  public String definition() {
    return definition;
  }

  public boolean usable() {
    return usable;
  }

  public String oboId() {
    return oboId;
  }

  public String jsonDocument() {
    return GsonUtil.getInstance().toJson(this);
  }

  public void setSynonyms(List<String> synonyms) {
    this.synonyms = synonyms;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public void setUsable(boolean usable) {
    this.usable = usable;
  }

  public void setOboId(String oboId) {
    this.oboId = oboId;
  }

  public void addParent(ParentTerm term) {
    parents.add(term);
  }

  @Override
  public String toString() {
    return GsonUtil.getInstance().toJson(this);
  }
}
