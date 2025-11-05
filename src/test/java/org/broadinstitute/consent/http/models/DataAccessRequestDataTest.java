package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class DataAccessRequestDataTest {

  @Test
  void serialization() {
    String exampleDarData =
        """
        {
          "institution": "The Broad Institute of MIT and Harvard",
          "projectTitle": "title",
          "checkCollaborator": false,
          "checkNihDataOnly": false
        }""";
    DataAccessRequestData resultingDarData = DataAccessRequestData.fromString(exampleDarData);
    String expectedDarData =
        """
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

  @Test
  void testPopulateDARDataWithValidJson() {
    String validJson =
        """
            {
                "projectTitle": "Test Project",
                "checkNihDataOnly": true
            }
        """;
    DataAccessRequestData result = DataAccessRequestData.populateDARData(validJson);
    assertNotNull(result);
    assertEquals("Test Project", result.getProjectTitle());
    assertTrue(result.getCheckNihDataOnly());
  }

  @Test
  void testPopulateDARDataWithInvalidJson() {
    String invalidJson =
        """
            {
                "projectTitle": "Test Project",
                "checkNihDataOnly": true,
        """;
    assertThrows(
        BadRequestException.class, () -> DataAccessRequestData.populateDARData(invalidJson));
  }

  @Test
  void testPopulateDARDataWithNullJson() {
    DataAccessRequestData result = DataAccessRequestData.populateDARData(null);
    assertNotNull(result);
    assertNull(result.getProjectTitle());
    assertNull(result.getCheckNihDataOnly());
  }
}
