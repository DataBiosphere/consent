package org.broadinstitute.consent.http.service.ontology;

import java.util.List;

public class ParentTerm {

  private String id;
  private String label;
  private Integer order;
  private String definition;
  private List<String> synonyms;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public List<String> getSynonyms() {
    return synonyms;
  }

  public void setSynonyms(List<String> synonyms) {
    this.synonyms = synonyms;
  }

  public void addSynonym(String synonym) {
    this.synonyms.add(synonym);
  }
}
