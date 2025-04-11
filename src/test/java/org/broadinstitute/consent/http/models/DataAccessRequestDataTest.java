package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class DataAccessRequestDataTest {

  @Test
  void serialization() {
    String exampleDarData = """
        {
          "institution": "The Broad Institute of MIT and Harvard",
          "projectTitle": "title",
          "checkCollaborator": false,
          "checkNihDataOnly": false
        }""";
    DataAccessRequestData resultingDarData = DataAccessRequestData.fromString(exampleDarData);
    String expectedDarData = """
        {"projectTitle":"title","checkNihDataOnly":false}""";
    // does not include fields removed from the object (ex. checkCollaborator, institution)
    assertEquals(expectedDarData, resultingDarData.toString());
  }
}