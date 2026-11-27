package org.broadinstitute.consent.http.service.passport;

import java.time.Instant;
import org.broadinstitute.consent.http.models.DataAccessAgreement;

/**
 * Data Passport visa listing the Data Access Agreement (DAA) that users must accept in order to
 * access the dataset. References the DAA document managed in DUOS by the DAC.
 *
 * @see <a href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5372874">GA4GH Data Passports
 *     specification</a>
 */
public class RequiredAgreementsVisa implements VisaClaimType {

  private final DataAccessAgreement daa;

  public RequiredAgreementsVisa(DataAccessAgreement daa) {
    this.daa = daa;
  }

  @Override
  public String type() {
    return VisaClaimTypes.REQUIRED_AGREEMENTS.type;
  }

  @Override
  public Long asserted() {
    if (daa.getCreateDate() != null) {
      return PassportService.getEpochSeconds(daa.getCreateDate());
    }
    return PassportService.getEpochSeconds(Instant.now());
  }

  /**
   * Returns a stable URL pointing to the DAA within DUOS. Consumers can use this to retrieve the
   * full agreement document and verify that a researcher's Library Card includes acceptance of this
   * agreement before granting access.
   */
  @Override
  public Object value() {
    return "%s/daa/%d".formatted(PassportService.ISS, daa.getDaaId());
  }

  @Override
  public String source() {
    return PassportService.ISS;
  }

  @Override
  public String by() {
    return VisaBy.SO.name().toLowerCase();
  }
}
