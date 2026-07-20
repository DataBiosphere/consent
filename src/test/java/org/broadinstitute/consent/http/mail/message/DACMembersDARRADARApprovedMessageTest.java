package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.junit.jupiter.api.Test;

class DACMembersDARRADARApprovedMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User toUser = new User();
    toUser.setDisplayName("DAC Member");
    User researcherUser = new User();
    researcherUser.setDisplayName("Researcher");
    List<DatasetMailDTO> datasetMailDTOs =
        List.of(new DatasetMailDTO("dataset-name", "DUOS-00001", null));

    var message =
        new DACMembersDARRADARApprovedMessage(
            toUser, "DAR-0001", researcherUser, "abcd-12345", datasetMailDTOs);

    assertRequiredModelFields(
        message,
        Map.of(
            "userName",
            "DAC Member",
            "darCode",
            "DAR-0001",
            "researcherUserName",
            "Researcher",
            "datasets",
            datasetMailDTOs));
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
    List<DatasetMailDTO> datasetMailDTOs =
        List.of(new DatasetMailDTO(datasetName, "DUOS-00001", null));
    String serverUrl = "http://localhost:8080/";
    String darCode = "DAR-0001";
    String referenceId = "abcd-12345";

    var message =
        new DACMembersDARRADARApprovedMessage(
            toUser, darCode, researcherUser, referenceId, datasetMailDTOs);
    assertEquals(referenceId, message.getEntityReferenceId());
    assertEquals(
        "Broad Data Use Oversight System - Data Access Committee - Data Access Request DAR-0001 is Rule Automated DAR (RADAR) Approved",
        message.createSubject());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - Data Access Committee - Access to a dataset was Rule Automated DAR (RADAR) approved",
        rendered.document().title());
    assertTrue(rendered.content().contains(researcherUser.getDisplayName()));
    assertTrue(rendered.content().contains(datasetName));
  }
}
