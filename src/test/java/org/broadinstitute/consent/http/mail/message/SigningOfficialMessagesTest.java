package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SigningOfficialMessagesTest extends AbstractMailMessageTest {

  private static final String DAR_CODE = "DAR-123";
  private static final String REFERENCE_ID = UUID.randomUUID().toString();
  private static final String TRANSLATION =
      new DataUseBuilder().setGeneralUse(true).build().toString();
  private static User toUser;
  private static User researcher;
  private static List<Dataset> datasets;

  private static Stream<Arguments> messageProvider() {
    return Stream.of(
        Arguments.of(
            new SoDARApproved(
                toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, false)),
        Arguments.of(
            new SoDARApproved(
                toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, true)),
        Arguments.of(
            new SoPRApproved(
                toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, false)),
        Arguments.of(
            new SoPRApproved(
                toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, true)),
        Arguments.of(new SoDARSubmitted(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets)),
        Arguments.of(new SoPRSubmitted(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets)));
  }

  @BeforeEach
  void setUp() {
    toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setEmail("test.user@test.com");
    researcher = new User();
    researcher.setDisplayName("Researcher Name");
    researcher.setEmail("researcher@test.com");
    Dataset dataset = new Dataset(1, "Dataset 1", "Description 1", new Date());
    dataset.setAlias(1);
    dataset.setDatasetIdentifier();
    datasets = List.of(dataset);
  }

  @ParameterizedTest
  @MethodSource("messageProvider")
  void testMessageSubject(MailMessage message) {
    assertTrue(
        message.createSubject().contains("Broad Data Use Oversight System - Signing Official"));
  }

  @ParameterizedTest
  @MethodSource("messageProvider")
  void testCreateModel_AddsRequiredFields(MailMessage message) {
    String radarText =
        message.createSubject().contains("RADAR") ? "Rule Automated DAR (RADAR) " : "";
    if (message instanceof SoDARApproved || message instanceof SoPRApproved) {
      assertRequiredModelFields(
          message,
          Map.of(
              "userName",
              toUser.getDisplayName(),
              "darCode",
              DAR_CODE,
              "radarText",
              radarText,
              "researcherUserName",
              researcher.getDisplayName(),
              "researcherEmail",
              researcher.getEmail(),
              "datasets",
              datasets,
              "dataUseRestriction",
              TRANSLATION));
    } else {
      assertRequiredModelFields(
          message,
          Map.of(
              "userName",
              toUser.getDisplayName(),
              "darCode",
              DAR_CODE,
              "researcherUserName",
              researcher.getDisplayName(),
              "datasets",
              datasets));
    }
  }

  @ParameterizedTest
  @MethodSource("messageProvider")
  void testMessageTemplate(MailMessage message) throws Exception {
    var linkUrl = "http://testServerUrl";
    var rendered = renderTemplate(message, linkUrl);

    assertTrue(rendered.content().contains(toUser.getDisplayName()));
    assertTrue(rendered.content().contains(DAR_CODE));
    assertTrue(rendered.content().contains(researcher.getDisplayName()));
    datasets.forEach(
        dataset -> {
          assertTrue(rendered.content().contains(dataset.getName()));
          assertTrue(rendered.content().contains(dataset.getDatasetIdentifier()));
        });
  }

  @ParameterizedTest
  @MethodSource("messageProvider")
  void testMessageEntityReferenceId(MailMessage message) {
    assertEquals(REFERENCE_ID, message.getEntityReferenceId());
  }

  @Test
  void testDARApprovedRADARReferences() throws Exception {
    MailMessage message =
        new SoDARApproved(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, true);
    var rendered = renderTemplate(message, "http://testServerUrl");

    assertEquals(2, StringUtils.countMatches(rendered.content(), " Rule Automated DAR (RADAR) "));
    assertFalse(rendered.content().contains("radarText"));
  }

  @Test
  void testDARNORADARReferences() throws Exception {
    MailMessage message =
        new SoDARApproved(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, false);
    var rendered = renderTemplate(message, "http://testServerUrl");

    assertFalse(rendered.content().toLowerCase().contains("radar"));
  }

  @Test
  void testPRApprovedRADARReferences() throws Exception {
    MailMessage message =
        new SoPRApproved(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, true);
    var rendered = renderTemplate(message, "http://testServerUrl");

    assertEquals(2, StringUtils.countMatches(rendered.content(), " Rule Automated DAR (RADAR) "));
    assertFalse(rendered.content().contains("radarText"));

    String subject = message.createSubject();
    assertFalse(subject.contains("radarText"));
    assertTrue(subject.contains(" Rule Automated DAR (RADAR) "));
  }

  @Test
  void testPRNORADARReferences() throws Exception {
    MailMessage message =
        new SoPRApproved(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets, TRANSLATION, false);
    var rendered = renderTemplate(message, "http://testServerUrl");

    assertFalse(rendered.content().toLowerCase().contains("radar"));
  }
}
