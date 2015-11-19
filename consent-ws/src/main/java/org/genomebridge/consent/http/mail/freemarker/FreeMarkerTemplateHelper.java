package org.genomebridge.consent.http.mail.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.genomebridge.consent.http.configurations.FreeMarkerConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class FreeMarkerTemplateHelper {

    Configuration freeMarkerConfig;

    public FreeMarkerTemplateHelper(FreeMarkerConfiguration config) throws IOException {
        freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_22);
        freeMarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freeMarkerConfig.setDirectoryForTemplateLoading(new File(config.getTemplateDirectory()));
        freeMarkerConfig.setDefaultEncoding(config.getDefaultEncoding());
    }

    private Writer generateTemplate(String user, String election, String entityId, Template temp, String serverUrl) throws IOException, TemplateException {
        TemplateModel model = new TemplateModel(user, election, entityId, serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    private Writer generateNewCaseTemplate(String election, String entityId, Template temp, String serverUrl) throws IOException, TemplateException {
        NewCaseTemplate model = new NewCaseTemplate(election, entityId, serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    private Writer generateNewDARRequestTemplate(String serverUrl, Template temp) throws IOException, TemplateException {
        NewDarRequestModel model = new NewDarRequestModel(serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    public Writer getCollectTemplate(String user, String election, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("collect.html");
        return generateTemplate(user, election, entityId, temp, serverUrl);
    }

    public Writer getNewCaseTemplate(String election, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("new-case.html");
        return generateNewCaseTemplate(election, entityId, temp, serverUrl);
    }

    public Writer getReminderTemplate(String user, String election, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("reminder.html");
        return generateTemplate(user, election, entityId, temp, serverUrl);
    }

    public Writer getNewDARRequestTemplate(String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("new-request.html");
        return generateNewDARRequestTemplate(serverUrl, temp);
    }
}
