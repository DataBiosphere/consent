package org.broadinstitute.consent.http.mail.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewStudyDigestMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testNewStudyDigestMessage() throws TemplateException, IOException {
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
    assertDoesNotThrow(
        () -> {
          message.createModel(serverUrl);
        });

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    String urlStringPattern = "%sstudies/%d\">%s";
    assertTrue(templateString.contains(user.getDisplayName()));
    assertEquals(
        "Dear Test User,",
        Objects.requireNonNull(parsedTemplate.getElementById("userName")).text());
    assertThat(
        parsedTemplate.body().html(),
        containsString(urlStringPattern.formatted(serverUrl, record1.id(), record1.name())));
    assertThat(
        parsedTemplate.body().html(),
        containsString(urlStringPattern.formatted(serverUrl, record2.id(), record2.name())));
  }
}
