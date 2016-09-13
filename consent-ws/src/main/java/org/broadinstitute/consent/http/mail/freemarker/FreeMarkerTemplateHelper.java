package org.broadinstitute.consent.http.mail.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.darsummary.SummaryItem;

public class FreeMarkerTemplateHelper {


    Configuration freeMarkerConfig;
    private final String CREATE_DAR_URL = "admin_manage_access";

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

    public Writer getNewCaseTemplate(String userName, String election, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("new-case.html");
        return generateNewCaseTemplate(userName, election, entityId, temp, serverUrl);
    }

    public Writer getReminderTemplate(String user, String election, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("reminder.html");
        return generateTemplate(user, election, entityId, temp, serverUrl);
    }

    public Writer getNewDARRequestTemplate(String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("new-request.html");
        return generateNewDARRequestTemplate(serverUrl+CREATE_DAR_URL, temp);
    }

    public Writer getCancelledDarTemplate(String userType, String entityId, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("cancelled-dar-request.html");
        return generateCancelledDarTemplate(userType, entityId, serverUrl, temp);
    }

    public Writer getAdminApprovedDarTemplate(String userName, String entityId, Map<DACUser, List<DataSet>> dataOwnersDataSets, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("admin-dar-approved.html");
        return generateAdminApprovedDarTemplate(userName, entityId, dataOwnersDataSets, serverUrl, temp);
    }

    public Writer getApprovedDarTemplate(String userName, String date, String entityId, String investigator, String institution,
                                         String researchPurpose, List<SummaryItem> typeOfResearch, String diseaseArea,
                                         List<String> checkedSentences, String translatedUseRestriction, List<DataSetPIMailModel> datasets,
                                         String daysToApprove, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("owner-dar-approved.html");
        return generateApprovedDarTemplate(userName, date, entityId, investigator, institution, researchPurpose, typeOfResearch, diseaseArea,
                checkedSentences, translatedUseRestriction, datasets, daysToApprove, serverUrl, temp);
    }

    public Writer getClosedDatasetElectionsTemplate(Map<String, List<Election>> elections, String darCode, String type, String serverUrl) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("closed-dataset-elections.html");
        return generateClosedDatasetElectionsTemplate(elections, darCode, serverUrl, temp);
    }

    private Writer generateClosedDatasetElectionsTemplate(Map<String, List<Election>> elections, String darCode, String serverUrl, Template temp) throws IOException, TemplateException {
        List<ClosedDatasetElectionModel> closedElections = new ArrayList<>();
        List<String> dars = new ArrayList<>(elections.keySet());
        for(String key: dars){
            String numberOfDatasets = String.valueOf((elections.get(key)).size());
            closedElections.add(new ClosedDatasetElectionModel(key, numberOfDatasets, consolidateDatasetElectionResult(elections.get(key))));
        }
        ClosedDatasetElectionsModel model = new ClosedDatasetElectionsModel(serverUrl, closedElections);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    private String consolidateDatasetElectionResult(List<Election> elections){
        for(Election e: elections){
            if(! e.getFinalAccessVote()){
                return "Denied";
            }
        }
        return "Approved";
    }

    private Writer generateTemplate(String user, String election, String entityId, Template temp, String serverUrl) throws IOException, TemplateException {
        TemplateModel model = new TemplateModel(user, election, entityId, serverUrl);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    private Writer generateNewCaseTemplate(String userName, String election, String entityId, Template temp, String serverUrl) throws IOException, TemplateException {
        NewCaseTemplate model = new NewCaseTemplate(userName, election, entityId, serverUrl);
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

    private Writer generateApprovedDarTemplate(String userName, String date, String entityId, String investigator, String institution,
                                               String researchPurpose, List<SummaryItem> typeOfResearch, String diseaseArea,
                                               List<String> checkedSentences, String translatedUseRestriction, List<DataSetPIMailModel> datasets,
                                               String daysToApprove, String serverUrl, Template temp) throws IOException, TemplateException {
        ApprovedDarModel model = new ApprovedDarModel(userName, date, entityId, investigator, institution, researchPurpose, typeOfResearch, diseaseArea, checkedSentences,
                translatedUseRestriction, datasets, serverUrl, daysToApprove);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

    public Writer getUserDelegateResponsibilitiesTemplate(String user, List<VoteAndElectionModel> delegatedVotes, String newRoleName, String serverUrl) throws IOException, TemplateException {
                Template temp = freeMarkerConfig.getTemplate("user-delegate-responsibilities.html");
                return generateUserDelegateResponsibilitiesTemplate(user, delegatedVotes, newRoleName, serverUrl, temp);
    }

    private Writer generateUserDelegateResponsibilitiesTemplate(String user, List<VoteAndElectionModel> delegatedVotes, String newRoleName, String serverUrl, Template temp) throws IOException, TemplateException {
                DelegateResponsibilitiesModel model = new DelegateResponsibilitiesModel(user, newRoleName, serverUrl, delegatedVotes);
                Writer out = new StringWriter();
                temp.process(model, out);
                return out;
    }

    public Writer getNewResearcherCreatedTemplate(String admin, String researcherName, String url, String action) throws IOException, TemplateException {
        Template temp = freeMarkerConfig.getTemplate("new-researcher.html");
        NewResearcherModel model = new NewResearcherModel(admin, researcherName, url, action);
        Writer out = new StringWriter();
        temp.process(model, out);
        return out;
    }

}
