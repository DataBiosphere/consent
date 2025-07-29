package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DACMembersDARRADARApprovedMessageTest {

  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  @Test
  void testGetDACMembersDARRADARApprovedTemplate() throws Exception {
    User toUser = new User();
    toUser.setEmail("Dac.Member@example.org");
    toUser.setDisplayName("DAC Member");

    User researcherUser = new User();
    researcherUser.setDisplayName("Researcher");
    researcherUser.setEmail("researcher@example.org");
    String datasetName = "dataset-name";
    List<DatasetMailDTO> datasetMailDTOs = List.of(new DatasetMailDTO(datasetName, "DUOS-00001"));
    String serverUrl = "http://localhost:8080";
    String darCode = "DAR-0001";
    String referenceId = "abcd-12345";

    var message = new DACMembersDARRADARApprovedMessage(toUser, darCode, researcherUser, referenceId, datasetMailDTOs);
    assertEquals(referenceId, message.getEntityReferenceId());
    assertEquals("Broad Data Use Oversight System - Data Access Committee - Data Access Request DAR-0001 is Rule Automated DAR (RADAR) Approved", message.createSubject());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateContent = out.toString();
    Document parsedTemplate = Jsoup.parse(templateContent);

    assertEquals("Broad Data Use Oversight System - Data Access Committee - Access to a dataset was Rule Automated DAR (RADAR) approved", parsedTemplate.title());

    assertTrue(templateContent.contains(researcherUser.getDisplayName()));

    assertTrue(templateContent.contains(datasetName));
  }
}
