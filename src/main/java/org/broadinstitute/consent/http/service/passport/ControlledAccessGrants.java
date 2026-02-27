package org.broadinstitute.consent.http.service.passport;

import java.util.Calendar;
import org.broadinstitute.consent.http.models.ApprovedDataset;

/**
 * <a
 * href="https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#controlledaccessgrants">ControlledAccessGrants</a>
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
      var calendar = Calendar.getInstance();
      calendar.setTime(approvedDataset.getExpirationDate());
      calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
      return calendar.getTimeInMillis() / 1000L;
    }
    return null;
  }

  @Override
  public String value() {
    return String.format(
        "%s/dataset/%s", PassportService.ISS, approvedDataset.getDatasetIdentifier());
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
