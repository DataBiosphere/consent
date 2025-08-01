package org.broadinstitute.consent.http.models.passport;

public enum VisaClaimTypes {
  AFFILIATION_AND_ROLE ("AffiliationAndRole"),
  CONTROLLED_ACCESS_GRANTS ("ControlledAccessGrants"),
  RESEARCHER_STATUS ("ResearcherStatus"),
  ;

  public final String type;

  VisaClaimTypes(String type) {
    this.type = type;
  }
}
