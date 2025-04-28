package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewDARRequestMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testGetNewDARRequestTemplate() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Admin");

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-01");

    Dataset d1 = new Dataset();
    d1.setDacId(1);
    d1.setDatasetName("Dataset-01");
    d1.setDatasetId(1);
    d1.setAlias(1);
    d1.setDatasetIdentifier();

    var dacDatasetGroups = Map.of(dac.getName(), List.of(d1.getDatasetIdentifier()));
    String darCode = "DAR-01";
    var message = new NewDARRequestMessage(toUser, darCode, dacDatasetGroups, "ResearcherName");
    assertEquals(darCode, message.getEntityReferenceId());
    assertEquals(
        "Create an election for Data Access Request id: DAR-01.", message.createSubject());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    String serverUrl = "http://testServerUrl";
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals("Broad Data Use Oversight System - New DAR submitted to your DAC",
        parsedTemplate.title());
    assertEquals(
        "Hello Admin,",
        Objects.requireNonNull(parsedTemplate.getElementById("userName")).text());
    assertTrue(templateString.contains(darCode));
    assertTrue(templateString.contains(serverUrl));
    assertTrue(templateString.contains(dac.getName()));
    assertTrue(templateString.contains(d1.getDatasetIdentifier()));
  }
}
