package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DatasetDeniedMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var message = new DatasetDeniedMessage(toUser, "dac name", "dataset name", "dac email");

    assertRequiredModelFields(
        message,
        Map.of(
            "dataSubmitterName",
            "researcher name",
            "datasetName",
            "dataset name",
            "dacName",
            "dac name",
            "dacEmail",
            "dac email"));
  }

  @Test
  void testGetDatasetApprovedTemplate() throws Exception {
    var serverUrl = "http://localhost:8000/#/";
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var datasetName = "dataset name";
    var message = new DatasetDeniedMessage(toUser, "dac name", datasetName, "dac email");
    assertEquals(datasetName, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Admin - Dataset Denied Notification",
        rendered.document().title());
    assertTrue(
        Objects.requireNonNull(rendered.document().getElementById("content"))
            .text()
            .contains(
                "dataset, dataset name, submitted to the dac name by researcher name for management of future data "
                    + "access requests has been rejected. Please contact the DAC directly at dac email for questions."));
    assertFalse(rendered.content().contains("${"));
  }
}
