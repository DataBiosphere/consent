package org.broadinstitute.consent.http.service.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.DacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassportServiceTest extends AbstractTestHelper {

  @Mock private DatasetDAO datasetDAO;
  @Mock private DacService dacService;
  @Mock private DuosUser duosUser;

  private PassportService service;

  @BeforeEach
  void setUp() {
    service = new PassportService(datasetDAO, dacService);
  }

  @Test
  void testGeneratePassport_nullUser() {
    when(duosUser.getUser()).thenReturn(null);
    assertThrows(NotFoundException.class, () -> service.generatePassport(null));
    assertThrows(NotFoundException.class, () -> service.generatePassport(duosUser));
  }

  @Test
  void generatePassport_buildsRoleResearcherAndGrantVisas() {
    ApprovedDataset d1 = createApprovedDataset();
    ApprovedDataset d2 = createApprovedDataset();
    User user = createUser();
    UserStatusInfo userStatusInfo = createUserStatusInfo(user);

    when(duosUser.getUser()).thenReturn(user);
    when(duosUser.getUserStatusInfo()).thenReturn(userStatusInfo);
    when(datasetDAO.getApprovedDatasets(user.getUserId())).thenReturn(List.of(d1, d2));

    PassportClaim claim = service.generatePassport(duosUser);

    assertNotNull(claim);
    assertNotNull(claim.ga4gh_passport_v1());
    assertEquals(4, claim.ga4gh_passport_v1().size(), "2 dataset grants + role + researcher");

    // Ensure all ga4gh_passport_v1 have common fields set correctly
    claim
        .ga4gh_passport_v1()
        .forEach(
            v -> {
              assertEquals(PassportService.ISS, v.iss());
              assertEquals(userStatusInfo.getUserSubjectId(), v.sub());
              assertNotNull(v.iat());
              assertNotNull(v.exp());
              assertSeconds(v);
              assertTrue(v.exp() > v.iat(), "exp should be after iat");
              assertNotNull(v.ga4gh_visa_v1());
              assertNotNull(v.ga4gh_visa_v1().type());
              assertNotNull(v.ga4gh_visa_v1().value());
            });

    long roleCount =
        claim.ga4gh_passport_v1().stream()
            .filter(v -> VisaClaimTypes.AFFILIATION_AND_ROLE.type.equals(v.ga4gh_visa_v1().type()))
            .count();
    long researcherCount =
        claim.ga4gh_passport_v1().stream()
            .filter(v -> VisaClaimTypes.RESEARCHER_STATUS.type.equals(v.ga4gh_visa_v1().type()))
            .count();
    long grantCount =
        claim.ga4gh_passport_v1().stream()
            .filter(
                v -> VisaClaimTypes.CONTROLLED_ACCESS_GRANTS.type.equals(v.ga4gh_visa_v1().type()))
            .count();

    assertEquals(1, roleCount);
    assertEquals(1, researcherCount);
    assertEquals(2, grantCount);
  }

  private void assertSeconds(Visa v) {
    // iat should be Unix seconds, not milliseconds (ms would be ~1000x larger than nowSeconds)
    long nowSeconds = Instant.now().getEpochSecond();
    assertTrue(
        v.iat() <= nowSeconds + 300,
        "iat should be expressed in seconds since epoch, not milliseconds");
    assertTrue(
        v.exp() <= nowSeconds + 300 + PassportService.EXPIRATION_SECONDS,
        "exp should be expressed in seconds since epoch, not milliseconds");
  }

  @Test
  void buildControlledAccessGrants_deduplicatesByDatasetIdentifier() {
    ApprovedDataset d1 = createApprovedDataset();
    ApprovedDataset d1Dup = createApprovedDataset();
    d1Dup.setDatasetIdentifier(d1.getDatasetIdentifier()); // same identifier, different object
    ApprovedDataset d2 = createApprovedDataset();
    User user = createUser();
    UserStatusInfo userStatusInfo = createUserStatusInfo(user);

    List<Visa> visas =
        service.buildControlledAccessGrants(
            userStatusInfo.getUserSubjectId(), List.of(d1, d1Dup, d2));

    assertNotNull(visas);
    assertEquals(2, visas.size(), "duplicate dataset identifiers should be collapsed");

    assertTrue(
        visas.stream()
            .allMatch(
                v ->
                    PassportService.ISS.equals(v.iss())
                        && userStatusInfo.getUserSubjectId().equals(v.sub())
                        && VisaClaimTypes.CONTROLLED_ACCESS_GRANTS.type.equals(
                            v.ga4gh_visa_v1().type())));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void testAffiliationAndRole_nullEmail(String email) {
    User user = createUser();
    user.setEmail(email);
    AffiliationAndRole affiliationAndRole = new AffiliationAndRole(user);
    assertEquals(AffiliationAndRole.DEFAULT_VALUE, affiliationAndRole.value());
    assertEquals(PassportService.ISS, affiliationAndRole.source());
    assertTrue(affiliationAndRole.asserted() > 0);
  }

  @Test
  void testAffiliationAndRole_withLibraryCard() {
    User user = createUser();
    LibraryCard card = new LibraryCard();
    user.setLibraryCard(card);
    AffiliationAndRole affiliationAndRole = new AffiliationAndRole(user);
    assertTrue(affiliationAndRole.value().contains("faculty@example.org"));
    assertEquals(PassportService.ISS, affiliationAndRole.source());
    assertEquals(VisaBy.SO.name().toLowerCase(), affiliationAndRole.by());
    assertTrue(affiliationAndRole.asserted() > 0);
  }

  @Test
  void testApprovedDataset_nullDatasetExpiration() {
    ApprovedDataset d = new ApprovedDataset(1, "DUOS-000001", "DUOS-000001 name", "DAC 001", null);
    ControlledAccessGrants grants = new ControlledAccessGrants(d);
    assertNotNull(grants.asserted(), "asserted should not be if expiration date is null");
  }

  @Test
  void testApprovedDataset_nullDatasetIdentifier() {
    ApprovedDataset d1 = createApprovedDataset();
    d1.setDatasetIdentifier(null); // should be filtered out
    ApprovedDataset d2 = createApprovedDataset();

    List<Visa> visas = service.buildControlledAccessGrants("userSubjectId", List.of(d1, d2));
    assertNotNull(visas);
    assertEquals(1, visas.size(), "null dataset identifiers should not be included");
  }

  @Test
  void testNullUserSubjectInfo() {
    User user = createUser();
    when(duosUser.getUser()).thenReturn(user);
    when(duosUser.getUserStatusInfo()).thenReturn(null);
    when(datasetDAO.getApprovedDatasets(user.getUserId())).thenReturn(List.of());

    PassportClaim claim = service.generatePassport(duosUser);

    assertNotNull(claim);
    assertNotNull(claim.ga4gh_passport_v1());
    claim
        .ga4gh_passport_v1()
        .forEach(v -> assertEquals("internal_subject_id_" + user.getUserId(), v.sub()));
  }

  private User createUser() {
    User user = new User();
    user.setUserId(123);
    user.setEmail("test@example.org");
    user.setCreateDate(Timestamp.from(Instant.now()));
    return user;
  }

  private UserStatusInfo createUserStatusInfo(User user) {
    UserStatusInfo info = new UserStatusInfo();
    info.setUserEmail(user.getEmail());
    info.setUserSubjectId(randomAlphanumeric(10));
    info.setEnabled(true);
    info.setTosAccepted(true);
    return info;
  }

  // -----------------------------------------------------------------------
  // generateDataPassport
  // -----------------------------------------------------------------------

  @Test
  void generateDataPassport_datasetNotFound_throwsNotFoundException() {
    when(datasetDAO.findDatasetByAlias(1)).thenReturn(null);
    assertThrows(NotFoundException.class, () -> service.generateDataPassport("DUOS-000001"));
  }

  @Test
  void generateDataPassport_datasetWithNoDac_returnsOnlyConsentedDataUseTermsVisa() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(null);
    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    assertNotNull(claim);
    assertEquals(1, claim.ga4gh_passport_v1().size());
    assertEquals(
        VisaClaimTypes.CONSENTED_DATA_USE_TERMS.type,
        claim.ga4gh_passport_v1().getFirst().ga4gh_visa_v1().type());
  }

  @Test
  void generateDataPassport_datasetWithDacAndNoDaa_returnsTwoVisas() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(10);
    Dac dac = dacWithId(10);
    dac.setAssociatedDaa(null);

    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);
    when(dacService.findById(10)).thenReturn(dac);

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    assertNotNull(claim);
    assertEquals(2, claim.ga4gh_passport_v1().size());
    assertVisaTypePresent(claim, VisaClaimTypes.CONSENTED_DATA_USE_TERMS.type);
    assertVisaTypePresent(claim, VisaClaimTypes.OVERSIGHT_BODIES.type);
  }

  @Test
  void generateDataPassport_datasetWithDacAndDaa_returnsThreeVisas() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(10);
    Dac dac = dacWithId(10);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(99);
    daa.setCreateDate(Instant.now());
    dac.setAssociatedDaa(daa);

    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);
    when(dacService.findById(10)).thenReturn(dac);

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    assertNotNull(claim);
    assertEquals(3, claim.ga4gh_passport_v1().size());
    assertVisaTypePresent(claim, VisaClaimTypes.CONSENTED_DATA_USE_TERMS.type);
    assertVisaTypePresent(claim, VisaClaimTypes.OVERSIGHT_BODIES.type);
    assertVisaTypePresent(claim, VisaClaimTypes.REQUIRED_AGREEMENTS.type);
  }

  @Test
  void generateDataPassport_subFieldIsDatasetIdentifier() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(null);
    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    claim
        .ga4gh_passport_v1()
        .forEach(v -> assertEquals("DUOS-000001", v.sub(), "sub should be the dataset identifier"));
  }

  @Test
  void generateDataPassport_issFieldIsIss() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(null);
    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    claim.ga4gh_passport_v1().forEach(v -> assertEquals(PassportService.ISS, v.iss()));
  }

  @Test
  void generateDataPassport_iatAndExpAreEpochSeconds() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(null);
    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    long nowSeconds = Instant.now().getEpochSecond();
    claim
        .ga4gh_passport_v1()
        .forEach(
            v -> {
              assertTrue(v.iat() <= nowSeconds + 5, "iat should be seconds, not milliseconds");
              assertTrue(v.exp() > v.iat(), "exp should be after iat");
              assertEquals(PassportService.EXPIRATION_SECONDS, v.exp() - v.iat());
            });
  }

  @Test
  void generateDataPassport_dacNotFound_returnsOnlyConsentedDataUseTermsVisa() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(10);
    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);
    when(dacService.findById(10)).thenReturn(null);

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    assertEquals(1, claim.ga4gh_passport_v1().size());
    assertVisaTypePresent(claim, VisaClaimTypes.CONSENTED_DATA_USE_TERMS.type);
  }

  @Test
  void
      generateDataPassport_unsupportedOperationFromDacLookup_returnsOnlyConsentedDataUseTermsVisa() {
    Dataset dataset = datasetWithAlias(1);
    dataset.setDacId(10);
    when(datasetDAO.findDatasetByAlias(1)).thenReturn(dataset);
    when(dacService.findById(10)).thenThrow(new UnsupportedOperationException("unsupported"));

    PassportClaim claim = service.generateDataPassport("DUOS-000001");

    assertNotNull(claim);
    assertEquals(1, claim.ga4gh_passport_v1().size());
    assertVisaTypePresent(claim, VisaClaimTypes.CONSENTED_DATA_USE_TERMS.type);
  }

  private void assertVisaTypePresent(PassportClaim claim, String type) {
    assertTrue(
        claim.ga4gh_passport_v1().stream().anyMatch(v -> type.equals(v.ga4gh_visa_v1().type())),
        "Expected visa type '%s' to be present".formatted(type));
  }

  private Dataset datasetWithAlias(int alias) {
    Dataset d = new Dataset();
    d.setAlias(alias);
    d.setCreateDate(new Date());
    return d;
  }

  private Dac dacWithId(int dacId) {
    Dac dac = new Dac();
    dac.setDacId(dacId);
    dac.setCreateDate(new Date());
    return dac;
  }

  private int datasetCounter = 0;

  private ApprovedDataset createApprovedDataset() {
    datasetCounter++;
    String datasetIdentifier = "DUOS-" + datasetCounter;
    ApprovedDataset d =
        new ApprovedDataset(
            datasetCounter,
            "DAR-" + datasetCounter,
            " Dataset " + randomAlphabetic(10),
            "DAC 001",
            Timestamp.from(Instant.now()));
    d.setDatasetIdentifier(datasetIdentifier);
    return d;
  }

  @Test
  void testAffiliationAndRole_assertedHandlesSqlDateOnUser() {
    User user = createUser();
    java.sql.Date sqlDate = new java.sql.Date(1_700_000_000_000L);
    user.setCreateDate(sqlDate);

    AffiliationAndRole affiliationAndRole = new AffiliationAndRole(user);

    assertEquals(
        PassportService.getEpochSeconds(Instant.ofEpochMilli(sqlDate.getTime())),
        affiliationAndRole.asserted());
  }

  @Test
  void testAffiliationAndRole_assertedHandlesSqlDateOnLibraryCard() {
    User user = createUser();
    LibraryCard card = new LibraryCard();
    java.sql.Date sqlDate = new java.sql.Date(1_710_000_000_000L);
    card.setCreateDate(sqlDate);
    user.setLibraryCard(card);

    AffiliationAndRole affiliationAndRole = new AffiliationAndRole(user);

    assertEquals(
        PassportService.getEpochSeconds(Instant.ofEpochMilli(sqlDate.getTime())),
        affiliationAndRole.asserted());
  }

  @Test
  void testResearcherStatus_assertedHandlesSqlDateOnUser() {
    User user = createUser();
    java.sql.Date sqlDate = new java.sql.Date(1_720_000_000_000L);
    user.setCreateDate(sqlDate);

    ResearcherStatus researcherStatus = new ResearcherStatus(user);

    assertEquals(
        PassportService.getEpochSeconds(Instant.ofEpochMilli(sqlDate.getTime())),
        researcherStatus.asserted());
  }

  @Test
  void testResearcherStatus_assertedHandlesSqlDateOnLibraryCard() {
    User user = createUser();
    LibraryCard card = new LibraryCard();
    java.sql.Date sqlDate = new java.sql.Date(1_730_000_000_000L);
    card.setCreateDate(sqlDate);
    user.setLibraryCard(card);

    ResearcherStatus researcherStatus = new ResearcherStatus(user);

    assertEquals(
        PassportService.getEpochSeconds(Instant.ofEpochMilli(sqlDate.getTime())),
        researcherStatus.asserted());
  }
}
