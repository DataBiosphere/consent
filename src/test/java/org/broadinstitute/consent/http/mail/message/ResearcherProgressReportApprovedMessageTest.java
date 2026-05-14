package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.junit.jupiter.api.Test;

class ResearcherProgressReportApprovedMessageTest extends AbstractMailMessageTest {

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
    String dataLocationUrl = randomAlphabetic(10);
    User researcher = new User();
    researcher.setDisplayName(researcherUserName);
    researcher.setEmail(researcherEmail);

    var message =
        new ResearcherApprovedProgressReportMessage(
            researcher,
            darCode,
            List.of(new DatasetMailDTO(datasetName, datasetId, dataLocationUrl)),
            "",
            false);
    assertEquals(darCode, message.getEntityReferenceId());

    var rendered = renderTemplate(message, "");

    assertEquals(
        "Broad Data Use Oversight System - Researcher - Your access to a dataset was approved",
        rendered.document().title());
    assertEquals(
        "Hello " + researcherUserName + ",", getElementTextById(rendered.document(), "userName"));
    assertTrue(
        rendered
            .content()
            .contains("Your progress report application " + darCode + " was approved"));
    assertTrue(rendered.content().contains(datasetId));
    assertTrue(rendered.content().contains(datasetName));
    // Positive test to ensure data location is in the template.
    assertTrue(rendered.content().contains(dataLocationUrl));
    assertTrue(rendered.content().contains(researcherEmail));
  }

  @Test
  void testGetResearcherRADARApprovedTemplate() throws Exception {
    String researcherUserName = randomAlphabetic(10);
    String researcherEmail = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String datasetName = randomAlphabetic(10);
    String datasetId = randomAlphabetic(10);
    String dataLocation = randomAlphabetic(10);
    User researcher = new User();
    researcher.setDisplayName(researcherUserName);
    researcher.setEmail(researcherEmail);

    var message =
        new ResearcherApprovedProgressReportMessage(
            researcher,
            darCode,
            List.of(new DatasetMailDTO(datasetName, datasetId, null)),
            "",
            true);
    assertEquals(darCode, message.getEntityReferenceId());

    var rendered = renderTemplate(message, "");

    assertEquals(
        "Broad Data Use Oversight System - Researcher - Your access to a dataset was Rule Automated DAR (RADAR) approved",
        rendered.document().title());
    assertEquals(
        "Hello " + researcherUserName + ",", getElementTextById(rendered.document(), "userName"));
    assertTrue(
        rendered
            .content()
            .contains(
                "Your progress report application "
                    + darCode
                    + " was Rule Automated DAR (RADAR) approved"));
    assertTrue(rendered.content().contains(datasetId));
    assertTrue(rendered.content().contains(datasetName));
    // Negative test to ensure data location is not in the template.
    assertFalse(rendered.content().contains(dataLocation));
    assertTrue(rendered.content().contains(researcherEmail));
  }
}
