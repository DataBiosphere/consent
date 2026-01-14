package org.broadinstitute.consent.http.db;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
class OntologyDAOTest extends DAOTestHelper {

  @Test
  void testInsertTerms() throws Exception {
    InputStream inputStream = new FileInputStream("src/test/resources/doid.owl");
    OntologyIndexService indexer = new OntologyIndexService();
    Collection<OntologyTerm> terms = indexer.generateTerms(inputStream, "DOID");
    User user = createUser();
    ontologyDAO.insertTerms(user, terms);
    int count = ontologyDAO.countTerms();
    assertEquals(terms.size(), count);
  }
}
