package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class DarExpirationReminderMessageTest extends AbstractTestHelper {

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
  void testGetDarExpiredTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String darReferenceId = UUID.randomUUID().toString();
    User requestUser = new User();
    requestUser.setDisplayName(userName);

    var message = new DarExpirationReminderMessage(requestUser, darCode, darReferenceId);
    assertEquals(darReferenceId, message.getEntityReferenceId());

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel("localhost:8080"), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Your DAR is about to expire", parsedTemplate.title());
    assertEquals("Hello %s,".formatted(userName), getElementTextById(parsedTemplate, "userName"));
    assertEquals(
        "Your Data Access Request %s is expiring in 30 days. Please complete a progress report to preserve your access to this data."
            .formatted(darCode),
        getElementTextById(parsedTemplate, "expirationWarning"));
    assertEquals(
        "Login to DUOS to submit a progress report.",
        getElementTextById(parsedTemplate, "loginLink"));
  }
}
