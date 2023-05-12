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
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

public class FreeMarkerTemplateHelper {


  Configuration freeMarkerConfig;

  public FreeMarkerTemplateHelper(FreeMarkerConfiguration config) {
    freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_22);
    freeMarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    freeMarkerConfig.setClassForTemplateLoading(this.getClass(), config.getTemplateDirectory());
    freeMarkerConfig.setDefaultEncoding(config.getDefaultEncoding());
  }

  public Writer getDisabledDatasetsTemplate(String user, List<String> datasets, String entityId,
      String serverUrl) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("disabled-datasets.html");
    return generateDisabledDatasetsTemplate(user, datasets, entityId, serverUrl, temp);
  }

  public Writer getNewCaseTemplate(String userName, String election, String entityId,
      String serverUrl) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("new-case.html");
    return generateNewCaseTemplate(userName, election, entityId, temp, serverUrl);
  }

  public Writer getReminderTemplate(String user, String election, String entityId, String serverUrl)
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("reminder.html");
    return generateTemplate(user, election, entityId, temp, serverUrl);
  }

  public Writer getNewDARRequestTemplate(String serverUrl, String userName, String entityId)
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("new-request.html");
    return generateNewDARRequestTemplate(temp, serverUrl, userName, entityId);
  }

  public Writer getClosedDatasetElectionsTemplate(Map<String, List<Election>> elections,
      String darCode, String type, String serverUrl) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("closed-dataset-elections.html");
    return generateClosedDatasetElectionsTemplate(elections, darCode, serverUrl, temp);
  }

  public Writer getResearcherDarApprovedTemplate(String darCode, String researcherName,
      List<DatasetMailDTO> datasets, String dataUseRestriction, String email)
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("researcher-dar-approved.html");
    return generateResearcherApprovedTemplate(datasets, dataUseRestriction, darCode, researcherName,
        email, temp);
  }

  public Writer getDatasetApprovedTemplate(String dataSubmitterName, String datasetName,
      String dacName) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("dataset-approved.html");
    return generateDatasetApprovedTemplate(dataSubmitterName, datasetName, dacName, temp);
  }

  public Writer getDatasetDeniedTemplate(String dataSubmitterName, String datasetName,
      String dacName) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("dataset-denied.html");
    return generateDatasetDeniedTemplate(dataSubmitterName, datasetName, dacName, temp);
  }

  public Writer getNewResearcherLibraryRequestTemplate(String researcherName, String serverUrl)
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("new-researcher-library-request.html");
    return generateNewResearcherLibraryRequestTemplate(researcherName, serverUrl, temp);
  }

  public Writer getDataCustodianApprovalTemplate(List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String darCode, String researcherEmail) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("data-custodian-approval.html");
    return generateDataCustodianApprovalTemplate(datasets, dataDepositorName, darCode,
        researcherEmail, temp);
  }

  private Writer generateDatasetApprovedTemplate(String dataSubmitterName, String datasetName,
      String dacName, Template temp) throws IOException, TemplateException {
    DatasetApprovedModel model = new DatasetApprovedModel(dataSubmitterName, datasetName, dacName);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateDatasetDeniedTemplate(String dataSubmitterName, String datasetName,
      String dacName, Template temp) throws IOException, TemplateException {
    DatasetDeniedModel model = new DatasetDeniedModel(dataSubmitterName, datasetName, dacName);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateNewResearcherLibraryRequestTemplate(String researcherName,
      String serverUrl, Template temp) throws IOException, TemplateException {
    NewResearcherLibraryRequestModel model = new NewResearcherLibraryRequestModel(researcherName,
        serverUrl);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateResearcherApprovedTemplate(List<DatasetMailDTO> datasets,
      String dataUseRestriction, String darCode, String researcherName, String email, Template temp)
      throws IOException, TemplateException {
    ResearcherDarApprovedModel model = new ResearcherDarApprovedModel()
        .setResearcherName(researcherName)
        .setDarCode(darCode)
        .setDatasets(datasets)
        .setDataUseRestriction(dataUseRestriction)
        .setResearcherEmail(email);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateDataCustodianApprovalTemplate(List<DatasetMailDTO> datasets,
      String dataDepositorName, String darCode,
      String researcherEmail, Template temp) throws IOException, TemplateException {
    DataCustodianApprovalModel model = new DataCustodianApprovalModel(datasets,
        dataDepositorName, darCode, researcherEmail);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateClosedDatasetElectionsTemplate(Map<String, List<Election>> elections,
      String darCode, String serverUrl, Template temp) throws IOException, TemplateException {
    List<ClosedDatasetElectionModel> closedElections = new ArrayList<>();
    List<String> dars = new ArrayList<>(elections.keySet());
    for (String key : dars) {
      String numberOfDatasets = String.valueOf((elections.get(key)).size());
      closedElections.add(new ClosedDatasetElectionModel(key, numberOfDatasets,
          consolidateDatasetElectionResult(elections.get(key))));
    }
    ClosedDatasetElectionsModel model = new ClosedDatasetElectionsModel(serverUrl, closedElections);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private String consolidateDatasetElectionResult(List<Election> elections) {
    for (Election e : elections) {
      if (!e.getFinalAccessVote()) {
        return "Denied";
      }
    }
    return "Approved";
  }

  private Writer generateTemplate(String user, String election, String entityId, Template temp,
      String serverUrl) throws IOException, TemplateException {
    TemplateModel model = new TemplateModel(user, election, entityId, serverUrl);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateNewCaseTemplate(String userName, String election, String entityId,
      Template temp, String serverUrl) throws IOException, TemplateException {
    NewCaseTemplate model = new NewCaseTemplate(userName, election, entityId, serverUrl);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateNewDARRequestTemplate(Template temp, String serverUrl, String userName,
      String entityId) throws IOException, TemplateException {
    NewDarRequestModel model = new NewDarRequestModel(serverUrl, userName, entityId);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateDisabledDatasetsTemplate(String user, List<String> datasets,
      String entityId, String serverUrl, Template temp) throws IOException, TemplateException {
    DisabledDatasetModel model = new DisabledDatasetModel(user, datasets, entityId, serverUrl);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

}
