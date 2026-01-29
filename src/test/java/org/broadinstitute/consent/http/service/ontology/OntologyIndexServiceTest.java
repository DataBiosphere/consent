package org.broadinstitute.consent.http.service.ontology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import java.io.FileInputStream;
import java.util.Collection;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

@ExtendWith(MockitoExtension.class)
class OntologyIndexServiceTest {

  private OntologyIndexService indexer;
  @Mock GCSService gcsService;
  @Mock StoreConfiguration storeConfiguration;

  @BeforeEach
  void setUp() {
    indexer = new OntologyIndexService(gcsService, storeConfiguration);
  }

  @Test
  void testGenerateTerms() throws Exception {
    Collection<OntologyTerm> terms = generateTerms();
    assertFalse(terms.isEmpty());
  }

  @Test
  void testGenerateTermWithParents() throws Exception {
    Collection<OntologyTerm> terms = generateTerms();

    // Find a term with parents to test
    OntologyTerm termWithParents =
        terms.stream()
            .filter(t -> t.getParents() != null && !t.getParents().isEmpty())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No terms with parents found"));

    // Verify term properties
    assertNotNull(termWithParents.id());
    assertNotNull(termWithParents.label());
    assertEquals(OntologyType.DUO.name(), termWithParents.ontology());
    assertTrue(termWithParents.usable());

    // Verify parent structure
    assertFalse(termWithParents.getParents().isEmpty());
    ParentTerm firstParent = termWithParents.getParents().getFirst();
    assertNotNull(firstParent.getId());
    assertNotNull(firstParent.getLabel());
    assertTrue(firstParent.getOrder() > 0);
  }

  @Test
  void testGenerateTermObsoleteClass() throws Exception {
    Collection<OntologyTerm> terms = generateTerms();

    // Verify no obsolete terms are marked as usable
    long obsoleteCount = terms.stream().filter(t -> !t.usable()).count();
    assertEquals(0, obsoleteCount, "There should be no obsolete terms marked as usable");

    // All terms returned should be usable (obsolete ones are filtered out)
    assertTrue(terms.stream().allMatch(OntologyTerm::usable));
  }

  @Test
  void testIsValidOWLClass() {
    IRI iri = IRI.create("http://purl.obolibrary.org/obo/DUO");
    OWLClass owlClass = new OWLClassImpl(iri);

    boolean isValid = indexer.isValidOWLClass(owlClass);
    assertTrue(isValid, "The OWLClass should be valid");
  }

  @Test
  void testIsValidOWLClassInvalidNull() {
    boolean isValid = indexer.isValidOWLClass(null);
    assertFalse(isValid, "The OWLClass should be invalid");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://purl.obolibrary.org/obo/BFO_0000001", // Is not a DOID or DUO class
        "http://www.w3.org/2002/07/owl#Thing", // OWL Thing class
        "http://www.w3.org/2002/07/owl#Nothing" // OWL Nothing class
      })
  void testIsValidOWLClassInvalid(String iriString) {
    IRI iri = IRI.create(iriString);
    OWLClass owlClass = new OWLClassImpl(iri);

    boolean isValid = indexer.isValidOWLClass(owlClass);
    assertFalse(isValid, "The OWLClass should be invalid");
  }

  /**
   * Helper method to generate ontology terms for testing.
   *
   * @return Collection of OntologyTerm objects populated from the DUO ontology file.
   * @throws Exception If there is an error during term generation.
   */
  private Collection<OntologyTerm> generateTerms() throws Exception {
    when(storeConfiguration.getBucket()).thenReturn("my-bucket");
    when(gcsService.getDocument(any(BlobId.class)))
        .thenReturn(new FileInputStream("src/test/resources/" + OntologyType.DUO.getFileName()));
    return indexer.generateTerms(OntologyType.DUO.getFileName(), OntologyType.DUO.name());
  }
}
