package org.broadinstitute.consent.http.service.ontology;

import com.google.gson.annotations.Expose;
import java.util.List;

public class ParentTerm {

  @Expose private String id;
  @Expose private String label;
  @Expose private Integer order;
  @Expose private String definition;
  @Expose private List<String> synonyms;

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
}
