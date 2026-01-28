package org.broadinstitute.consent.http.service.ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLLiteral;

public class OwlClassFields {

  private static final String FIELD_DEFINITION_PROPERTY = "IAO_0000115";
  private static final String FIELD_HAS_EXACT_SYNONYM_PROPERTY = "hasExactSynonym";
  private static final String FIELD_LABEL_PROPERTY = "label";
  private static final String FIELD_OBO_ID_PROPERTY = "id";
  private static final String FIELD_DEPRECATED_PROPERTY = "deprecated";

  private String definition;
  private final List<String> synonyms = new ArrayList<>();
  private String label;
  private String oboId;
  private Boolean deprecated = false;

  public OwlClassFields(Set<OWLAnnotation> classAnnotations) {
    for (OWLAnnotation annotation : classAnnotations) {
      String propertyName = annotation.getProperty().getIRI().getRemainder().orElse("");
      String propertyValue =
          annotation.getValue().asLiteral().map(OWLLiteral::getLiteral).orElse("");
      if (propertyName.equals(FIELD_DEFINITION_PROPERTY)) {
        this.definition = propertyValue;
      }
      if (propertyName.equals(FIELD_HAS_EXACT_SYNONYM_PROPERTY)) {
        synonyms.add(propertyValue);
      }
      if (propertyName.equals(FIELD_LABEL_PROPERTY)) {
        label = propertyValue;
      }
      if (propertyName.equals(FIELD_OBO_ID_PROPERTY)) {
        // OBO IDs usually look like "DOID:1234" but are referenced with underscores in IRIs
        oboId = propertyValue.replace(":", "_");
      }
      if (propertyName.equals(FIELD_DEPRECATED_PROPERTY)) {
        deprecated = true;
      }
    }
  }

  public String getDefinition() {
    return definition;
  }

  public List<String> getSynonyms() {
    return synonyms;
  }

  public String getLabel() {
    return label;
  }

  public Boolean getDeprecated() {
    return deprecated;
  }

  public String getOboId() {
    return oboId;
  }
}
