package org.broadinstitute.consent.http.util.gson.deserializer;

import static java.lang.Integer.parseInt;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.broadinstitute.consent.http.models.Publication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicationDeserializerTest {

  private Gson gson;

  @BeforeEach
  void setUp() {
    gson =
        new GsonBuilder()
            .registerTypeAdapter(Publication.class, new PublicationDeserializer())
            .create();
  }

  @Test
  void testDeserializeV1_WithAuthorsString() {
    String v1Json =
        "{\"authors\":\"John Doe, Jane Smith\",\"pubmedId\":\"12345\",\"date\":\"2023-01-15\"}";

    Publication result = gson.fromJson(v1Json, Publication.class);

    assertNotNull(result);
    assertNotNull(result.authors());
    assertEquals(2, result.authors().size());
    assertEquals("John Doe", result.authors().get(0).name());
    assertEquals("Jane Smith", result.authors().get(1).name());
    assertEquals("2023-01-15", result.publishedDate());
  }

  @Test
  void testDeserializeV1_WithEmptyAuthorsString() {
    String v1Json = "{\"authors\":\"\",\"pubmedId\":\"12345\"}";

    Publication result = gson.fromJson(v1Json, Publication.class);

    assertNotNull(result);
    assertNotNull(result.authors());
    assertTrue(result.authors().isEmpty());
  }

  @Test
  void testDeserializeV1_WithoutAuthors() {
    String v1Json = "{\"pubmedId\":\"12345\",\"date\":\"2023-01-15\"}";

    Publication result = gson.fromJson(v1Json, Publication.class);

    assertNotNull(result);
    assertEquals("2023-01-15", result.publishedDate());
  }

  @Test
  void testDeserializeV2_RemainsUnchanged() {
    String v2Json =
        "{\"publicationId\":1,\"title\":\"Research Paper\",\"authors\":[{\"name\":\"John Doe\"}],\"publishedDate\":\"2023-01-15\"}";

    Publication result = gson.fromJson(v2Json, Publication.class);

    assertNotNull(result);
    assertEquals(1, parseInt(result.publicationId()));
    assertEquals("Research Paper", result.title());
    assertNotNull(result.authors());
    assertEquals(1, result.authors().size());
    assertEquals("John Doe", result.authors().getFirst().name());
    assertEquals("2023-01-15", result.publishedDate());
  }

  @Test
  void testDeserializeV1_MigratesDateToPublishedDate() {
    String v1Json = "{\"pubmedId\":\"12345\",\"date\":\"2023-01-15\"}";

    Publication result = gson.fromJson(v1Json, Publication.class);

    assertNotNull(result);
    assertEquals("2023-01-15", result.publishedDate());
  }
}
