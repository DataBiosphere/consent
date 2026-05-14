package org.broadinstitute.consent.http.mail.message;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewLibraryCardIssuedMessageTest extends AbstractMailMessageTest {

  @Test
  void testNewLibraryCardIssuedTemplate() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setEmail("test.user@test.com");
    toUser.setUserId(1);
    String serverUrl = "http://localhost:8080/";
    String expectedUrl = serverUrl + "datalibrary";
    var message = new NewLibraryCardIssuedMessage(toUser);
    assertEquals(toUser.getEmail(), message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Your Library Card Has Been Issued",
        rendered.document().title());
    assertEquals(
        "Hello %s,".formatted(toUser.getDisplayName()),
        getElementTextById(rendered.document(), "userName"));
    assertThat(
        getElementTextById(rendered.document(), "content"),
        containsString(
            "You can now initiate data access requests. Get started by searching for data you would like to access in the DUOS Data Library."));
    assertTrue(
        Objects.requireNonNull(rendered.document().getElementById("content"))
            .html()
            .contains(expectedUrl));
  }
}
