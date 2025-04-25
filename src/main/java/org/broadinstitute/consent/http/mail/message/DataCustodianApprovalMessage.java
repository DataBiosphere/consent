package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;

public class DataCustodianApprovalMessage extends MailMessage {

  private final String darCode;
  private final List<DatasetMailDTO> datasets;
  private final String dataDepositorName;
  private final String researcherEmail;

  public DataCustodianApprovalMessage(
      User toUser,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String researcherEmail) {
    super(toUser, EmailType.DATA_CUSTODIAN_APPROVAL);
    this.darCode = darCode;
    this.datasets = datasets;
    this.dataDepositorName = dataDepositorName;
    this.researcherEmail = researcherEmail;
  }

  @Override
  public String createSubject() {
    return String.format("%s has been approved by the DAC", darCode);
  }

  record Model(
      List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String darCode,
      String researcherEmail) {}

  @Override
  public Object createModel(String serverUrl) {
    return new Model(datasets, dataDepositorName, darCode, researcherEmail);
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
