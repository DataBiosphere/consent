package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;

public class SoPRApproved extends MailMessage {

  private static final String SUBJECT =
      "Broad Data Use Oversight System - Signing Official - Your Institutional Researcher's Progress Report %s is %sApproved";
  private final String darCode;
  private final User researcher;
  private final String referenceId;
  private final List<Dataset> datasets;
  private final String dataUseRestriction;
  private final String radarText;

  public SoPRApproved(
      User toUser,
      String darCode,
      User researcher,
      String referenceId,
      List<Dataset> datasets,
      String dataUseRestriction,
      boolean radarApproved) {
    super(toUser, EmailType.SO_PROGRESS_REPORT_APPROVED);
    this.darCode = darCode;
    this.researcher = researcher;
    this.referenceId = referenceId;
    this.datasets = datasets;
    this.dataUseRestriction = dataUseRestriction;
    this.radarText = radarApproved ? "Rule Automated DAR (RADAR) " : "";
  }

  @Override
  public String createSubject() {
    return String.format(SUBJECT, darCode, radarText);
  }

  @Override
  public Object createModel(Map<String, Object> model) {
    return mergeModel(
        model,
        Map.of(
            "userName",
            toUser.getDisplayName(),
            "darCode",
            darCode,
            "radarText",
            radarText,
            "researcherUserName",
            researcher.getDisplayName(),
            "researcherEmail",
            researcher.getEmail(),
            "datasets",
            datasets,
            "dataUseRestriction",
            dataUseRestriction));
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
