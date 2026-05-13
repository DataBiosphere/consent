package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewDAAUploadResearcherMessageTest extends AbstractMailMessageTest {

  @Test
  void testGetNewDaaUploadSOTemplate() throws Exception {
    String researcherUserName = randomAlphabetic(10);
    String dacName = randomAlphabetic(10);
    String newDaaName = randomAlphabetic(10);
    String previousDaaName = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User researcher = new User();
    researcher.setDisplayName(researcherUserName);

    var message =
        new NewDAAUploadResearcherMessage(researcher, dacName, previousDaaName, newDaaName);
    assertEquals(dacName, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement Upload",
        rendered.document().title());
    assertTrue(
        getElementTextById(rendered.document(), "userName")
            .contains("Dear " + researcherUserName + ","));
    String content = getElementTextById(rendered.document(), "content");
    assertTrue(
        content.contains(
            "You were previously pre-authorized to request data from the "
                + dacName
                + " under the "
                + previousDaaName
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
