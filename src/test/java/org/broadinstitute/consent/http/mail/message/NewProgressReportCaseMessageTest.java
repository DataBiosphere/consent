package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Objects;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewProgressReportCaseMessageTest extends AbstractTestHelper {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  String getElementTextById(Document document, String id) {
    return Objects.requireNonNull(document.getElementById(id)).text();
  }

  @Test
  void testMessageSubject() {
    var message2 = new NewProgressReportCaseMessage(new User(), "DAR-123");
    assertEquals("Log votes on Progress Report case id: DAR-123.", message2.createSubject());
  }

  @Test
  void testGetNewCaseTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String referenceId = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User toUser = new User();
    toUser.setDisplayName(userName);

    var message = new NewProgressReportCaseMessage(toUser, referenceId);
    assertEquals(referenceId, message.getEntityReferenceId());

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - New Progress Report ready for your vote",
        parsedTemplate.title());
    assertEquals("Hello " + userName + ",", getElementTextById(parsedTemplate, "userName"));
    assertTrue(
        templateString.contains(
            "Progress Report Review case id " + referenceId + ", has been created"));
    assertTrue(templateString.contains(serverUrl));
  }
}
