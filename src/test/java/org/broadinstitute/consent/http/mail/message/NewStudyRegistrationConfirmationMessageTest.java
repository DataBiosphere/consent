package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewStudyRegistrationConfirmationMessageTest extends AbstractMailMessageTest {

  @Test
  void testMessageTemplate_singleAsset() throws Exception {
    User submitter = new User();
    submitter.setDisplayName("Test User");
    String studyName = "Cancer Research";
    Integer studyId = 123;
    Map<String, Object> assets = Map.of("assetType", List.of("asset1"));

    var message =
        new NewStudyRegistrationConfirmationMessage(submitter, studyName, studyId, assets);

    var rendered = renderTemplate(message, "localhost:8080");

    assertTrue(rendered.document().text().contains("Test User"));
    assertTrue(rendered.document().text().contains(studyName));
    assertTrue(rendered.document().text().contains(String.valueOf(studyId)));
    assertTrue(rendered.document().text().contains("assetType"));
    assertTrue(rendered.document().text().contains("1 item"));
  }

  @Test
  void testMessageTemplate_multipleAssets() throws Exception {
    User submitter = new User();
    submitter.setDisplayName("Test User");
    String studyName = "Cancer Research";
    Integer studyId = 123;
    Map<String, Object> assets = Map.of("assetType", List.of("asset1", "asset2"));

    var message =
        new NewStudyRegistrationConfirmationMessage(submitter, studyName, studyId, assets);

    var rendered = renderTemplate(message, "localhost:8080");

    assertTrue(rendered.document().text().contains("Test User"));
    assertTrue(rendered.document().text().contains(studyName));
    assertTrue(rendered.document().text().contains(String.valueOf(studyId)));
    assertTrue(rendered.document().text().contains("assetType"));
    assertTrue(rendered.document().text().contains("2 items"));
  }
}
