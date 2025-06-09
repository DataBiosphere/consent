package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Objects;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResearcherCloseoutCompletedMessageTest extends AbstractTestHelper {

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
  void testGetResearcherCloseoutCompletedTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String referenceId = UUID.randomUUID().toString();
    User requestUser = new User();
    requestUser.setDisplayName(userName);

    var message = new ResearcherCloseoutCompletedMessage(requestUser, darCode, referenceId);
    assertEquals(referenceId, message.getEntityReferenceId());

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel("localhost:8080"), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals("Broad Data Use Oversight System - Researcher - Closeout Complete",
        parsedTemplate.title());
    assertEquals("Dear %s,".formatted(userName), getElementTextById(parsedTemplate, "userName"));
    assertEquals(
        "The closeout on Data Access Request (DAR) %s has been approved and your access to all datasets in this DAR will be revoked unless you have permission to use that data under another DAR.".formatted(
            darCode), getElementTextById(parsedTemplate, "content"));
    assertTrue(getElementTextById(parsedTemplate, "warning").contains(
        "you have agreed to destroy all copies"));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }
}
