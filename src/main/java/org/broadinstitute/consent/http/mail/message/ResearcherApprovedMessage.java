package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

public class ResearcherApprovedMessage extends MailMessage {

  private static final String APPROVED_DAR = "Your DUOS Data Access Request Results";

  private final String darCode;
  private final List<DatasetMailDTO> datasets;
  private final String dataUseRestriction;

  public ResearcherApprovedMessage(User toUser, String darCode, List<DatasetMailDTO> datasets,
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

  record Model(
      String researcherName,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataUseRestriction,
      String researcherEmail) {}

  @Override
  public Object createModel(String serverUrl) {
    return new Model(
        toUser.getDisplayName(),
        darCode,
        datasets,
        dataUseRestriction,
        toUser.getEmail());
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
