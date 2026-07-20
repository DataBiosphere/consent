package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewProgressReportRequestMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User toUser = new User();
    toUser.setDisplayName("Admin");
    var dacDatasetGroups = Map.of("DAC-01", List.of("DUOS-000001"));
    var message =
        new NewProgressReportRequestMessage(
            toUser, "DAR-01", "myReferenceId", dacDatasetGroups, "ResearcherName");

    assertRequiredModelFields(
        message,
        Map.of(
            "userName",
            "Admin",
            "dacDatasetGroups",
            dacDatasetGroups,
            "researcherUserName",
            "ResearcherName",
            "darID",
            "DAR-01"));
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
    var message =
        new NewProgressReportRequestMessage(
            toUser, darCode, "myReferenceId", dacDatasetGroups, "ResearcherName");
    assertEquals("myReferenceId", message.getEntityReferenceId());
    assertEquals("Create an election for Progress Report id: DAR-01.", message.createSubject());

    String serverUrl = "http://testServerUrl";
    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - New Progress Report submitted to your DAC",
        rendered.document().title());
    assertEquals(
        "Hello Admin,",
        Objects.requireNonNull(rendered.document().getElementById("userName")).text());
    assertTrue(rendered.content().contains(darCode));
    assertTrue(rendered.content().contains(serverUrl));
    assertTrue(rendered.content().contains(dac.getName()));
    assertTrue(rendered.content().contains(d1.getDatasetIdentifier()));
  }
}
