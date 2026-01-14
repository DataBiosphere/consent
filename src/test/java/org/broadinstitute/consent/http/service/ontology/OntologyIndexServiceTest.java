package org.broadinstitute.consent.http.service.ontology;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OntologyIndexServiceTest {

  @Test
  public void testIndexStream() throws Exception {
    InputStream inputStream = new FileInputStream("src/test/resources/doid.owl");
    OntologyIndexService indexer = new OntologyIndexService();
    Collection<OntologyTerm> terms = indexer.generateTerms(inputStream, "DOID");
    assertFalse(terms.isEmpty());
  }
}
