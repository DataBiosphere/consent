package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmittedCloseoutMessageTest {

  private FreeMarkerTemplateHelper helper;

  private User toUser;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
    toUser = new User();
    toUser.setDisplayName("Test User");
  }

  @Test
  void testMessageSubject() {
    var message =
        new SubmittedCloseoutMessage(toUser, "DAR-123", "ref-456", "http://testServerUrl");
    assertEquals("DAR DAR-123 Closeout Available for Review", message.createSubject());
  }

  @Test
  void testGetSubmittedCloseoutTemplate() throws Exception {
    String darId = "DAR-123";
    String referenceId = "ref-456";
    String linkUrl = "http://testServerUrl";

    var message = new SubmittedCloseoutMessage(toUser, darId, referenceId, "http://testServerUrl");
    assertEquals(referenceId, message.getEntityReferenceId());

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel(linkUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertTrue(templateString.contains(toUser.getDisplayName()));
    assertTrue(templateString.contains(darId));
    assertTrue(templateString.contains("closeout for your review and approval"));
    assertEquals(
        "Hello Test User,",
        Objects.requireNonNull(parsedTemplate.getElementById("userName")).text());
  }
}
