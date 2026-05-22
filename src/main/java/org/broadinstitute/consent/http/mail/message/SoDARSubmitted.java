package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;

public class SoDARSubmitted extends MailMessage {

  private static final String SUBJECT =
      "Broad Data Use Oversight System - Signing Official - New Data Access Request Submitted From Your Institution";
  private final String darCode;
  private final User researcher;
  private final String referenceId;
  private final List<Dataset> datasets;

  public SoDARSubmitted(
      User toUser, String darCode, User researcher, String referenceId, List<Dataset> datasets) {
    super(toUser, EmailType.SO_DAR_SUBMITTED);
    this.darCode = darCode;
    this.researcher = researcher;
    this.referenceId = referenceId;
    this.datasets = datasets;
  }

  @Override
  public String createSubject() {
    return SUBJECT;
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of(
        "userName",
        toUser.getDisplayName(),
        "darCode",
        darCode,
        "researcherUserName",
        researcher.getDisplayName(),
        "datasets",
        datasets);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
