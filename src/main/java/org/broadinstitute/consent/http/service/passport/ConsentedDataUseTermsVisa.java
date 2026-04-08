package org.broadinstitute.consent.http.service.passport;

import java.time.Instant;
import org.broadinstitute.consent.http.models.Dataset;

/**
 * Data Passport visa encoding the permitted uses of a dataset based on participant consent,
 * expressed as a link to the dataset's data use terms. Leverages the Data Use Ontology (DUO) for
 * standardization.
 *
 * @see <a href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5372874">GA4GH Data Passports
 *     specification</a>
 */
public class ConsentedDataUseTermsVisa implements VisaClaimType {

  private final Dataset dataset;

  public ConsentedDataUseTermsVisa(Dataset dataset) {
    this.dataset = dataset;
  }

  @Override
  public String type() {
    return VisaClaimTypes.CONSENTED_DATA_USE_TERMS.type;
  }

  @Override
  public Long asserted() {
    if (dataset.getCreateDate() != null) {
      // java.sql.Date#toInstant throws UnsupportedOperationException; use epoch millis instead.
      return PassportService.getEpochSeconds(
          Instant.ofEpochMilli(dataset.getCreateDate().getTime()));
    }
    return PassportService.getEpochSeconds(Instant.now());
  }

  /**
   * Returns a stable URL pointing to the dataset's data use terms in DUOS. Consumers can
   * dereference this URL to retrieve the full DUO-coded data use object for the dataset.
   */
  @Override
  public String value() {
    return "%s/dataset/%s/dataUse".formatted(PassportService.ISS, dataset.getDatasetIdentifier());
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
