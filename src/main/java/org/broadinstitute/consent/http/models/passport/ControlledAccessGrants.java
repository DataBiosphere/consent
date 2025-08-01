package org.broadinstitute.consent.http.models.passport;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;
import org.broadinstitute.consent.http.models.ApprovedDataset;

/**
 * https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#controlledaccessgrants
 */
public class ControlledAccessGrants implements VisaClaimType {

  private final ApprovedDataset approvedDataset;

  public ControlledAccessGrants(ApprovedDataset approvedDataset) {
    this.approvedDataset = approvedDataset;
  }

  @Override
  public String type() {
    return "ControlledAccessGrants";
  }

  @Override
  public Long asserted() {
    if (approvedDataset.getExpirationDate() != null) {
      var expiration = new Date(approvedDataset.getExpirationDate().getTime());
      var asserted = expiration.toInstant().minus(1, ChronoUnit.YEARS);
      return asserted.toEpochMilli();
    }
    return null;
  }

  @Override
  public String value() {
    return String.format("https://duos.org/dataset/%s", approvedDataset.getDatasetIdentifier());
  }

  @Override
  public String source() {
    return approvedDataset.getDacName();
  }

  @Override
  public String by() {
    return VisaBy.dac.name();
  }
}
