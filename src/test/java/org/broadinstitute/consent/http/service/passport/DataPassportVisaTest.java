package org.broadinstitute.consent.http.service.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Data Passport visa types introduced in <a
 * href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5372874">GA4GH Data Passports</a>:
 * ConsentedDataUseTermsVisa, OversightBodiesVisa, and RequiredAgreementsVisa.
 */
class DataPassportVisaTest {

  // -----------------------------------------------------------------------
  // ConsentedDataUseTermsVisa
  // -----------------------------------------------------------------------

  @Test
  void consentedDataUseTerms_type() {
    ConsentedDataUseTermsVisa visa = new ConsentedDataUseTermsVisa(datasetWithAlias(42));
    assertEquals(VisaClaimTypes.CONSENTED_DATA_USE_TERMS.type, visa.type());
  }

  @Test
  void consentedDataUseTerms_value_isListOfDuoTerms() {
    Dataset dataset = datasetWithAlias(42);
    ConsentedDataUseTermsVisa visa = new ConsentedDataUseTermsVisa(dataset);
    assertInstanceOf(List.class, visa.value());
  }

  @Test
  void consentedDataUseTerms_value_emptyListWhenDataUseIsNull() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDataUse(null);
    List<?> terms = (List<?>) new ConsentedDataUseTermsVisa(dataset).value();
    assertNotNull(terms);
    assertTrue(terms.isEmpty());
  }

  @Test
  void consentedDataUseTerms_value_generalUse() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000004"), "GRU should map to DUO:0000004");
  }

  @Test
  void consentedDataUseTerms_value_hmbResearch() {
    DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000006"), "HMB should map to DUO:0000006");
  }

  @Test
  void consentedDataUseTerms_value_diseaseRestrictions_includesDuoClassifierAndTermIds() {
    DataUse dataUse =
        new DataUseBuilder().setDiseaseRestrictions(List.of("MONDO:0005267", "HP:0001250")).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000007"), "DS should include DUO:0000007 classifier");
    assertTrue(terms.contains("MONDO:0005267"), "disease term MONDO:0005267 should be included");
    assertTrue(terms.contains("HP:0001250"), "disease term HP:0001250 should be included");
  }

  @Test
  void consentedDataUseTerms_value_diseaseRestrictions_emptyListDoesNotAddClassifier() {
    DataUse dataUse = new DataUseBuilder().setDiseaseRestrictions(List.of()).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(
        terms.stream().noneMatch("DUO:0000007"::equals),
        "Empty disease list should not add DUO:0000007");
  }

  @Test
  void consentedDataUseTerms_value_populationOriginsAncestry() {
    DataUse dataUse = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000011"), "POA should map to DUO:0000011");
  }

  @Test
  void consentedDataUseTerms_value_geneticStudiesOnly() {
    DataUse dataUse = new DataUseBuilder().setGeneticStudiesOnly(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000016"), "GSO should map to DUO:0000016");
  }

  @Test
  void consentedDataUseTerms_value_methodsResearch() {
    DataUse dataUse = new DataUseBuilder().setMethodsResearch(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000015"), "NMDS should map to DUO:0000015");
  }

  @Test
  void consentedDataUseTerms_value_nonProfitUse() {
    DataUse dataUse = new DataUseBuilder().setNonProfitUse(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000018"), "NPU should map to DUO:0000018");
  }

  @Test
  void consentedDataUseTerms_value_publicationResults() {
    DataUse dataUse = new DataUseBuilder().setPublicationResults(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000019"), "PUB should map to DUO:0000019");
  }

  @Test
  void consentedDataUseTerms_value_collaboratorRequired() {
    DataUse dataUse = new DataUseBuilder().setCollaboratorRequired(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000020"), "COL should map to DUO:0000020");
  }

  @Test
  void consentedDataUseTerms_value_ethicsApprovalRequired() {
    DataUse dataUse = new DataUseBuilder().setEthicsApprovalRequired(true).build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000021"), "IRB should map to DUO:0000021");
  }

  @Test
  void consentedDataUseTerms_value_geographicalRestrictions() {
    DataUse dataUse = new DataUseBuilder().setGeographicalRestrictions("US-only").build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000022"), "GS should map to DUO:0000022");
  }

  @Test
  void consentedDataUseTerms_value_geographicalRestrictions_blankStringNotIncluded() {
    DataUse dataUse = new DataUseBuilder().setGeographicalRestrictions("   ").build();
    List<?> terms = valueFor(dataUse);
    assertTrue(
        terms.stream().noneMatch("DUO:0000022"::equals),
        "Blank geographical restriction should not add DUO:0000022");
  }

  @Test
  void consentedDataUseTerms_value_publicationMoratorium() {
    DataUse dataUse = new DataUseBuilder().setPublicationMoratorium("2027-01-01").build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000024"), "MOR should map to DUO:0000024");
  }

  @Test
  void consentedDataUseTerms_value_multipleCombinedTerms() {
    DataUse dataUse =
        new DataUseBuilder()
            .setHmbResearch(true)
            .setEthicsApprovalRequired(true)
            .setNonProfitUse(true)
            .build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.contains("DUO:0000006"));
    assertTrue(terms.contains("DUO:0000021"));
    assertTrue(terms.contains("DUO:0000018"));
  }

  @Test
  void consentedDataUseTerms_value_falseBooleansNotIncluded() {
    DataUse dataUse =
        new DataUseBuilder()
            .setGeneralUse(false)
            .setHmbResearch(false)
            .setNonProfitUse(false)
            .build();
    List<?> terms = valueFor(dataUse);
    assertTrue(terms.isEmpty(), "False boolean fields should not produce any DUO terms");
  }

  // Helper: build a dataset with the given DataUse and return the visa value
  @SuppressWarnings("unchecked")
  private List<String> valueFor(DataUse dataUse) {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDataUse(dataUse);
    return (List<String>) new ConsentedDataUseTermsVisa(dataset).value();
  }

  @Test
  void consentedDataUseTerms_source_isIss() {
    assertEquals(PassportService.ISS, new ConsentedDataUseTermsVisa(datasetWithAlias(1)).source());
  }

  @Test
  void consentedDataUseTerms_by_isDac() {
    assertEquals(
        VisaBy.DAC.name().toLowerCase(), new ConsentedDataUseTermsVisa(datasetWithAlias(1)).by());
  }

  @Test
  void consentedDataUseTerms_asserted_usesDatasetCreateDate() {
    Dataset dataset = datasetWithAlias(1);
    Date createDate = new Date(1_000_000_000L);
    dataset.setCreateDate(createDate);
    ConsentedDataUseTermsVisa visa = new ConsentedDataUseTermsVisa(dataset);
    assertEquals(PassportService.getEpochSeconds(createDate.toInstant()), visa.asserted());
  }

  @Test
  void consentedDataUseTerms_asserted_fallsBackToNowWhenCreateDateNull() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setCreateDate(null);
    long before = Instant.now().getEpochSecond();
    long asserted = new ConsentedDataUseTermsVisa(dataset).asserted();
    long after = Instant.now().getEpochSecond();
    assertTrue(asserted >= before && asserted <= after);
  }

  @Test
  void consentedDataUseTerms_asserted_handlesSqlDate() {
    Dataset dataset = datasetWithAlias(42);
    java.sql.Date createDate = new java.sql.Date(2_100_000_000L);
    dataset.setCreateDate(createDate);

    ConsentedDataUseTermsVisa visa = new ConsentedDataUseTermsVisa(dataset);

    assertEquals(
        PassportService.getEpochSeconds(Instant.ofEpochMilli(createDate.getTime())),
        visa.asserted());
  }

  // -----------------------------------------------------------------------
  // OversightBodiesVisa
  // -----------------------------------------------------------------------

  @Test
  void oversightBodies_type() {
    assertEquals(VisaClaimTypes.OVERSIGHT_BODIES.type, new OversightBodiesVisa(dac(7)).type());
  }

  @Test
  void oversightBodies_value_containsDacId() {
    Dac dac = dac(7);
    assertEquals(PassportService.ISS + "/dac/7", new OversightBodiesVisa(dac).value().toString());
  }

  @Test
  void oversightBodies_source_isIss() {
    assertEquals(PassportService.ISS, new OversightBodiesVisa(dac(1)).source());
  }

  @Test
  void oversightBodies_by_isDac() {
    assertEquals(VisaBy.DAC.name().toLowerCase(), new OversightBodiesVisa(dac(1)).by());
  }

  @Test
  void oversightBodies_asserted_usesDacCreateDate() {
    Dac dac = dac(1);
    Date createDate = new Date(2_000_000_000L);
    dac.setCreateDate(createDate);
    assertEquals(
        PassportService.getEpochSeconds(createDate.toInstant()),
        new OversightBodiesVisa(dac).asserted());
  }

  @Test
  void oversightBodies_asserted_fallsBackToNowWhenCreateDateNull() {
    Dac dac = dac(1);
    dac.setCreateDate(null);
    long before = Instant.now().getEpochSecond();
    long asserted = new OversightBodiesVisa(dac).asserted();
    long after = Instant.now().getEpochSecond();
    assertTrue(asserted >= before && asserted <= after);
  }

  @Test
  void oversightBodies_asserted_handlesSqlDate() {
    Dac dac = dac(1);
    java.sql.Date createDate = new java.sql.Date(2_000_000_000L);
    dac.setCreateDate(createDate);
    assertEquals(
        PassportService.getEpochSeconds(Instant.ofEpochMilli(createDate.getTime())),
        new OversightBodiesVisa(dac).asserted());
  }

  // -----------------------------------------------------------------------
  // RequiredAgreementsVisa
  // -----------------------------------------------------------------------

  @Test
  void requiredAgreements_type() {
    assertEquals(
        VisaClaimTypes.REQUIRED_AGREEMENTS.type, new RequiredAgreementsVisa(daa(5)).type());
  }

  @Test
  void requiredAgreements_value_containsDaaId() {
    assertEquals(
        PassportService.ISS + "/daa/5", new RequiredAgreementsVisa(daa(5)).value().toString());
  }

  @Test
  void requiredAgreements_source_isIss() {
    assertEquals(PassportService.ISS, new RequiredAgreementsVisa(daa(1)).source());
  }

  @Test
  void requiredAgreements_by_isSo() {
    assertEquals(VisaBy.SO.name().toLowerCase(), new RequiredAgreementsVisa(daa(1)).by());
  }

  @Test
  void requiredAgreements_asserted_usesDaaCreateDate() {
    Instant createDate = Instant.ofEpochSecond(3_000_000L);
    DataAccessAgreement daa = daa(1);
    daa.setCreateDate(createDate);
    assertEquals(
        PassportService.getEpochSeconds(createDate), new RequiredAgreementsVisa(daa).asserted());
  }

  @Test
  void requiredAgreements_asserted_fallsBackToNowWhenCreateDateNull() {
    DataAccessAgreement daa = daa(1);
    daa.setCreateDate(null);
    long before = Instant.now().getEpochSecond();
    long asserted = new RequiredAgreementsVisa(daa).asserted();
    long after = Instant.now().getEpochSecond();
    assertTrue(asserted >= before && asserted <= after);
  }

  // -----------------------------------------------------------------------
  // ApprovedUsersVisa
  // -----------------------------------------------------------------------

  @Test
  void approvedUsers_type() {
    assertEquals(VisaClaimTypes.APPROVED_USERS.type, new ApprovedUsersVisa("DUOS-000001").type());
  }

  @Test
  void approvedUsers_value_containsDatasetIdentifier() {
    ApprovedUsersVisa visa = new ApprovedUsersVisa("DUOS-000042");
    assertEquals(PassportService.getApprovedUsersEndpoint("DUOS-000042"), visa.value().toString());
  }

  @Test
  void approvedUsers_value_containsDatasetIdentifierInUrl() {
    ApprovedUsersVisa visa = new ApprovedUsersVisa("DUOS-000042");
    assertTrue(visa.value().toString().contains("DUOS-000042"));
  }

  @Test
  void approvedUsers_source_isIss() {
    assertEquals(PassportService.ISS, new ApprovedUsersVisa("DUOS-000001").source());
  }

  @Test
  void approvedUsers_by_isDac() {
    assertEquals(VisaBy.DAC.name().toLowerCase(), new ApprovedUsersVisa("DUOS-000001").by());
  }

  @Test
  void approvedUsers_asserted_isEpochSeconds() {
    long before = Instant.now().getEpochSecond();
    long asserted = new ApprovedUsersVisa("DUOS-000001").asserted();
    long after = Instant.now().getEpochSecond();
    assertTrue(asserted >= before && asserted <= after);
  }

  @Test
  void approvedUsers_asserted_isNotMilliseconds() {
    long asserted = new ApprovedUsersVisa("DUOS-000001").asserted();
    long nowSeconds = Instant.now().getEpochSecond();
    // If asserted were in milliseconds it would be ~1000x larger than nowSeconds
    assertTrue(asserted <= nowSeconds + 5, "asserted should be seconds, not milliseconds");
  }

  // -----------------------------------------------------------------------
  // Common contract across all four visa types
  // -----------------------------------------------------------------------

  @Test
  void allVisaTypes_haveNonNullFields() {
    VisaClaimType[] visas = {
      new ConsentedDataUseTermsVisa(datasetWithAlias(1)),
      new OversightBodiesVisa(dac(1)),
      new RequiredAgreementsVisa(daa(1)),
      new ApprovedUsersVisa("DUOS-000001")
    };
    for (VisaClaimType v : visas) {
      assertNotNull(v.type(), "type must not be null for " + v.getClass().getSimpleName());
      assertNotNull(v.value(), "value must not be null for " + v.getClass().getSimpleName());
      assertNotNull(v.source(), "source must not be null for " + v.getClass().getSimpleName());
      assertNotNull(v.by(), "by must not be null for " + v.getClass().getSimpleName());
      assertTrue(v.asserted() > 0, "asserted must be positive for " + v.getClass().getSimpleName());
    }
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private Dataset datasetWithAlias(int alias) {
    Dataset d = new Dataset();
    d.setAlias(alias);
    d.setCreateDate(new Date());
    return d;
  }

  private Dac dac(int dacId) {
    Dac dac = new Dac();
    dac.setDacId(dacId);
    dac.setCreateDate(new Date());
    return dac;
  }

  private DataAccessAgreement daa(int daaId) {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    daa.setCreateDate(Instant.now());
    return daa;
  }
}
