package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
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
  private final Gson gson = GsonUtil.getInstance();

  private Collection<OntologyTerm> batchInsertTerms() throws Exception {
    when(storeConfiguration.getBucket()).thenReturn("my-bucket");
    when(gcsService.getDocument(any(BlobId.class)))
        .thenReturn(new FileInputStream("src/test/resources/duo.owl"));
    OntologyIndexService indexer = new OntologyIndexService(gcsService, storeConfiguration);
    Collection<OntologyTerm> terms = indexer.generateTerms(OntologyType.DUO);
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

  @Test
  void testDeleteTerms() throws Exception {
    Collection<OntologyTerm> terms = batchInsertTerms();
    int count = ontologyDAO.countTerms();
    assertEquals(terms.size(), count);
    ontologyDAO.deleteByOntology(OntologyType.DUO.name());
    int count2 = ontologyDAO.countTerms();
    assertEquals(0, count2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "DUO_0000006", // normalized obo id
        " DUO_0000007 ", // normalized obo id with spaces
        "http://purl.obolibrary.org/obo/DUO_0000006" // full term_id
      })
  void testFindByIds(String id) throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByTermIds(new String[] {id});
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertEquals(1, jsonArray.size());
  }

  @Test
  void testFindByIdsMultiple() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByTermIds(new String[] {"DUO_0000006", "DUO_0000007"});
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertEquals(2, jsonArray.size());
  }

  @Test
  void testFindByQuery() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery("data use modifier", OntologyType.DUO, 5);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertEquals(5, jsonArray.size());
  }

  @Test
  void testFindByQueryRanked() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery("health medical biomedical", null, null);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    List<OntologyTerm> terms =
        jsonArray.asList().stream().map(t -> gson.fromJson(t, OntologyTerm.class)).toList();
    // In this example, DUO_0000037 contains the query terms more frequently than DUO_0000006, so it
    // should be ranked higher and returned first in the results.
    assertTrue(terms.getFirst().id().contains("DUO_0000037"));
    assertTrue(terms.get(1).id().contains("DUO_0000006"));
  }
}
