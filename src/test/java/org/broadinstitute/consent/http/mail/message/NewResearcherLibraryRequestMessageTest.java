package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewResearcherLibraryRequestMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testGetNewResearcherLibraryRequestTemplate() throws Exception {
    User researcher = new User();
    researcher.setDisplayName("John Doe");
    researcher.setUserId(123);
    User signingOfficial = new User();
    signingOfficial.setEmail("offical@institution");
    var serverUrl = "http://localhost:8000/#/";

    var message = new NewResearcherLibraryRequestMessage(signingOfficial, researcher);

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Request from your researcher for Library Card permissions",
        parsedTemplate.title());
    assertEquals(researcher.getUserId().toString(), message.getEntityReferenceId());

    assertTrue(
        Objects.requireNonNull(parsedTemplate.getElementById("content"))
            .text()
            .contains("A researcher from your institution, John Doe, has registered in DUOS"));

    assertEquals(
        serverUrl,
        Objects.requireNonNull(parsedTemplate.getElementById("serverUrl")).attr("href"));

    // no unspecified values
    assertFalse(templateString.contains("${"));
  }
}
