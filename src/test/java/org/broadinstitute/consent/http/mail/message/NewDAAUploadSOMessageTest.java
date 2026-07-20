package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewDAAUploadSOMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User signingOfficial = new User();
    signingOfficial.setDisplayName("Signing Official User");

    var message = new NewDAAUploadSOMessage(signingOfficial, "DAC Name", "Previous DAA", "New DAA");

    assertRequiredModelFields(
        message,
        Map.of(
            "dacName",
            "DAC Name",
            "signingOfficialUserName",
            "Signing Official User",
            "previousDaaName",
            "Previous DAA",
            "newDaaName",
            "New DAA"));
  }

  @Test
  void testGetNewDaaUploadSOTemplate() throws Exception {
    String signingOfficialUserName = randomAlphabetic(10);
    String dacName = randomAlphabetic(10);
    String newDaaName = randomAlphabetic(10);
    String previousDaaName = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User signingOfficial = new User();
    signingOfficial.setDisplayName(signingOfficialUserName);

    var message = new NewDAAUploadSOMessage(signingOfficial, dacName, previousDaaName, newDaaName);
    assertEquals(dacName, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement Upload",
        rendered.document().title());
    assertTrue(
        getElementTextById(rendered.document(), "userName")
            .contains("Dear " + signingOfficialUserName + ","));
    String content = getElementTextById(rendered.document(), "content");
    assertTrue(
        content.contains(
            "You previously pre-authorized researchers under the "
                + previousDaaName
                + " which was in use by the "
                + dacName
                + "."));
    assertTrue(
        content.contains(
            "The "
                + dacName
                + " has recently transitioned to using the "
                + newDaaName
                + " which will apply for all future requests to this DAC."));
    assertFalse(rendered.content().contains("${"));
  }
}
