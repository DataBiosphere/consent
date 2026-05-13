package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DatasetSubmittedMessageTest extends AbstractMailMessageTest {

  @Test
  void testGetDatasetSubmittedTemplate() throws Exception {
    var serverUrl = "http://localhost:8000/#/";
    User dacChair = new User();
    dacChair.setDisplayName("dacChairName");

    String datasetName = "testDataset";
    var message =
        new DatasetSubmittedMessage(dacChair, "dataSubmitterName", datasetName, "dacName");
    assertEquals(datasetName, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Signing Official - Dataset Submitted Notification",
        rendered.document().title());
    assertTrue(
        Objects.requireNonNull(rendered.document().getElementById("content"))
            .text()
            .contains(
                "A new dataset, "
                    + datasetName
                    + ", has been submitted to your DAC, dacName by dataSubmitterName. Please log in to DUOS to review and accept or reject management of this dataset."));
    assertFalse(rendered.content().contains("${"));
  }
}
