package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewStudyRegistrationConfirmationMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testMessageTemplate() throws IOException, TemplateException {
    User submitter = new User();
    submitter.setDisplayName("Test User");
    String studyName = "Cancer Research";
    Integer studyId = 123;
    Map<String, Object> assets = Map.of("assetType", List.of("asset1", "asset2"));

    var message =
        new NewStudyRegistrationConfirmationMessage(submitter, studyName, studyId, assets);

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel("localhost:8080"), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertTrue(parsedTemplate.text().contains("Test User"));
    assertTrue(parsedTemplate.text().contains(studyName));
    assertTrue(parsedTemplate.text().contains(String.valueOf(studyId)));
    assertTrue(parsedTemplate.text().contains("assetType"));
    assertTrue(parsedTemplate.text().contains("2 item(s)"));
  }
}
