package org.broadinstitute.consent.http.mail.message;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewLibraryCardIssuedMessageTest {
  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testNewLibraryCardIssuedTemplate() throws IOException, TemplateException {
    User toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setEmail("test.user@test.com");
    toUser.setUserId(1);
    String serverUrl = "http://localhost:8080/";
    String expectedUrl = serverUrl + "datalibrary";
    var message = new NewLibraryCardIssuedMessage(toUser);
    assertEquals(toUser.getEmail(), message.getEntityReferenceId());

    var template = helper.getTemplate(message.getTemplateName());

    var out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);
    assertEquals(
        "Broad Data Use Oversight System - Your Library Card Has Been Issued",
        parsedTemplate.title());

    assertEquals(
        "Hello %s,".formatted(toUser.getDisplayName()),
        getElementTextById(parsedTemplate, "userName"));
    assertThat(
        getElementTextById(parsedTemplate, "content"),
        containsString(
            "You can now initiate data access requests. Get started by searching for data you would like to access in the DUOS Data Library."));
    assertTrue(
        Objects.requireNonNull(parsedTemplate.getElementById("content"))
            .html()
            .contains(expectedUrl));
  }

  String getElementTextById(Document document, String id) {
    return Objects.requireNonNull(document.getElementById(id)).text();
  }
}
