package org.broadinstitute.consent.http.service.passport;

public enum VisaClaimTypes {
  // GA4GH Researcher Passport visa types
  AFFILIATION_AND_ROLE("AffiliationAndRole"),
  CONTROLLED_ACCESS_GRANTS("ControlledAccessGrants"),
  RESEARCHER_STATUS("ResearcherStatus"),

  // GA4GH Data Passport visa types (see
  // https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5372874)
  APPROVED_USERS("ApprovedUsers"),
  CONSENTED_DATA_USE_TERMS("ConsentedDataUseTerms"),
  OVERSIGHT_BODIES("OversightBodies"),
  REQUIRED_AGREEMENTS("RequiredAgreements");

  public final String type;

  VisaClaimTypes(String type) {
    this.type = type;
  }
}
