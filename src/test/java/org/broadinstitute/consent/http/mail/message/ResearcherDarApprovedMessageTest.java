package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResearcherDarApprovedMessageTest extends AbstractTestHelper {

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
    var message = new ResearcherDarApprovedMessage(new User(), "DAR-123", List.of(), "");
    assertEquals("Your DUOS Data Access Request Results", message.createSubject());
  }

  @Test
  void testGetResearcherApprovedTemplate() throws Exception {
    String researcherUserName = randomAlphabetic(10);
    String researcherEmail = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String datasetName = randomAlphabetic(10);
    String datasetId = randomAlphabetic(10);
    User researcher = new User();
    researcher.setDisplayName(researcherUserName);
    researcher.setEmail(researcherEmail);

    var message =
        new ResearcherDarApprovedMessage(
            researcher, darCode, List.of(new DatasetMailDTO(datasetName, datasetId)), "");
    assertEquals(darCode, message.getEntityReferenceId());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(""), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Researcher - Your access to a dataset was approved",
        parsedTemplate.title());
    assertEquals(
        "Hello " + researcherUserName + ",", getElementTextById(parsedTemplate, "userName"));
    assertTrue(templateString.contains("Your data access request application " + darCode + " was approved"));
    assertTrue(templateString.contains(datasetId));
    assertTrue(templateString.contains(datasetName));
    assertTrue(templateString.contains(researcherEmail));
  }
}
