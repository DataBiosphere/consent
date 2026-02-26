package org.broadinstitute.consent.http.service.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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

  private int datasetCounter = 0;

  @BeforeEach
  void setUp() {
    service = new PassportService(datasetDAO);
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
    assertNotNull(claim.visas());
    assertEquals(4, claim.visas().size(), "2 dataset grants + role + researcher");

    // Ensure all visas have common fields set correctly
    claim
        .visas()
        .forEach(
            v -> {
              assertEquals(PassportService.ISS, v.iss());
              assertEquals(userStatusInfo.getUserSubjectId(), v.sub());
              assertNotNull(v.iat());
              assertNotNull(v.exp());
              assertTrue(v.exp() > v.iat(), "exp should be after iat");
              assertNotNull(v.visaClaim());
              assertNotNull(v.visaClaim().type());
              assertNotNull(v.visaClaim().value());
            });

    long roleCount =
        claim.visas().stream()
            .filter(v -> VisaClaimTypes.AFFILIATION_AND_ROLE.type.equals(v.visaClaim().type()))
            .count();
    long researcherCount =
        claim.visas().stream()
            .filter(v -> VisaClaimTypes.RESEARCHER_STATUS.type.equals(v.visaClaim().type()))
            .count();
    long grantCount =
        claim.visas().stream()
            .filter(v -> VisaClaimTypes.CONTROLLED_ACCESS_GRANTS.type.equals(v.visaClaim().type()))
            .count();

    assertEquals(1, roleCount);
    assertEquals(1, researcherCount);
    assertEquals(2, grantCount);
  }

  @Test
  void buildControlledAccessGrants_deduplicatesByDatasetIdentifier() {
    ApprovedDataset d1 = createApprovedDataset();
    ApprovedDataset d1Dup = createApprovedDataset();
    d1Dup.setDatasetIdentifier(d1.getDatasetIdentifier()); // same identifier, different object
    ApprovedDataset d2 = createApprovedDataset();
    User user = createUser();
    UserStatusInfo userStatusInfo = createUserStatusInfo(user);

    List<Visa> visas = service.buildControlledAccessGrants(userStatusInfo, List.of(d1, d1Dup, d2));

    assertNotNull(visas);
    assertEquals(2, visas.size(), "duplicate dataset identifiers should be collapsed");

    assertTrue(
        visas.stream()
            .allMatch(
                v ->
                    PassportService.ISS.equals(v.iss())
                        && userStatusInfo.getUserSubjectId().equals(v.sub())
                        && VisaClaimTypes.CONTROLLED_ACCESS_GRANTS.type.equals(
                            v.visaClaim().type())));
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
    ApprovedDataset d = createApprovedDataset();
    d.setExpirationDate(null);
    ControlledAccessGrants grants = new ControlledAccessGrants(d);
    assertNull(grants.asserted(), "asserted should be null if expiration date is null");
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

  private ApprovedDataset createApprovedDataset() {
    datasetCounter++;
    String datasetIdentifier = "DUOS-" + datasetCounter;
    ApprovedDataset d =
        new ApprovedDataset(
            datasetCounter,
            datasetIdentifier,
            datasetIdentifier + " name",
            "DAC 001",
            Timestamp.from(Instant.now()));
    d.setDatasetIdentifier(datasetIdentifier);
    return d;
  }
}
