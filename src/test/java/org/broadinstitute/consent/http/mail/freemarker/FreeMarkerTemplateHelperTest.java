package org.broadinstitute.consent.http.mail.freemarker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.StringWriter;
import java.util.Map;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreeMarkerTemplateHelperTest {

  private FreeMarkerTemplateHelper freeMarkerTemplateHelper;
  private Configuration mockedConfiguration;

  @BeforeEach
  void setUp() {
    try (var mockedFreeMarker = mockConstruction(Configuration.class)) {
      // Provide a config that matches production usage so the helper will
      // prefix template requests with the configured directory.
      FreeMarkerConfiguration fmConfig = new FreeMarkerConfiguration();
      fmConfig.setTemplateDirectory("freemarker");
      fmConfig.setDefaultEncoding("UTF-8");

      freeMarkerTemplateHelper = new FreeMarkerTemplateHelper(fmConfig);
      mockedConfiguration = mockedFreeMarker.constructed().getFirst();
    }
  }

  @Test
  void getTemplate() throws Exception {
    var templateName = "template name";
    Template template = mock();
    var expectedPath = "freemarker/%s".formatted(templateName);
    when(mockedConfiguration.getTemplate(expectedPath)).thenReturn(template);
    assertEquals(template, freeMarkerTemplateHelper.getTemplate(templateName));
  }

  @Test
  void renderedTemplatePreservesRawAsmUnsubscribeToken() throws Exception {
    String renderedTemplate =
        renderTemplate(
            Map.of(
                "researcherName", "Synthetic User",
                "darCode", "DAR-123",
                "sendGridUnsubscribeGroupId", 12345));

    assertTrue(renderedTemplate.contains("href=\"<%asm_group_unsubscribe_raw_url%>\""));
  }

  @Test
  void renderedTemplateFallsBackWhenAsmUnsubscribeGroupMissing() throws Exception {
    String renderedTemplate =
        renderTemplate(Map.of("researcherName", "Synthetic User", "darCode", "DAR-123"));

    assertTrue(
        renderedTemplate.contains("To manage DUOS email notifications, please sign in to DUOS."));
    assertFalse(renderedTemplate.contains("<%asm_group_unsubscribe_raw_url%>"));
  }

  private String renderTemplate(Map<String, Object> model) throws Exception {
    FreeMarkerConfiguration fmConfig = new FreeMarkerConfiguration();
    fmConfig.setTemplateDirectory("freemarker");
    fmConfig.setDefaultEncoding("UTF-8");

    FreeMarkerTemplateHelper realTemplateHelper = new FreeMarkerTemplateHelper(fmConfig);
    Template template = realTemplateHelper.getTemplate("dar-expired.ftl");
    StringWriter out = new StringWriter();

    template.process(model, out);

    return out.toString();
  }
}
