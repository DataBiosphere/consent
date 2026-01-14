package org.broadinstitute.consent.http.service.ontology;

public record ParentTerm(String id, String label, Integer order) {

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public Integer getOrder() {
    return order;
  }
}
