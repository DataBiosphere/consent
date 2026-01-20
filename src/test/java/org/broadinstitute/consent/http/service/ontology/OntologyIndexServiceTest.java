package org.broadinstitute.consent.http.service.ontology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import java.io.FileInputStream;
import java.util.Collection;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OntologyIndexServiceTest {

  @Mock GCSService gcsService;
  @Mock StoreConfiguration servicesConfiguration;

  @Test
  public void testIndexStream() throws Exception {
    when(gcsService.getDocument(any(BlobId.class)))
        .thenReturn(new FileInputStream("src/test/resources/doid.owl"));
    OntologyIndexService indexer = new OntologyIndexService(gcsService, servicesConfiguration);
    Collection<OntologyTerm> terms = indexer.generateTerms("doid.owl", "DOID");
    assertFalse(terms.isEmpty());
  }
}
