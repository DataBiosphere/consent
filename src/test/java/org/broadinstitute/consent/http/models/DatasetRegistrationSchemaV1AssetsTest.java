package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.junit.jupiter.api.Test;

class DatasetRegistrationSchemaV1AssetsTest {

  @Test
  void testAssetsFieldCanBeSetAndRetrieved() {
    // Create a DatasetRegistrationSchemaV1 instance
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();

    // Create a sample assets object
    Map<String, Object> assets = new HashMap<>();
    assets.put(
        "workspaces",
        Map.of(
            "workspace_id",
            "c7b96ac5-5568-441c-a3f4-2e82e45e3e6d",
            "name",
            "Cardiometabolic GWAS Analysis Workspace",
            "platform",
            "Terra"));
    assets.put(
        "funding",
        Map.of("funding_id", "fund-0001", "funder_name", "NIH", "grant_number", "R01HG012345"));

    // Set the assets
    registration.setAssets(assets);

    // Verify the assets can be retrieved
    Map<String, Object> retrievedAssets = registration.getAssets();
    assertNotNull(retrievedAssets);
    assertEquals(2, retrievedAssets.size());
    assertNotNull(retrievedAssets.get("workspaces"));
  }

  @Test
  void testAssetsFieldSerializationWithGson() {
    // Create a DatasetRegistrationSchemaV1 instance with assets
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();
    registration.setStudyName("Test Study");

    Map<String, Object> assets = new HashMap<>();
    assets.put("customField", "customValue");
    assets.put("nestedObject", Map.of("key1", "value1", "key2", 123));
    registration.setAssets(assets);

    // Serialize to JSON
    Gson gson = new Gson();
    String json = gson.toJson(registration);

    // Verify JSON contains assets
    assertTrue(json.contains("\"assets\""));
    assertTrue(json.contains("\"customField\""));
    assertTrue(json.contains("\"customValue\""));

    // Deserialize back
    DatasetRegistrationSchemaV1 deserialized =
        gson.fromJson(json, DatasetRegistrationSchemaV1.class);
    assertNotNull(deserialized.getAssets());
    assertEquals("customValue", deserialized.getAssets().get("customField"));
  }

  @Test
  void testAssetsFieldIsOptional() {
    // Create a DatasetRegistrationSchemaV1 instance without setting assets
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();
    registration.setStudyName("Test Study");

    // Verify assets is initialized as empty map
    Map<String, Object> assets = registration.getAssets();
    assertNotNull(assets);
    assertTrue(assets.isEmpty());
  }
}
