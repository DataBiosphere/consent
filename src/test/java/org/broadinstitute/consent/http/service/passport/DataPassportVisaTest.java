package org.broadinstitute.consent.http.service.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Data Passport visa types introduced in
 * https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5372874: ConsentedDataUseTermsVisa,
 * OversightBodiesVisa, and RequiredAgreementsVisa.
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
  void consentedDataUseTerms_value_containsDatasetIdentifier() {
    Dataset dataset = datasetWithAlias(42);
    ConsentedDataUseTermsVisa visa = new ConsentedDataUseTermsVisa(dataset);
    assertEquals(
        PassportService.ISS + "/dataset/" + dataset.getDatasetIdentifier() + "/dataUse",
        visa.value());
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
    assertEquals(PassportService.ISS + "/dac/7", new OversightBodiesVisa(dac).value());
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
    assertEquals(PassportService.ISS + "/daa/5", new RequiredAgreementsVisa(daa(5)).value());
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
  // Common contract across all three visa types
  // -----------------------------------------------------------------------

  @Test
  void allVisaTypes_haveNonNullFields() {
    VisaClaimType[] visas = {
      new ConsentedDataUseTermsVisa(datasetWithAlias(1)),
      new OversightBodiesVisa(dac(1)),
      new RequiredAgreementsVisa(daa(1))
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
