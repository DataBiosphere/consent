package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewDAAUploadResearcherMessageTest extends AbstractTestHelper {

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
  void testGetNewDaaUploadSOTemplate() throws Exception {
    String researcherUserName = randomAlphabetic(10);
    String dacName = randomAlphabetic(10);
    String newDaaName = randomAlphabetic(10);
    String previousDaaName = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User researcher = new User();
    researcher.setDisplayName(researcherUserName);

    var message = new NewDAAUploadResearcherMessage(researcher, dacName, previousDaaName, newDaaName);
    assertEquals(dacName, message.getEntityReferenceId());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement Upload",
        parsedTemplate.title());
    assertTrue(
        getElementTextById(parsedTemplate, "userName")
            .contains("Dear " + researcherUserName + ","));
    String content = getElementTextById(parsedTemplate, "content");
    assertTrue(
        content.contains(
            "You were previously pre-authorized to request data from the "
                + dacName
                + " under the "
                + previousDaaName
                + "."));
    assertTrue(
        content.contains(
            "The "
                + dacName
                + " has recently transitioned to using the "
                + newDaaName
                + " which will apply for all future requests to this DAC."));

    // no unspecified values
    assertFalse(templateString.contains("${"));
  }
}
