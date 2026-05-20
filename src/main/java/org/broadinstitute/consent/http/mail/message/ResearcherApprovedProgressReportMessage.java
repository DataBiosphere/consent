package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

public class ResearcherApprovedProgressReportMessage extends MailMessage {

  private static final String APPROVED_PROGRESS_REPORT = "Your DUOS Progress Report Results";

  private final String darCode;
  private final List<DatasetMailDTO> datasets;
  private final String dataUseRestriction;
  private final String radarText;

  public ResearcherApprovedProgressReportMessage(
      User toUser,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataUseRestriction,
      boolean radarApproved) {
    super(toUser, EmailType.RESEARCHER_PROGRESS_REPORT_APPROVED);
    this.darCode = darCode;
    this.datasets = datasets;
    this.dataUseRestriction = dataUseRestriction;
    this.radarText = radarApproved ? "Rule Automated DAR (RADAR) " : "";
  }

  @Override
  public String createSubject() {
    return APPROVED_PROGRESS_REPORT;
  }

  @Override
  public Object createModel(Map<String, Object> model) {
    return mergeModel(
        model,
        Map.of(
            "researcherName",
            toUser.getDisplayName(),
            "darCode",
            darCode,
            "datasets",
            datasets,
            "dataUseRestriction",
            dataUseRestriction,
            "radarText",
            radarText,
            "researcherEmail",
            toUser.getEmail()));
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
