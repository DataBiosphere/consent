package org.broadinstitute.consent.http.models.passport;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.broadinstitute.consent.http.models.ApprovedDataset;

/**
 * <a href="https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#controlledaccessgrants">ControlledAccessGrants</a>
 */
public class ControlledAccessGrants implements VisaClaimType {

  private final ApprovedDataset approvedDataset;

  public ControlledAccessGrants(ApprovedDataset approvedDataset) {
    this.approvedDataset = approvedDataset;
  }

  @Override
  public String type() {
    return VisaClaimTypes.CONTROLLED_ACCESS_GRANTS.type;
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
    return VisaBy.DAC.name().toLowerCase();
  }
}
