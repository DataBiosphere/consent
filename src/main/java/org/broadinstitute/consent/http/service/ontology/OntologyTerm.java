package org.broadinstitute.consent.http.service.ontology;

import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

@SuppressWarnings("unused") // Unused getters are used in JDBI method mappint.
public class OntologyTerm {

  String termId;
  String version;
  String ontology;
  List<String> synonyms;
  String label;
  String definition;
  Boolean usable;
  String oboId;
  List<ParentTerm> parents;

  public OntologyTerm(String termId, String version, String ontology) {
    this.termId = termId;
    this.version = version;
    this.ontology = ontology;
    this.synonyms = new ArrayList<>();
    this.usable = true;
    this.parents = new ArrayList<>();
  }

  public String termId() {
    return termId;
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

  public void addSynonym(String synonym) {
    synonyms.add(synonym);
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

  public void addParent(String parent, String label, Integer position) {
    parents.add(new ParentTerm(parent, label, position));
  }

  @Override
  public String toString() {
    return GsonUtil.getInstance().toJson(this);
  }
}
