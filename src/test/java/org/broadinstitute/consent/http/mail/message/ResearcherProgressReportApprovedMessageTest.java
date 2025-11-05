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

class ResearcherProgressReportApprovedMessageTest extends AbstractTestHelper {

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
    var message =
        new ResearcherApprovedProgressReportMessage(new User(), "DAR-123", List.of(), "", false);
    assertEquals("Your DUOS Progress Report Results", message.createSubject());
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
        new ResearcherApprovedProgressReportMessage(
            researcher, darCode, List.of(new DatasetMailDTO(datasetName, datasetId)), "", false);
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
    assertTrue(
        templateString.contains("Your progress report application " + darCode + " was approved"));
    assertTrue(templateString.contains(datasetId));
    assertTrue(templateString.contains(datasetName));
    assertTrue(templateString.contains(researcherEmail));
  }

  @Test
  void testGetResearcherRADARApprovedTemplate() throws Exception {
    String researcherUserName = randomAlphabetic(10);
    String researcherEmail = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String datasetName = randomAlphabetic(10);
    String datasetId = randomAlphabetic(10);
    User researcher = new User();
    researcher.setDisplayName(researcherUserName);
    researcher.setEmail(researcherEmail);

    var message =
        new ResearcherApprovedProgressReportMessage(
            researcher, darCode, List.of(new DatasetMailDTO(datasetName, datasetId)), "", true);
    assertEquals(darCode, message.getEntityReferenceId());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(""), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Researcher - Your access to a dataset was Rule Automated DAR (RADAR) approved",
        parsedTemplate.title());
    assertEquals(
        "Hello " + researcherUserName + ",", getElementTextById(parsedTemplate, "userName"));
    assertTrue(
        templateString.contains(
            "Your progress report application "
                + darCode
                + " was Rule Automated DAR (RADAR) approved"));
    assertTrue(templateString.contains(datasetId));
    assertTrue(templateString.contains(datasetName));
    assertTrue(templateString.contains(researcherEmail));
  }
}
