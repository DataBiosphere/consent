package org.broadinstitute.consent.http.models.passport;

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
  public Integer asserted() {
    if (approvedDataset.getApprovalDate() != null) {
      return Long.valueOf(approvedDataset.getApprovalDate().getTime()).intValue();
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
