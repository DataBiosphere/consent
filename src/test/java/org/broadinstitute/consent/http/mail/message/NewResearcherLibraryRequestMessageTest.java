package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewResearcherLibraryRequestMessageTest extends AbstractMailMessageTest {

  @Test
  void testGetNewResearcherLibraryRequestTemplate() throws Exception {
    User researcher = new User();
    researcher.setDisplayName("John Doe");
    researcher.setUserId(123);
    User signingOfficial = new User();
    signingOfficial.setEmail("offical@institution");
    var serverUrl = "http://localhost:8000/#/";

    var message = new NewResearcherLibraryRequestMessage(signingOfficial, researcher);

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Request from your researcher for Library Card permissions",
        rendered.document().title());
    assertEquals(researcher.getUserId().toString(), message.getEntityReferenceId());
    assertTrue(
        Objects.requireNonNull(rendered.document().getElementById("content"))
            .text()
            .contains("A researcher from your institution, John Doe, has registered in DUOS"));
    assertEquals(
        serverUrl,
        Objects.requireNonNull(rendered.document().getElementById("serverUrl")).attr("href"));
    assertFalse(rendered.content().contains("${"));
  }
}
