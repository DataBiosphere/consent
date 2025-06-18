package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SigningOfficialMessagesTest extends AbstractTestHelper {

  private FreeMarkerTemplateHelper helper;

  private static User toUser;

  private static User researcher;

  private static List<Dataset> datasets;

  private static final String DAR_CODE = "DAR-123";

  private static final String REFERENCE_ID = UUID.randomUUID().toString();

  private static final String TRANSLATION = new DataUseBuilder().setGeneralUse(true).build()
      .toString();

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
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

  private static Stream<Arguments> provideSOMessages() {
    return Stream.of(
        Arguments.of(new SoDARApproved(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets,
            TRANSLATION)),
        Arguments.of(new SoPRApproved(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets,
            TRANSLATION)),
        Arguments.of(new SoDARSubmitted(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets)),
        Arguments.of(new SoPRSubmitted(toUser, DAR_CODE, researcher, REFERENCE_ID, datasets))
    );
  }

  @ParameterizedTest
  @MethodSource("provideSOMessages")
  void testMessageSubject(MailMessage message) {
    assertTrue(
        message.createSubject().contains("Broad Data Use Oversight System - Signing Official"));
  }

  @ParameterizedTest
  @MethodSource("provideSOMessages")
  void testMessageTemplate(MailMessage message) throws Exception {
    var linkUrl = "http://testServerUrl";
    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel(linkUrl), out);
    String templateString = out.toString();

    assertTrue(templateString.contains(toUser.getDisplayName()));
    assertTrue(templateString.contains(DAR_CODE));
    assertTrue(templateString.contains(researcher.getDisplayName()));
    datasets.forEach(dataset -> {
      assertTrue(templateString.contains(dataset.getName()));
      assertTrue(templateString.contains(dataset.getDatasetIdentifier()));
    });
  }

  @ParameterizedTest
  @MethodSource("provideSOMessages")
  void testMessageEntityReferenceId(MailMessage message) {
    assertEquals(REFERENCE_ID, message.getEntityReferenceId());
  }
}
