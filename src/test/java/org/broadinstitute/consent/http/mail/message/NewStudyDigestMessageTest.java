package org.broadinstitute.consent.http.mail.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewStudyDigestMessageTest extends AbstractMailMessageTest {

  @Test
  void testNewStudyDigestMessage() throws Exception {
    List<StudyDatasetCountRecord> newStudies = new ArrayList<>();
    StudyDatasetCountRecord record1 = new StudyDatasetCountRecord("My new study", 3, 1);
    StudyDatasetCountRecord record2 = new StudyDatasetCountRecord("My other new study", 4000, 2);
    newStudies.add(record1);
    newStudies.add(record2);
    String referenceId = "My reference id";
    User user = new User();
    user.setUserId(1);
    user.setEmail("testUser@duos.org");
    user.setDisplayName("Test User");
    String serverUrl = "http://localhost:8080/";

    var message = new NewStudyDigestMessage(user, newStudies, referenceId);

    assertEquals("New data in DUOS today!", message.createSubject());
    assertEquals(referenceId, message.getEntityReferenceId());
    assertDoesNotThrow(() -> message.createModel(serverUrl));

    var rendered = renderTemplate(message, serverUrl);

    String urlStringPattern = "%sstudies/%d\">%s";
    assertTrue(rendered.content().contains(user.getDisplayName()));
    assertEquals(
        "Dear Test User,",
        Objects.requireNonNull(rendered.document().getElementById("userName")).text());
    assertThat(
        rendered.document().body().html(),
        containsString(urlStringPattern.formatted(serverUrl, record1.id(), record1.name())));
    assertThat(
        rendered.document().body().html(),
        containsString(urlStringPattern.formatted(serverUrl, record2.id(), record2.name())));
  }
}
