package org.broadinstitute.consent.http.service.passport;

public enum VisaClaimTypes {
  AFFILIATION_AND_ROLE("AffiliationAndRole"),
  CONTROLLED_ACCESS_GRANTS("ControlledAccessGrants"),
  RESEARCHER_STATUS("ResearcherStatus"),
  ACCEPTED_TERMS_AND_POLICIES("AcceptedTermsAndPolicies"),
  ;

  public final String type;

  VisaClaimTypes(String type) {
    this.type = type;
  }
}
