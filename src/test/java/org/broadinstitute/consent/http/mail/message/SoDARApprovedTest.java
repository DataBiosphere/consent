package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class SoDARApprovedTest extends AbstractTestHelper {

  private FreeMarkerTemplateHelper helper;

  private User toUser;

  private User researcher;

  private List<Dataset> datasets;

  private SoDARApproved message;

  private final String darCode = "DAR-123";

  private final String referenceId = UUID.randomUUID().toString();

  private final String dataUseRestriction = new DataUseBuilder().setGeneralUse(true).build().toString();

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

  @Test
  void testMessageSubject() {
    message = new SoDARApproved(toUser, darCode, researcher, referenceId, datasets, dataUseRestriction);
    assertEquals("Your Institutional Researcher's Data Access Request %s is Approved".formatted(darCode), message.createSubject());
  }

  @Test
  void testGetSoDarApprovedTemplate() throws Exception {
    String linkUrl = "http://testServerUrl";
    message = new SoDARApproved(toUser, darCode, researcher, referenceId, datasets, dataUseRestriction);

    assertEquals(referenceId, message.getEntityReferenceId());

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel(linkUrl), out);
    String templateString = out.toString();

    assertTrue(templateString.contains(toUser.getDisplayName()));
    assertTrue(templateString.contains(darCode));
    assertTrue(templateString.contains(researcher.getDisplayName()));
    datasets.forEach(dataset -> {
      assertTrue(templateString.contains(dataset.getName()));
      assertTrue(templateString.contains(dataset.getDatasetIdentifier()));
    });
  }
}
