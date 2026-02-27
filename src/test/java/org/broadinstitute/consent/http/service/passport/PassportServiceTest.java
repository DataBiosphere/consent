package org.broadinstitute.consent.http.service.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
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
  @Mock private DuosUser duosUser;

  private PassportService service;

  @BeforeEach
  void setUp() {
    service = new PassportService(datasetDAO);
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
    assertNull(grants.asserted(), "asserted should be null if expiration date is null");
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
}
