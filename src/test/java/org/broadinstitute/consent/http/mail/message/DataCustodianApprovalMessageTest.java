package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataCustodianApprovalMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testGetDataCustodianApprovalTemplate() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Data Custodian");
    String datasetName = "dataset name";
    List<DatasetMailDTO> datasetMailDTOs = List.of(new DatasetMailDTO(datasetName, "dataset id"));
    var serverUrl = "http://localhost:8000/#/";
    String darCode = "Dar Code";

    var message =
        new DataCustodianApprovalMessage(
            toUser, darCode, datasetMailDTOs, "Depositor", "researcher@email.com", false);
    assertEquals(darCode, message.getEntityReferenceId());
    assertEquals("Dar Code has been approved by the DAC", message.createSubject());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Researcher - A researcher was approved for your dataset",
        parsedTemplate.title());
    assertTrue(
        Objects.requireNonNull(parsedTemplate.getElementById("content"))
            .text()
            .contains("researcher@email.com was approved by the DAC for the following datasets"));

    assertTrue(templateString.contains(datasetName));

    // no unspecified values
    assertFalse(templateString.contains("${"));

    assertFalse(templateString.toLowerCase().contains("radar"));
  }

  @Test
  void testGetDataCustodianRADARApprovalTemplate() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Data Custodian");
    String datasetName = "dataset name";
    List<DatasetMailDTO> datasetMailDTOs = List.of(new DatasetMailDTO(datasetName, "dataset id"));
    var serverUrl = "http://localhost:8000/#/";
    String darCode = "Dar Code";

    var message =
        new DataCustodianApprovalMessage(
            toUser, darCode, datasetMailDTOs, "Depositor", "researcher@email.com", true);
    assertEquals(
        "Dar Code has been Rule Automated DAR (RADAR) approved by the DAC",
        message.createSubject());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Researcher - A researcher was Rule Automated DAR (RADAR) approved for your dataset",
        parsedTemplate.title());
    assertTrue(
        Objects.requireNonNull(parsedTemplate.getElementById("content"))
            .text()
            .contains(
                "researcher@email.com was Rule Automated DAR (RADAR) approved by the DAC for the following datasets"));
  }
}
