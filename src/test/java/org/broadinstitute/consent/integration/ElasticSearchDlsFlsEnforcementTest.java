package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.elasticsearch.client.Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the {@link ElasticSearchContainerTests} harness delivers a cluster on which
 * Elasticsearch enforces document-level and field-level security, reached through the application's
 * own {@code ElasticSearchSupport}-built client.
 *
 * <p>This establishes the mechanism Epic D depends on. Once {@code accessPolicy} is indexed (Epic
 * B) and the auth context resolver exists (Epic C), Ticket D-5's tests assert the same way against
 * generated DLS queries and FLS grants instead of the literal role descriptor used here.
 */
class ElasticSearchDlsFlsEnforcementTest extends ElasticSearchContainerTests {

  private static final String MAPPING =
      """
      {"mappings":{"properties":{
        "datasetId":{"type":"keyword"},
        "datasetName":{"type":"keyword"},
        "secretField":{"type":"keyword"},
        "accessPolicy":{"properties":{"publicVisibility":{"type":"boolean"}}}}}}
      """;

  /** Role descriptor restricting reads to public documents and to two granted fields. */
  private static final String PUBLIC_ONLY_ROLE =
      """
      {"restricted":{"indices":[{"names":["%s"],"privileges":["read"],
        "query":"{\\"term\\":{\\"accessPolicy.publicVisibility\\":true}}",
        "field_security":{"grant":["datasetId","datasetName"]}}]}}
      """;

  @BeforeAll
  static void seedIndex() throws Exception {
    recreateIndex(datasetIndex(), MAPPING);
    indexDocument(
        datasetIndex(),
        "1",
        """
        {"datasetId":"DS-1","datasetName":"public dataset",
         "secretField":"MUST-NOT-BE-RETURNED","accessPolicy":{"publicVisibility":true}}
        """);
    indexDocument(
        datasetIndex(),
        "2",
        """
        {"datasetId":"DS-2","datasetName":"private dataset",
         "secretField":"MUST-NOT-BE-RETURNED","accessPolicy":{"publicVisibility":false}}
        """);
  }

  @Test
  void applicationClientAuthenticatesAgainstSecuredCluster() throws Exception {
    assertEquals(200, statusOf(new Request("GET", "/")));
  }

  @Test
  void trialLicenseIsActiveSoDlsFlsIsPermitted() throws Exception {
    JsonObject license = jsonResponse(new Request("GET", "/_license")).getAsJsonObject("license");
    assertEquals("trial", license.get("type").getAsString());
    assertEquals("active", license.get("status").getAsString());
  }

  @Test
  void privilegedClientSeesAllDocumentsAndFields() throws Exception {
    JsonObject results = jsonResponse(new Request("GET", "/" + datasetIndex() + "/_search"));
    assertEquals(2, totalHits(results));
    assertTrue(firstSource(results).has("secretField"));
  }

  @Test
  void documentLevelSecurityHidesNonPublicDocuments() throws Exception {
    String apiKey = createApiKey("dls-test", PUBLIC_ONLY_ROLE.formatted(datasetIndex()));
    JsonObject results = searchAsApiKey(datasetIndex(), apiKey);

    assertEquals(1, totalHits(results));
    assertEquals("DS-1", firstSource(results).get("datasetId").getAsString());
  }

  @Test
  void fieldLevelSecurityStripsUngrantedFields() throws Exception {
    String apiKey = createApiKey("fls-test", PUBLIC_ONLY_ROLE.formatted(datasetIndex()));
    JsonObject source = firstSource(searchAsApiKey(datasetIndex(), apiKey));

    assertTrue(source.has("datasetId"));
    assertTrue(source.has("datasetName"));
    assertFalse(source.has("secretField"));
    assertFalse(source.has("accessPolicy"));
  }

  private static int totalHits(JsonObject searchResponse) {
    return searchResponse.getAsJsonObject("hits").getAsJsonObject("total").get("value").getAsInt();
  }

  private static JsonObject firstSource(JsonObject searchResponse) {
    return searchResponse
        .getAsJsonObject("hits")
        .getAsJsonArray("hits")
        .get(0)
        .getAsJsonObject()
        .getAsJsonObject("_source");
  }
}
