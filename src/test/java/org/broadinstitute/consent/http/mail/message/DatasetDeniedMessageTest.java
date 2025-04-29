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

class DatasetDeniedMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testGetDatasetApprovedTemplate() throws Exception {
    var serverUrl = "http://localhost:8000/#/";
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var datasetName = "dataset name";
    var message = new DatasetDeniedMessage(toUser, "dac name", datasetName, "dac email");
    assertEquals(datasetName, message.getEntityReferenceId());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Admin - Dataset Denied Notification",
        parsedTemplate.title());
    assertTrue(
        Objects.requireNonNull(parsedTemplate.getElementById("content"))
            .text()
            .contains(
                "dataset, dataset name, submitted to the dac name by researcher name for management of future data "
                    + "access requests has been rejected. Please contact the DAC directly at dac email for questions."));

    // no unspecified values
    assertFalse(templateString.contains("${"));
  }
}
