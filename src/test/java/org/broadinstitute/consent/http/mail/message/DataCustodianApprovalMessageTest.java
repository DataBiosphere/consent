package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.junit.jupiter.api.Test;

class DataCustodianApprovalMessageTest extends AbstractMailMessageTest {

  @Test
  void testGetDataCustodianApprovalTemplate() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Data Custodian");
    String datasetName = "dataset name";
    List<DatasetMailDTO> datasetMailDTOs =
        List.of(new DatasetMailDTO(datasetName, "dataset id", null));
    var serverUrl = "http://localhost:8000/#/";
    String darCode = "Dar Code";

    var message =
        new DataCustodianApprovalMessage(
            toUser, darCode, datasetMailDTOs, "Depositor", "researcher@email.com", false);
    assertEquals(darCode, message.getEntityReferenceId());
    assertEquals("Dar Code has been approved by the DAC", message.createSubject());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Researcher - A researcher was approved for your dataset",
        rendered.document().title());
    assertTrue(
        Objects.requireNonNull(rendered.document().getElementById("content"))
            .text()
            .contains("researcher@email.com was approved by the DAC for the following datasets"));
    assertTrue(rendered.content().contains(datasetName));
    assertFalse(rendered.content().contains("${"));
    assertFalse(rendered.content().toLowerCase().contains("radar"));
  }

  @Test
  void testGetDataCustodianRADARApprovalTemplate() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Data Custodian");
    String datasetName = "dataset name";
    List<DatasetMailDTO> datasetMailDTOs =
        List.of(new DatasetMailDTO(datasetName, "dataset id", null));
    var serverUrl = "http://localhost:8000/#/";
    String darCode = "Dar Code";

    var message =
        new DataCustodianApprovalMessage(
            toUser, darCode, datasetMailDTOs, "Depositor", "researcher@email.com", true);
    assertEquals(
        "Dar Code has been Rule Automated DAR (RADAR) approved by the DAC",
        message.createSubject());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Researcher - A researcher was Rule Automated DAR (RADAR) approved for your dataset",
        rendered.document().title());
    assertTrue(
        Objects.requireNonNull(rendered.document().getElementById("content"))
            .text()
            .contains(
                "researcher@email.com was Rule Automated DAR (RADAR) approved by the DAC for the following datasets"));
  }
}
