package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

/**
 * This class is responsible for generating the email message to be sent to the researcher
 * when a DAC automation rule has been triggered and the datasets have been approved.
 */
public class DACAutomationApprovalResearcherMessage extends MailMessage {

  private static final String APPROVED_DAR = "Your DUOS Data Access Request Results";
  private final String darCode;
  private final List<DatasetMailDTO> datasets;
  private final String dataUseRestriction;

  public DACAutomationApprovalResearcherMessage(
      User toUser, String darCode, List<DatasetMailDTO> datasets,
      String dataUseRestriction) {
    super(toUser, EmailType.DAC_AUTOMATION_APPROVAL);
    this.darCode = darCode;
    this.datasets = datasets;
    this.dataUseRestriction = dataUseRestriction;
  }

  @Override
  public String createSubject() {
    return APPROVED_DAR;
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of("researcherName", toUser.getDisplayName(),
        "darCode", darCode,
        "datasets", datasets,
        "dataUseRestriction", dataUseRestriction,
        "researcherEmail", toUser.getEmail());
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
