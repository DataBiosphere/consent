package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewDARSigningOfficialRequestMessageTest extends AbstractMailMessageTest {

  @Test
  void testGetNewDARRequestTemplate() throws Exception {
    User signingOfficial = new User();
    signingOfficial.setDisplayName("SO");

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-01");

    Dataset d1 = new Dataset();
    d1.setDacId(1);
    d1.setDatasetName("Dataset-01");
    d1.setDatasetId(1);
    d1.setAlias(1);
    d1.setDatasetIdentifier();

    String darCode = "DAR-01";
    var message =
        new NewDARSigningOfficialRequestMessage(signingOfficial, darCode, "ResearcherName");
    assertEquals(darCode, message.getEntityReferenceId());
    assertEquals("A data access request requires your approval: DAR-01.", message.createSubject());

    String serverUrl = "http://testServerUrl";
    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - New DAR submitted that requires your approval",
        rendered.document().title());
    assertEquals(
        "Hello SO,", Objects.requireNonNull(rendered.document().getElementById("userName")).text());
    assertTrue(rendered.content().contains(" ResearcherName,"));
    assertTrue(rendered.content().contains(darCode));
    assertTrue(rendered.content().contains(serverUrl));
  }
}
