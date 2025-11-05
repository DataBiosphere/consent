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

class DatasetSubmittedMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testGetDatasetSubmittedTemplate() throws Exception {
    var serverUrl = "http://localhost:8000/#/";
    User dacChair = new User();
    dacChair.setDisplayName("dacChairName");

    String datasetName = "testDataset";
    var message =
        new DatasetSubmittedMessage(dacChair, "dataSubmitterName", datasetName, "dacName");
    assertEquals(datasetName, message.getEntityReferenceId());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Signing Official - Dataset Submitted Notification",
        parsedTemplate.title());
    assertTrue(
        Objects.requireNonNull(parsedTemplate.getElementById("content"))
            .text()
            .contains(
                "A new dataset, "
                    + datasetName
                    + ", has been submitted to your DAC, dacName by dataSubmitterName. Please log in to DUOS to review and accept or reject management of this dataset."));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }
}
