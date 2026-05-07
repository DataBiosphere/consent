package org.broadinstitute.consent.http.mail.freemarker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import freemarker.template.Configuration;
import freemarker.template.Template;
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
      freeMarkerTemplateHelper = new FreeMarkerTemplateHelper(new FreeMarkerConfiguration());
      mockedConfiguration = mockedFreeMarker.constructed().getFirst();
    }
  }

  @Test
  void getTemplate() throws Exception {
    var templateName = "template name";
    Template template = mock();
    when(mockedConfiguration.getTemplate(templateName)).thenReturn(template);
    assertEquals(template, freeMarkerTemplateHelper.getTemplate(templateName));
  }
}
