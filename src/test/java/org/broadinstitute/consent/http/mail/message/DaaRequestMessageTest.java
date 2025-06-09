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

class DaaRequestMessageTest extends AbstractTestHelper {

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
  void testGetDaaRequestTemplate() throws Exception {
    String signingOfficialUserName = randomAlphabetic(10);
    String userName = randomAlphabetic(10);
    String daaName = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User signingOfficial = new User();
    signingOfficial.setDisplayName(signingOfficialUserName);
    User requestUser = new User();
    requestUser.setDisplayName(userName);
    Integer daaId = 123;

    var message = new DaaRequestMessage(signingOfficial, requestUser, daaName, daaId);
    assertEquals(daaId.toString(), message.getEntityReferenceId());

    Template template = helper.getTemplate(message.getTemplateName());
    Writer out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    String templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);


    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement-Library Card Relationship Request for your Institution",
        parsedTemplate.title());
    assertTrue(
        getElementTextById(parsedTemplate, "userName")
            .contains("Hello " + signingOfficialUserName + ","));
    assertTrue(
        getElementTextById(parsedTemplate, "content")
            .contains(
                userName
                    + " has registered with your institution and is requesting you approve them under the "
                    + daaName
                    + " data access agreement, so that they can request access to data."));
    assertTrue(
        getElementTextById(parsedTemplate, "link")
            .contains("Please login to review " + userName + "'s Data Access Agreements."));
    assertTrue(templateString.contains(serverUrl));

    // no unspecified values
    assertFalse(templateString.contains("${"));
  }
}
