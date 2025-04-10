package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;

/**
 * This class is responsible for generating the email message to be sent to the researcher
 * when a DAC automation rule has been triggered and the datasets have been approved.
 */
public class DACAutomationApprovalResearcherMessage {

  private final FreeMarkerTemplateHelper templateHelper;
  private final DarCollection darCollection;
  private final String dacName;
  private final User researcher;
  private final String serverUrl;
  private final List<Dataset> datasets;
  private final String automationRuleName;
  private final String fromAddress;
  private final Writer writer;

  public DACAutomationApprovalResearcherMessage(
      FreeMarkerTemplateHelper templateHelper,
      DarCollection darCollection,
      String dacName,
      User researcher,
      String serverUrl,
      List<Dataset> datasets,
      String automationRuleName,
      String fromAddress) {
    this.templateHelper = templateHelper;
    this.darCollection = darCollection;
    this.dacName = dacName;
    this.researcher = researcher;
    this.serverUrl = serverUrl;
    this.datasets = datasets;
    this.automationRuleName = automationRuleName;
    this.fromAddress = fromAddress;
    this.writer = new StringWriter();
  }

  // EmailService needs this to record the message contents
  public Writer getWriter() {
    return writer;
  }

  // Template processing requires a generic object with fields that map to the content
  // in the html template.
  private record Model(
      DarCollection darCollection,
      User researcher,
      List<Dataset> datasets,
      String dacName,
      String automationRuleName,
      String serverUrl) {
    // TODO: We need the data use restrictions as well
  }

  // Summary of the email message required by the SendGrid API
  public Mail generateEmailMessage() throws Exception {
    String subject = "DAC Automation Dataset Approval for Data Access Request %s".formatted(
        darCollection.getDarCode());
    Model model = new Model(darCollection, researcher, datasets, dacName, automationRuleName, serverUrl);
    Template template = templateHelper.getFreeMarkerConfig()
        .getTemplate("dac-automation-approval-researcher.html");
    template.process(model, writer);
    Content content = new Content("text/html", template.toString());
    return new Mail(new Email(fromAddress), subject, new Email(researcher.getEmail()), content);
  }

}
