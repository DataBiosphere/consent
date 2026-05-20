package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

public class DACMembersDARRADARApprovedMessage extends MailMessage {

  private static final String SUBJECT =
      "Broad Data Use Oversight System - Data Access Committee - Data Access Request %s is Rule Automated DAR (RADAR) Approved";
  private final String darCode;
  private final User researcher;
  private final String referenceId;
  private final List<DatasetMailDTO> datasets;

  public DACMembersDARRADARApprovedMessage(
      User toUser,
      String darCode,
      User researcher,
      String referenceId,
      List<DatasetMailDTO> datasets) {
    super(toUser, EmailType.DAC_RADAR_APPROVED);
    this.darCode = darCode;
    this.researcher = researcher;
    this.referenceId = referenceId;
    this.datasets = datasets;
  }

  @Override
  public String createSubject() {
    return String.format(SUBJECT, darCode);
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
            "researcherUserName",
            researcher.getDisplayName(),
            "datasets",
            datasets));
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
