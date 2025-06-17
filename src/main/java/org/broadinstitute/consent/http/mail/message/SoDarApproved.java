package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;

public class SoDarApproved extends MailMessage {

  private static final String SUBJECT = "Your Institutional Researcher's Data Access Request %s is Approved";
  private final String darCode;
  private final User researcher;
  private final String referenceId;
  private final List<Dataset> datasets;
  private final String dataUseRestriction;

  public SoDarApproved(User toUser, String darCode, User researcher, String referenceId, List<Dataset> datasets, String dataUseRestriction) {
    super(toUser, EmailType.SO_DAR_APPROVED);
    this.darCode = darCode;
    this.researcher = researcher;
    this.referenceId = referenceId;
    this.datasets = datasets;
    this.dataUseRestriction = dataUseRestriction;
  }

  @Override
  public String createSubject() {
    return String.format(SUBJECT, darCode);
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "userName", toUser.getDisplayName(),
        "darCode", darCode,
        "researcherUserName", researcher.getDisplayName(),
        "researcherEmail", researcher.getEmail(),
        "datasets", datasets,
        "dataUseRestriction", dataUseRestriction,
        "serverUrl", serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
