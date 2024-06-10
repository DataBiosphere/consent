package org.broadinstitute.consent.http.mail.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
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

  public Writer getNewDARRequestTemplate(
      String serverUrl,
      String userName,
      Map<String, List<String>> dacDatasetGroups,
      String researcherUserName,
      String darID
  )
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("new-request.html");
    return generateNewDARRequestTemplate(
        temp,
        serverUrl,
        userName,
        dacDatasetGroups,
        researcherUserName,
        darID
    );
  }

  public Writer getResearcherDarApprovedTemplate(String darCode, String researcherName,
      List<DatasetMailDTO> datasets, String dataUseRestriction, String email)
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("researcher-dar-approved.html");
    return generateResearcherApprovedTemplate(datasets, dataUseRestriction, darCode, researcherName,
        email, temp);
  }

  public Writer getDatasetSubmittedTemplate(String dacChairName, String dataSubmitterName,
      String datasetName,
      String dacName) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("dataset-submitted.html");
    return generateDatasetSubmittedTemplate(dacChairName, dataSubmitterName, datasetName, dacName,
        temp);
  }

  public Writer getDatasetApprovedTemplate(String dataSubmitterName, String datasetName,
      String dacName) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("dataset-approved.html");
    return generateDatasetApprovedTemplate(dataSubmitterName, datasetName, dacName, temp);
  }

  public Writer getDatasetDeniedTemplate(String dataSubmitterName, String datasetName,
      String dacName, String dacEmail) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("dataset-denied.html");
    return generateDatasetDeniedTemplate(dataSubmitterName, datasetName, dacName, dacEmail, temp);
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

  public Writer getDaaRequestTemplate(String signingOfficialUserName,
      String userName, String daaName, String serverUrl) throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("new-daa-request.html");
    return generateNewDAARequestTemplate(signingOfficialUserName, userName, daaName,
        serverUrl, temp);
  }

  public Writer getNewDaaUploadSOTemplate(String signingOfficialUserName,
      String dacName, String newDaaName, String previousDaaName, String serverUrl)
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("new-daa-upload-signing-official.html");
    return generateNewDAAUploadSOTemplate(signingOfficialUserName, dacName, newDaaName, previousDaaName, serverUrl, temp);
  }

  public Writer getNewDaaUploadResearcherTemplate(String researcherUserName,
      String dacName, String newDaaName, String previousDaaName, String serverUrl)
      throws IOException, TemplateException {
    Template temp = freeMarkerConfig.getTemplate("new-daa-upload-researcher.html");
    return generateNewDAAUploadResearcherTemplate(researcherUserName, dacName, newDaaName, previousDaaName, serverUrl, temp);
  }

  private Writer generateDatasetApprovedTemplate(String dataSubmitterName, String datasetName,
      String dacName, Template temp) throws IOException, TemplateException {
    DatasetApprovedModel model = new DatasetApprovedModel(dataSubmitterName, datasetName, dacName);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateDatasetDeniedTemplate(String dataSubmitterName, String datasetName,
      String dacName, String dacEmail, Template temp) throws IOException, TemplateException {
    DatasetDeniedModel model = new DatasetDeniedModel(dataSubmitterName, datasetName, dacName, dacEmail);
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

  private Writer generateDatasetSubmittedTemplate(String dacChairName, String dataSubmitterName,
      String datasetName,
      String dacName, Template temp) throws IOException, TemplateException {
    DatasetSubmittedModel model = new DatasetSubmittedModel(dacChairName, dataSubmitterName,
        datasetName,
        dacName);
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

  private Writer generateNewDARRequestTemplate(
      Template temp,
      String serverUrl,
      String userName,
      Map<String, List<String>> dacDatasetGroups,
      String researcherUserName,
      String darID
  ) throws IOException, TemplateException {
    NewDarRequestModel model = new NewDarRequestModel(
        serverUrl,
        userName,
        dacDatasetGroups,
        researcherUserName,
        darID);
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

  private Writer generateNewDAARequestTemplate(
      String signingOfficialUserName,
      String userName,
      String daaName,
      String serverUrl,
      Template temp
  ) throws IOException, TemplateException {
    NewDaaRequestModel model = new NewDaaRequestModel(serverUrl, daaName, userName,
        signingOfficialUserName);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateNewDAAUploadSOTemplate(
      String signingOfficialUserName,
      String dacName,
      String previousDaaName,
      String newDaaName,
      String serverUrl,
      Template temp
  ) throws IOException, TemplateException {
    NewDAAUploadSOModel model = new NewDAAUploadSOModel(serverUrl, dacName, signingOfficialUserName,
        previousDaaName, newDaaName);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

  private Writer generateNewDAAUploadResearcherTemplate(
      String researcherUserName,
      String dacName,
      String previousDaaName,
      String newDaaName,
      String serverUrl,
      Template temp
  ) throws IOException, TemplateException {
    NewDAAUploadResearcherModel model = new NewDAAUploadResearcherModel(serverUrl, dacName,
        researcherUserName, previousDaaName, newDaaName);
    Writer out = new StringWriter();
    temp.process(model, out);
    return out;
  }

}
