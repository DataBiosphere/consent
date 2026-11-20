package org.broadinstitute.consent.http.service.passport;

import java.time.Instant;
import org.broadinstitute.consent.http.models.Dac;

/**
 * Data Passport visa describing the entity responsible for governing access to the dataset. Maps to
 * the DAC (Data Access Committee) that oversees the dataset in DUOS.
 *
 * @see <a href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5372874">GA4GH Data Passports
 *     specification</a>
 */
public class OversightBodiesVisa implements VisaClaimType {

  private final Dac dac;

  public OversightBodiesVisa(Dac dac) {
    this.dac = dac;
  }

  @Override
  public String type() {
    return VisaClaimTypes.OVERSIGHT_BODIES.type;
  }

  @Override
  public Long asserted() {
    if (dac.getCreateDate() != null) {
      // java.sql.Date#toInstant throws UnsupportedOperationException; use epoch millis instead.
      return PassportService.getEpochSeconds(Instant.ofEpochMilli(dac.getCreateDate().getTime()));
    }
    return PassportService.getEpochSeconds(Instant.now());
  }

  /**
   * Returns a stable URL identifying the DAC within DUOS. Consumers can dereference this URL to
   * retrieve details about the oversight body, including its members and chairpersons. TODO: We
   * need a public and stable identifier to point users to for DACs.
   */
  @Override
  public Object value() {
    return "%s/dac/%d".formatted(PassportService.ISS, dac.getDacId());
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
