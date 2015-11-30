package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class FreeMarkerConfiguration {

    @NotNull
    public String templateDirectory;

    @NotNull
    public String defaultEncoding;

    public String getTemplateDirectory() {
        return templateDirectory;
    }

    public void setTemplateDirectory(String templateDirectory) {
        this.templateDirectory = templateDirectory;
    }

    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

}
