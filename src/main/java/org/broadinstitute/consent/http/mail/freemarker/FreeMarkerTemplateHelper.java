package org.broadinstitute.consent.http.mail.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;

public class FreeMarkerTemplateHelper {

  private final Configuration freeMarkerConfig;

  public FreeMarkerTemplateHelper(FreeMarkerConfiguration config) {
    freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
    freeMarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    freeMarkerConfig.setClassForTemplateLoading(this.getClass(), config.getTemplateDirectory());
    freeMarkerConfig.setDefaultEncoding(config.getDefaultEncoding());
  }

  public Template getTemplate(String templateName) throws IOException {
    return freeMarkerConfig.getTemplate(templateName);
  }
}
