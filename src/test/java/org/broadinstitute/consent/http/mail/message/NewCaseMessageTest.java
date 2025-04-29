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

class NewCaseMessageTest extends AbstractTestHelper {

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
    var message = new NewCaseMessage(new User(), "DUL-123", "Data Use Limitations");
    assertEquals("Log vote on Data Use Limitations case id: DUL-123.", message.createSubject());
    var message2 = new NewCaseMessage(new User(), "DAR-123", "Data Access");
    assertEquals("Log votes on Data Access Request case id: DAR-123.", message2.createSubject());
  }

  @Test
  void testGetNewCaseTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String referenceId = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User toUser = new User();
    toUser.setDisplayName(userName);

    var message = new NewCaseMessage(toUser, referenceId, "Data Use Limitations");
    assertEquals(referenceId, message.getEntityReferenceId());

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - New DAR ready for your vote", parsedTemplate.title());
    assertEquals("Hello " + userName + ",", getElementTextById(parsedTemplate, "userName"));
    assertTrue(
        templateString.contains(
            "Data Use Limitations Review case id " + referenceId + ", has been created"));
    assertTrue(templateString.contains(serverUrl));
  }
}
