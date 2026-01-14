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

class NewDARSigningOfficialRequestMessageTest {

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
    User signingOfficial = new User();
    signingOfficial.setDisplayName("SO");

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-01");

    Dataset d1 = new Dataset();
    d1.setDacId(1);
    d1.setDatasetName("Dataset-01");
    d1.setDatasetId(1);
    d1.setAlias(1);
    d1.setDatasetIdentifier();

    String darCode = "DAR-01";
    var message =
        new NewDARSigningOfficialRequestMessage(
            signingOfficial, darCode, "ResearcherName");
    assertEquals(darCode, message.getEntityReferenceId());
    assertEquals("A data access request requires your approval: DAR-01.", message.createSubject());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    String serverUrl = "http://testServerUrl";
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);

    assertEquals(
        "Broad Data Use Oversight System - New DAR submitted that requires your approval", parsedTemplate.title());
    assertEquals(
        "Hello SO,", Objects.requireNonNull(parsedTemplate.getElementById("userName")).text());
    assertTrue(templateString.contains(" ResearcherName,"));
    assertTrue(templateString.contains(darCode));
    assertTrue(templateString.contains(serverUrl));
  }
}
