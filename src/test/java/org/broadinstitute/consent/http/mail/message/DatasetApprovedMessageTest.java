package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DatasetApprovedMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var message =
        new DatasetApprovedMessage(toUser, "dac name", "dataset Identifier", "dataset name");

    assertRequiredModelFields(
        message,
        Map.of(
            "dataSubmitterName",
            "researcher name",
            "datasetIdentifier",
            "dataset Identifier",
            "datasetName",
            "dataset name",
            "dacName",
            "dac name"));
  }

  @Test
  void testGetDatasetApprovedTemplate() throws Exception {
    var serverUrl = "http://localhost:8000/#/";
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var datasetIdentifier = "dataset Identifier";
    var datasetName = "dataset name";
    var message = new DatasetApprovedMessage(toUser, "dac name", datasetIdentifier, datasetName);
    assertEquals(datasetIdentifier, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Admin - Dataset Approved Notification",
        rendered.document().title());
    assertTrue(
        Objects.requireNonNull(rendered.document().getElementById("content"))
            .text()
            .contains(
                "Your dataset, dataset name, submitted to the dac name for management of future data access requests has "
                    + "been accepted and can be found in the DUOS Data Library with this URL: dataset Identifier"));
    assertFalse(rendered.content().contains("${"));
  }
}
