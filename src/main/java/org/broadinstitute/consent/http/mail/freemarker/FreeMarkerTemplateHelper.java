package org.broadinstitute.consent.http.mail.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;

public class FreeMarkerTemplateHelper {

  private final Configuration freeMarkerConfig;
  private final String templateDirectory;

  public FreeMarkerTemplateHelper(FreeMarkerConfiguration config) {
    freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_34);
    // Recognize standard file extensions if templates use them in the future
    freeMarkerConfig.setRecognizeStandardFileExtensions(true);
    freeMarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    // Load templates from the classpath root; we'll resolve the configured templateDirectory
    // when requesting templates to allow include directives to resolve relative paths.
    freeMarkerConfig.setClassForTemplateLoading(this.getClass(), "/");
    freeMarkerConfig.setDefaultEncoding(config.getDefaultEncoding());
    this.templateDirectory = config.getTemplateDirectory();
  }

  public Template getTemplate(String templateName) throws IOException {
    // Ensure we request the template with the configured directory as a prefix so
    // templates are loaded from e.g. "freemarker/<templateName>". Trim any leading '/'.
    String dir = templateDirectory == null ? "" : templateDirectory.replaceFirst("^/", "");
    String path = dir.isEmpty() ? templateName : "%s/%s".formatted(dir, templateName);
    return freeMarkerConfig.getTemplate(path);
  }
}
