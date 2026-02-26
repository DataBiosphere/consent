package org.broadinstitute.consent.http.service.passport;

public class AcceptedTermsAndPolicies implements VisaClaimType {

  @Override
  public String type() {
    return VisaClaimTypes.ACCEPTED_TERMS_AND_POLICIES.type;
  }

  @Override
  public Long asserted() {
    return 0L;
  }

  @Override
  public String value() {
    return "";
  }

  @Override
  public String source() {
    return PassportService.ISS;
  }

  @Override
  public String by() {
    return VisaBy.SELF.name().toLowerCase();
  }
}
