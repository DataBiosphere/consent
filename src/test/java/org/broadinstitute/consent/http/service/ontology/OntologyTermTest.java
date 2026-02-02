package org.broadinstitute.consent.http.service.ontology;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  private OntologyTerm generateTerm() {
    OntologyTerm term = new OntologyTerm("http://purl.obolibrary.org/obo/DUO_0000006", "v1", "DUO");
    term.setSynonyms(List.of("GRU", "General Use"));
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
    return term;
  }

  @Test
  void testOntologyTermSerialization() {
    OntologyTerm term = generateTerm();

    String json = term.toString();
    JsonElement jsonElement = GsonUtil.getInstance().fromJson(json, JsonElement.class);
    assertTrue(jsonElement.getAsJsonObject().has("id"));
    assertEquals(term.id(), jsonElement.getAsJsonObject().get("id").getAsString());
    assertTrue(jsonElement.getAsJsonObject().has("ontology"));
    assertEquals(term.ontology(), jsonElement.getAsJsonObject().get("ontology").getAsString());
    assertTrue(jsonElement.getAsJsonObject().has("synonyms"));
    jsonElement.getAsJsonObject().get("synonyms").getAsJsonArray().asList().stream()
        .map(JsonElement::getAsString)
        .forEach(s -> assertTrue(term.synonyms().contains(s)));
    assertTrue(jsonElement.getAsJsonObject().has("label"));
    assertEquals(term.label(), jsonElement.getAsJsonObject().get("label").getAsString());
    assertTrue(jsonElement.getAsJsonObject().has("definition"));
    assertEquals(term.definition(), jsonElement.getAsJsonObject().get("definition").getAsString());
    assertTrue(jsonElement.getAsJsonObject().has("parents"));
    assertEquals(
        term.getParents().size(),
        jsonElement.getAsJsonObject().get("parents").getAsJsonArray().size());

    // These fields are not exposed and should not be present in the JSON
    assertFalse(jsonElement.getAsJsonObject().has("version"));
    assertFalse(jsonElement.getAsJsonObject().has("usable"));
    assertFalse(jsonElement.getAsJsonObject().has("oboId"));

    // Test parent terms
    JsonArray parents = jsonElement.getAsJsonObject().get("parents").getAsJsonArray();
    ParentTerm firstParent = term.getParents().getFirst();
    parents.forEach(
        p -> {
          var parentObj = p.getAsJsonObject();
          assertTrue(parentObj.has("id"));
          assertEquals(firstParent.getId(), parentObj.get("id").getAsString());
          assertTrue(parentObj.has("label"));
          assertEquals(firstParent.getLabel(), parentObj.get("label").getAsString());
          assertTrue(parentObj.has("order"));
          assertEquals(firstParent.getOrder(), parentObj.get("order").getAsInt());
          assertTrue(parentObj.has("definition"));
          assertEquals(firstParent.getDefinition(), parentObj.get("definition").getAsString());
          assertTrue(parentObj.has("synonyms"));
          parentObj.get("synonyms").getAsJsonArray().asList().stream()
              .map(JsonElement::getAsString)
              .forEach(s -> assertTrue(firstParent.getSynonyms().contains(s)));
        });
  }
}
