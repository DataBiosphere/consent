package org.broadinstitute.consent.http.service.ontology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.List;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyTermTest {

  @Test
  void testOntologyTermSerialization() {
    OntologyTerm term = new OntologyTerm("http://purl.obolibrary.org/obo/DUO_0000006", "v1", "DUO");
    term.setSynonyms(List.of("GRU"));
    term.setLabel("General Research Use");
    term.setDefinition("This data use limitation allows for general research use.");
    term.setUsable(true);
    term.setOboId("obo:DUO_0000006");
    ParentTerm parent = new ParentTerm();
    parent.setId("http://purl.obolibrary.org/obo/DUO_0000042");
    parent.setLabel("Health/Medical/Biomedical research");
    parent.setOrder(1);
    parent.setDefinition("This data use limitation allows for health/medical/biomedical research.");
    parent.setSynonyms(List.of("HMB"));
    term.addParent(parent);

    String json = term.toString();
    JsonElement jsonElement = GsonUtil.getInstance().fromJson(json, JsonElement.class);
    assertTrue(jsonElement.getAsJsonObject().has("id"));
    assertTrue(jsonElement.getAsJsonObject().has("ontology"));
    assertTrue(jsonElement.getAsJsonObject().has("synonyms"));
    assertTrue(jsonElement.getAsJsonObject().has("label"));
    assertTrue(jsonElement.getAsJsonObject().has("definition"));
    assertTrue(jsonElement.getAsJsonObject().has("parents"));

    // These fields are not exposed and should not be present in the JSON
    assertFalse(jsonElement.getAsJsonObject().has("version"));
    assertFalse(jsonElement.getAsJsonObject().has("usable"));
    assertFalse(jsonElement.getAsJsonObject().has("oboId"));

    // Test parent terms
    JsonArray parents = jsonElement.getAsJsonObject().get("parents").getAsJsonArray();
    parents.forEach(
        p -> {
          var parentObj = p.getAsJsonObject();
          assertTrue(parentObj.has("id"));
          assertTrue(parentObj.has("label"));
          assertTrue(parentObj.has("order"));
          assertTrue(parentObj.has("definition"));
          assertTrue(parentObj.has("synonyms"));
        });
  }
}
