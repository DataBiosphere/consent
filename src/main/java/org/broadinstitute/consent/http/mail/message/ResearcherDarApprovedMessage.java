package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

public class ResearcherDarApprovedMessage extends MailMessage {

  private static final String APPROVED_DAR = "Your DUOS Data Access Request Results";

  private final String darCode;
  private final List<DatasetMailDTO> datasets;
  private final String dataUseRestriction;

  public ResearcherDarApprovedMessage(User toUser, String darCode, List<DatasetMailDTO> datasets,
      String dataUseRestriction) {
    super(toUser, EmailType.RESEARCHER_DAR_APPROVED);
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
