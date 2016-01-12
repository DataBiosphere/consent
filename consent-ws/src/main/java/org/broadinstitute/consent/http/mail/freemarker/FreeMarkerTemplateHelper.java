package org.broadinstitute.consent.http.mail.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class FreeMarkerTemplateHelper {

    Configuration freeMarkerConfig;

    public FreeMarkerTemplateHelper(FreeMarkerConfiguration config) throws IOException {
        freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_22);
        freeMarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freeMarkerConfig.setClassForTemplateLoading(this.getClass(), config.getTemplateDirectory());
        freeMarkerConfig.setDefaultEncoding(config.getDefaultEncoding());
    }


    public Writer getDisabledDatasetsTemplate(String user, List<String> datasets, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("disabled-datasets.html");
        return generateDisabledDatasetsTemplate(user, datasets, entityId, serverUrl, temp);
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

    public Writer getCancelledDarTemplate(String userType, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("cancelled-dar-request.html");
        return generateCancelledDarTemplate(userType, entityId, serverUrl, temp);
    }

    public Writer getAdminApprovedDarTemplate(String userName, String entityId, Map<DACUser, List<DataSet>> dataOwnersDataSets, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("admin-dar-approved.html");
        return generateAdminApprovedDarTemplate(userName, entityId, dataOwnersDataSets, serverUrl, temp);
    }

    public Writer getApprovedDarTemplate(String userName, String entityId, List<DataSet> datasets, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("owner-dar-approved.html");
        return generateApprovedDarTemplate(userName, entityId, datasets, serverUrl, temp);
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

    private Writer generateDisabledDatasetsTemplate(String user, List<String> datasets, String entityId, String serverUrl, Template temp) throws IOException, TemplateException {
        DisabledDatasetModel model = new DisabledDatasetModel(user, datasets, entityId, serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    private Writer generateCancelledDarTemplate(String userType, String entityId, String serverUrl, Template temp) throws IOException, TemplateException {
        CancelledDarModel model = new CancelledDarModel(userType, entityId, serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    private Writer generateAdminApprovedDarTemplate(String userType, String entityId, Map<DACUser, List<DataSet>> dataOwnersDataSets, String serverUrl, Template temp) throws IOException, TemplateException {
        AdminDarApprovedModel model = new AdminDarApprovedModel(userType, entityId, dataOwnersDataSets, serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    private Writer generateApprovedDarTemplate(String userName, String entityId, List<DataSet> dataSetList, String serverUrl, Template temp) throws IOException, TemplateException {
        ApprovedDarModel model = new ApprovedDarModel(userName, entityId, dataSetList, serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }
}
