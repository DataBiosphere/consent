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
    var message =
        new DatasetDeniedMessage(toUser, "dac name", "DUOS-000001", "dataset name", "dac email");

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

  /** The model names the dataset for the reader; the identifier is not part of it. */
  @Test
  void testCreateModel_UsesDatasetNameNotIdentifier() {
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var message =
        new DatasetDeniedMessage(toUser, "dac name", "DUOS-000001", "dataset name", "dac email");

    Map<String, Object> model = message.createModel();

    assertEquals("dataset name", model.get("datasetName"));
    assertFalse(model.containsValue("DUOS-000001"), "The identifier is not a model field");
  }

  /** The email is keyed on the dataset identifier, not on the submitter supplied name. */
  @Test
  void testGetEntityReferenceId_UsesDatasetIdentifier() {
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var message =
        new DatasetDeniedMessage(toUser, "dac name", "DUOS-000001", "dataset name", "dac email");

    assertEquals("DUOS-000001", message.getEntityReferenceId());
  }

  @Test
  void testGetDatasetApprovedTemplate() throws Exception {
    var serverUrl = "http://localhost:8000/#/";
    User toUser = new User();
    toUser.setDisplayName("researcher name");
    var datasetName = "dataset name";
    var datasetIdentifier = "DUOS-000001";
    var message =
        new DatasetDeniedMessage(toUser, "dac name", datasetIdentifier, datasetName, "dac email");
    assertEquals(datasetIdentifier, message.getEntityReferenceId());

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
    // The reader is told which dataset by name, so the identifier does not appear in the body.
    assertFalse(rendered.content().contains(datasetIdentifier));
    assertFalse(rendered.content().contains("${"));
  }
}
