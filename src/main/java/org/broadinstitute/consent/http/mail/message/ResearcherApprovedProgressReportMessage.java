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

  public ResearcherApprovedProgressReportMessage(User toUser, String darCode, List<DatasetMailDTO> datasets,
      String dataUseRestriction) {
    super(toUser, EmailType.RESEARCHER_PROGRESS_REPORT_APPROVED);
    this.darCode = darCode;
    this.datasets = datasets;
    this.dataUseRestriction = dataUseRestriction;
  }

  @Override
  public String createSubject() {
    return APPROVED_PROGRESS_REPORT;
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
