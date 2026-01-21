package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import java.io.FileInputStream;
import java.util.Collection;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyDAOTest extends DAOTestHelper {

  @Mock private GCSService gcsService;
  @Mock private StoreConfiguration storeConfiguration;

  @Test
  void testInsertTerms() throws Exception {
    when(storeConfiguration.getBucket()).thenReturn("my-bucket");
    when(gcsService.getDocument(any(BlobId.class)))
        .thenReturn(new FileInputStream("src/test/resources/duo.owl"));
    OntologyIndexService indexer = new OntologyIndexService(gcsService, storeConfiguration);
    Collection<OntologyTerm> terms = indexer.generateTerms("duo.owl", "DOID");
    User user = createUser();
    ontologyDAO.batchInsertTerms(terms, user.getUserId());
    int count = ontologyDAO.countTerms();
    assertEquals(terms.size(), count);
  }
}
