package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

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
    JSONAssert.assertEquals(expectedDarData, resultingDarData.toString(), false);
  }

  @Test
  void testGetSetItDirectorEmail() {
    DataAccessRequestData data = new DataAccessRequestData();

    assertNull(data.getItDirectorEmail());

    String testEmail = "it@example.broadinstitute.org";
    data.setItDirectorEmail(testEmail);

    assertEquals(testEmail, data.getItDirectorEmail());
  }

  @Test
  void testGetSetPiEmail() {
    DataAccessRequestData data = new DataAccessRequestData();

    assertNull(data.getPiEmail());

    String testEmail = "pi@example.broadinstitute.org";
    data.setPiEmail(testEmail);

    assertEquals(testEmail, data.getPiEmail());
  }

  @Test
  void testGetSetSigningOfficialEmail() {
    DataAccessRequestData data = new DataAccessRequestData();

    assertNull(data.getSigningOfficialEmail());

    String testEmail = "so@example.broadinstitute.org";
    data.setSigningOfficialEmail(testEmail);

    assertEquals(testEmail, data.getSigningOfficialEmail());
  }
}
