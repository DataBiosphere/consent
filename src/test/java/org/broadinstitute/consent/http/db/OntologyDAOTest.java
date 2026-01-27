package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyDAOTest extends DAOTestHelper {

  @Mock private GCSService gcsService;
  @Mock private StoreConfiguration storeConfiguration;

  private Collection<OntologyTerm> batchInsertTerms() throws Exception {
    when(storeConfiguration.getBucket()).thenReturn("my-bucket");
    when(gcsService.getDocument(any(BlobId.class)))
        .thenReturn(new FileInputStream("src/test/resources/duo.owl"));
    OntologyIndexService indexer = new OntologyIndexService(gcsService, storeConfiguration);
    Collection<OntologyTerm> terms = indexer.generateTerms("duo.owl", "DUO");
    User user = createUser();
    ontologyDAO.batchInsertTerms(terms, user.getUserId());
    return terms;
  }

  @Test
  void testInsertTerms() throws Exception {
    Collection<OntologyTerm> terms = batchInsertTerms();
    int count = ontologyDAO.countTerms();
    assertEquals(terms.size(), count);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "DUO_0000006", // normalized obo id
        "DUO:0000006", // raw obo id
        "http://purl.obolibrary.org/obo/DUO_0000006" // full term_id
      })
  void testFindByTerms(String term) throws Exception {
    batchInsertTerms();
    List<JsonObject> terms = ontologyDAO.findByTerms(List.of(term));
    assertEquals(1, terms.size());
  }

  @Test
  void testFindByTermsMultiple() throws Exception {
    batchInsertTerms();
    List<JsonObject> terms = ontologyDAO.findByTerms(List.of("DUO:0000006", "DUO:0000007"));
    assertEquals(2, terms.size());
  }
}
