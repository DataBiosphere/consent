package org.broadinstitute.consent.http.service.ontology;

import static org.broadinstitute.consent.http.service.ontology.OwlClassFields.FIELD_DEFINITION_PROPERTY;
import static org.broadinstitute.consent.http.service.ontology.OwlClassFields.FIELD_DEPRECATED_PROPERTY;
import static org.broadinstitute.consent.http.service.ontology.OwlClassFields.FIELD_HAS_EXACT_SYNONYM_PROPERTY;
import static org.broadinstitute.consent.http.service.ontology.OwlClassFields.FIELD_LABEL_PROPERTY;
import static org.broadinstitute.consent.http.service.ontology.OwlClassFields.FIELD_OBO_ID_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLLiteral;

@ExtendWith(MockitoExtension.class)
class OwlClassFieldsTest {

  private OWLAnnotation createMockAnnotation(String propertyName, String value) {
    OWLAnnotation annotation = mock(OWLAnnotation.class);
    OWLAnnotationProperty property = mock(OWLAnnotationProperty.class);
    IRI iri = mock(IRI.class);
    OWLAnnotationValue annotationValue = mock(OWLAnnotationValue.class);
    OWLLiteral literal = mock(OWLLiteral.class);

    when(annotation.getProperty()).thenReturn(property);
    when(property.getIRI()).thenReturn(iri);
    when(iri.getRemainder()).thenReturn(Optional.of(propertyName));
    when(annotation.getValue()).thenReturn(annotationValue);
    when(annotationValue.asLiteral()).thenReturn(Optional.of(literal));
    when(literal.getLiteral()).thenReturn(value);

    return annotation;
  }

  @Test
  void testConstructorWithAllFields() {
    Set<OWLAnnotation> annotations = new HashSet<>();
    annotations.add(createMockAnnotation(FIELD_DEFINITION_PROPERTY, "Test definition"));
    annotations.add(createMockAnnotation(FIELD_HAS_EXACT_SYNONYM_PROPERTY, "Synonym 1"));
    annotations.add(createMockAnnotation(FIELD_HAS_EXACT_SYNONYM_PROPERTY, "Synonym 2"));
    annotations.add(createMockAnnotation(FIELD_LABEL_PROPERTY, "Test Label"));
    annotations.add(createMockAnnotation(FIELD_OBO_ID_PROPERTY, "DOID:1234"));
    annotations.add(createMockAnnotation(FIELD_DEPRECATED_PROPERTY, "true"));

    OwlClassFields fields = new OwlClassFields(annotations);

    assertEquals("Test definition", fields.getDefinition());
    assertEquals("Test Label", fields.getLabel());
    assertEquals("DOID_1234", fields.getOboId());
    assertTrue(fields.getDeprecated());
    assertNotNull(fields.getSynonyms());
    assertEquals(2, fields.getSynonyms().size());
  }

  @Test
  void testConstructorWithEmptyAnnotations() {
    Set<OWLAnnotation> annotations = new HashSet<>();

    OwlClassFields fields = new OwlClassFields(annotations);

    assertNull(fields.getDefinition());
    assertNull(fields.getLabel());
    assertNull(fields.getOboId());
    assertFalse(fields.getDeprecated());
    assertTrue(fields.getSynonyms().isEmpty());
  }

  @Test
  void testOboIdConversion() {
    Set<OWLAnnotation> annotations = new HashSet<>();
    annotations.add(createMockAnnotation(FIELD_OBO_ID_PROPERTY, "DUO:0000001"));

    OwlClassFields fields = new OwlClassFields(annotations);

    assertEquals("DUO_0000001", fields.getOboId());
  }

  @Test
  void testDeprecatedDefaultValue() {
    Set<OWLAnnotation> annotations = new HashSet<>();
    annotations.add(createMockAnnotation("label", "Test"));

    OwlClassFields fields = new OwlClassFields(annotations);

    assertFalse(fields.getDeprecated());
  }
}
