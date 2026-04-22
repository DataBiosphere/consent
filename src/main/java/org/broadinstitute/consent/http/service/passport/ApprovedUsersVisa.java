package org.broadinstitute.consent.http.service.passport;

public class ApprovedUsersVisa implements VisaClaimType {

  private final String datasetIdentifier;

  public ApprovedUsersVisa(String datasetIdentifier) {
    this.datasetIdentifier = datasetIdentifier;
  }

  @Override
  public String type() {
    return VisaClaimTypes.APPROVED_USERS.type;
  }

  @Override
  public Long asserted() {
    return PassportService.getEpochSeconds(java.time.Instant.now());
  }

  @Override
  public String value() {
    return PassportService.getApprovedUsersEndpoint(datasetIdentifier);
  }

  @Override
  public String source() {
    return PassportService.ISS;
  }

  @Override
  public String by() {
    return VisaBy.DAC.name().toLowerCase();
  }
}
