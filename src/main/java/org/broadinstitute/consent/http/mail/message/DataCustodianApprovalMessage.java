package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

public class DataCustodianApprovalMessage extends MailMessage {

  private final String darCode;
  private final List<DatasetMailDTO> datasets;
  private final String dataDepositorName;
  private final String researcherEmail;
  private final String radarText;

  public DataCustodianApprovalMessage(
      User toUser,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String researcherEmail,
      boolean radarApproved) {
    super(toUser, EmailType.DATA_CUSTODIAN_APPROVAL);
    this.darCode = darCode;
    this.datasets = datasets;
    this.dataDepositorName = dataDepositorName;
    this.researcherEmail = researcherEmail;
    this.radarText = radarApproved ? "Rule Automated DAR (RADAR) " : "";
  }

  @Override
  public String createSubject() {
    return String.format("%s has been %sapproved by the DAC", darCode, radarText);
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of(
        "datasets",
        datasets,
        "dataDepositorName",
        dataDepositorName,
        "darCode",
        darCode,
        "researcherEmail",
        researcherEmail,
        "radarText",
        radarText);
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
