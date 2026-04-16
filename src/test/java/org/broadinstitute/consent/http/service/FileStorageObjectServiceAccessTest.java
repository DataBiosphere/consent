package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import java.util.List;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.OperationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Full access-matrix coverage for {@link FileStorageObjectService#checkAccess}.
 *
 * <p>Matrix under test:
 *
 * <pre>
 * Entity   | Category                     | CRUD                           | READ
 * ---------+------------------------------+--------------------------------+-----------------------------
 * DAR      | IRB_COLLABORATION_LETTER     | creator only                   | creator, ADMIN, CHAIR, MEMBER
 * DAR      | DATA_USE_LETTER              | creator only                   | creator, ADMIN, CHAIR, MEMBER
 * DAC      | DATA_ACCESS_AGREEMENT        | ADMIN or DAC-scoped CHAIR      | any authenticated user
 * DATASET  | NIH_INSTITUTIONAL_CERT       | ADMIN, DATASUBMITTER, CHAIR    | + MEMBER
 * STUDY    | ALTERNATIVE_DATA_SHARING_PLAN| ADMIN, DATASUBMITTER, CHAIR    | same (no MEMBER)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class FileStorageObjectServiceAccessTest {

  @Mock private FileStorageObjectDAO fileStorageObjectDAO;
  @Mock private GCSService gcsService;
  @Mock private DatasetService datasetService;
  @Mock private DacService dacService;
  @Mock private DataAccessRequestService dataAccessRequestService;

  private FileStorageObjectService service;

  @BeforeEach
  void setUp() {
    service =
        new FileStorageObjectService(
            fileStorageObjectDAO, gcsService, datasetService, dacService, dataAccessRequestService);
  }

  // ---------------------------------------------------------------------------
  // Helper factories
  // ---------------------------------------------------------------------------

  private User adminUser() {
    User u = new User();
    u.setUserId(1);
    u.setAdminRole();
    return u;
  }

  private User chairUser(Integer dacId) {
    User u = new User();
    u.setUserId(2);
    if (dacId != null) {
      u.setChairpersonRoleWithDAC(dacId);
    } else {
      u.setChairpersonRole();
    }
    return u;
  }

  private User memberUser() {
    User u = new User();
    u.setUserId(3);
    u.setMemberRole();
    return u;
  }

  private User dataSubmitterUser() {
    User u = new User();
    u.setUserId(4);
    u.setRoles(List.of(UserRoles.DataSubmitter()));
    return u;
  }

  private User researcherUser() {
    User u = new User();
    u.setUserId(5);
    u.setResearcherRole();
    return u;
  }

  private User darCreatorUser(String darReferenceId) {
    User u = new User();
    u.setUserId(99);
    u.setResearcherRole();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(u.getUserId());
    when(dataAccessRequestService.findByReferenceId(darReferenceId)).thenReturn(dar);
    return u;
  }

  private User nonCreatorUser(String darReferenceId) {
    User u = new User();
    u.setUserId(77);
    u.setResearcherRole();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(999); // different user
    when(dataAccessRequestService.findByReferenceId(darReferenceId)).thenReturn(dar);
    return u;
  }

  // ---------------------------------------------------------------------------
  // DAR — IRB_COLLABORATION_LETTER
  // ---------------------------------------------------------------------------

  @Nested
  class DarIrbCollaborationLetter {

    private static final String DAR_ID = "DAR-100";

    @Test
    void crudAllowedForCreator() {
      User creator = darCreatorUser(DAR_ID);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  creator,
                  "dar",
                  DAR_ID,
                  FileCategory.IRB_COLLABORATION_LETTER,
                  OperationType.CRUD));
    }

    @Test
    void crudDeniedForNonCreator() {
      User other = nonCreatorUser(DAR_ID);
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  other, "dar", DAR_ID, FileCategory.IRB_COLLABORATION_LETTER, OperationType.CRUD));
    }

    @Test
    void readAllowedForCreator() {
      User creator = darCreatorUser(DAR_ID);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  creator,
                  "dar",
                  DAR_ID,
                  FileCategory.IRB_COLLABORATION_LETTER,
                  OperationType.READ));
    }

    @Test
    void readAllowedForAdmin() {
      // Admin is not the creator; mock DAR with different userId
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(DAR_ID)).thenReturn(dar);
      User admin = adminUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  admin, "dar", DAR_ID, FileCategory.IRB_COLLABORATION_LETTER, OperationType.READ));
    }

    @Test
    void readAllowedForChairperson() {
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(DAR_ID)).thenReturn(dar);
      User chair = chairUser(null);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  chair, "dar", DAR_ID, FileCategory.IRB_COLLABORATION_LETTER, OperationType.READ));
    }

    @Test
    void readAllowedForMember() {
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(DAR_ID)).thenReturn(dar);
      User member = memberUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  member,
                  "dar",
                  DAR_ID,
                  FileCategory.IRB_COLLABORATION_LETTER,
                  OperationType.READ));
    }

    @Test
    void readDeniedForNonCreatorResearcher() {
      User other = nonCreatorUser(DAR_ID);
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  other, "dar", DAR_ID, FileCategory.IRB_COLLABORATION_LETTER, OperationType.READ));
    }
  }

  // ---------------------------------------------------------------------------
  // DAR — DATA_USE_LETTER
  // ---------------------------------------------------------------------------

  @Nested
  class DarDataUseLetter {

    private static final String DAR_ID = "DAR-200";

    @Test
    void crudAllowedForCreator() {
      User creator = darCreatorUser(DAR_ID);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  creator, "dar", DAR_ID, FileCategory.DATA_USE_LETTER, OperationType.CRUD));
    }

    @Test
    void crudDeniedForNonCreator() {
      User other = nonCreatorUser(DAR_ID);
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  other, "dar", DAR_ID, FileCategory.DATA_USE_LETTER, OperationType.CRUD));
    }

    @Test
    void readAllowedForAdmin() {
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(DAR_ID)).thenReturn(dar);
      User admin = adminUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  admin, "dar", DAR_ID, FileCategory.DATA_USE_LETTER, OperationType.READ));
    }

    @Test
    void readDeniedForNonCreatorResearcher() {
      User other = nonCreatorUser(DAR_ID);
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  other, "dar", DAR_ID, FileCategory.DATA_USE_LETTER, OperationType.READ));
    }
  }

  // ---------------------------------------------------------------------------
  // DAR — invalid category
  // ---------------------------------------------------------------------------

  @Test
  void darWithDisallowedCategoryThrowsBadRequest() {
    User admin = adminUser();
    assertThrows(
        BadRequestException.class,
        () ->
            service.checkAccess(
                admin,
                "dar",
                "DAR-1",
                FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                OperationType.READ));
  }

  // ---------------------------------------------------------------------------
  // DAC — DATA_ACCESS_AGREEMENT
  // ---------------------------------------------------------------------------

  @Nested
  class DacDataAccessAgreement {

    private static final String DAC_ID = "10";
    private static final int DAC_INT_ID = 10;

    @Test
    void crudAllowedForAdmin() {
      User admin = adminUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  admin, "dac", DAC_ID, FileCategory.DATA_ACCESS_AGREEMENT, OperationType.CRUD));
    }

    @Test
    void crudAllowedForDacScopedChairperson() {
      User chair = chairUser(DAC_INT_ID);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  chair, "dac", DAC_ID, FileCategory.DATA_ACCESS_AGREEMENT, OperationType.CRUD));
    }

    @Test
    void crudDeniedForChairpersonOfDifferentDac() {
      User chair = chairUser(99);
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  chair, "dac", DAC_ID, FileCategory.DATA_ACCESS_AGREEMENT, OperationType.CRUD));
    }

    @Test
    void crudDeniedForMember() {
      User member = memberUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  member, "dac", DAC_ID, FileCategory.DATA_ACCESS_AGREEMENT, OperationType.CRUD));
    }

    @Test
    void readAllowedForAnyAuthenticatedUser() {
      User researcher = researcherUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  researcher,
                  "dac",
                  DAC_ID,
                  FileCategory.DATA_ACCESS_AGREEMENT,
                  OperationType.READ));
    }

    @Test
    void readAllowedForMember() {
      User member = memberUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  member, "dac", DAC_ID, FileCategory.DATA_ACCESS_AGREEMENT, OperationType.READ));
    }
  }

  // ---------------------------------------------------------------------------
  // DAC — invalid category
  // ---------------------------------------------------------------------------

  @Test
  void dacWithDisallowedCategoryThrowsBadRequest() {
    User admin = adminUser();
    assertThrows(
        BadRequestException.class,
        () ->
            service.checkAccess(
                admin, "dac", "10", FileCategory.IRB_COLLABORATION_LETTER, OperationType.READ));
  }

  // ---------------------------------------------------------------------------
  // DATASET — NIH_INSTITUTIONAL_CERTIFICATION
  // ---------------------------------------------------------------------------

  @Nested
  class DatasetNihInstitutionalCertification {

    @Test
    void crudAllowedForAdmin() {
      User admin = adminUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  admin,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.CRUD));
    }

    @Test
    void crudAllowedForDataSubmitter() {
      User dataSubmitter = dataSubmitterUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  dataSubmitter,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.CRUD));
    }

    @Test
    void crudAllowedForChairperson() {
      User chair = chairUser(null);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  chair,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.CRUD));
    }

    @Test
    void crudDeniedForMember() {
      User member = memberUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  member,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.CRUD));
    }

    @Test
    void crudDeniedForResearcher() {
      User researcher = researcherUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  researcher,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.CRUD));
    }

    @Test
    void readAllowedForAdmin() {
      User admin = adminUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  admin,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.READ));
    }

    @Test
    void readAllowedForMember() {
      User member = memberUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  member,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.READ));
    }

    @Test
    void readDeniedForResearcher() {
      User researcher = researcherUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  researcher,
                  "dataset",
                  "1",
                  FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                  OperationType.READ));
    }
  }

  // ---------------------------------------------------------------------------
  // DATASET — invalid category
  // ---------------------------------------------------------------------------

  @Test
  void datasetWithDisallowedCategoryThrowsBadRequest() {
    User admin = adminUser();
    assertThrows(
        BadRequestException.class,
        () ->
            service.checkAccess(
                admin, "dataset", "1", FileCategory.DATA_ACCESS_AGREEMENT, OperationType.READ));
  }

  // ---------------------------------------------------------------------------
  // STUDY — ALTERNATIVE_DATA_SHARING_PLAN
  // ---------------------------------------------------------------------------

  @Nested
  class StudyAlternativeDataSharingPlan {

    @Test
    void crudAllowedForAdmin() {
      User admin = adminUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  admin,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.CRUD));
    }

    @Test
    void crudAllowedForDataSubmitter() {
      User dataSubmitter = dataSubmitterUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  dataSubmitter,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.CRUD));
    }

    @Test
    void crudAllowedForChairperson() {
      User chair = chairUser(null);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  chair,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.CRUD));
    }

    @Test
    void crudDeniedForMember() {
      User member = memberUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  member,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.CRUD));
    }

    @Test
    void crudDeniedForResearcher() {
      User researcher = researcherUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  researcher,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.CRUD));
    }

    @Test
    void readAllowedForAdmin() {
      User admin = adminUser();
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  admin,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.READ));
    }

    @Test
    void readAllowedForChairperson() {
      User chair = chairUser(null);
      assertDoesNotThrow(
          () ->
              service.checkAccess(
                  chair,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.READ));
    }

    @Test
    void readDeniedForMember() {
      User member = memberUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  member,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.READ));
    }

    @Test
    void readDeniedForResearcher() {
      User researcher = researcherUser();
      assertThrows(
          ForbiddenException.class,
          () ->
              service.checkAccess(
                  researcher,
                  "study",
                  "1",
                  FileCategory.ALTERNATIVE_DATA_SHARING_PLAN,
                  OperationType.READ));
    }
  }

  // ---------------------------------------------------------------------------
  // STUDY — invalid category
  // ---------------------------------------------------------------------------

  @Test
  void studyWithDisallowedCategoryThrowsBadRequest() {
    User admin = adminUser();
    assertThrows(
        BadRequestException.class,
        () ->
            service.checkAccess(
                admin, "study", "1", FileCategory.DATA_ACCESS_AGREEMENT, OperationType.READ));
  }

  // ---------------------------------------------------------------------------
  // Entity-level READ (null category) — used by listDocuments / getDocument
  // ---------------------------------------------------------------------------

  @Nested
  class EntityLevelRead {

    @Test
    void darEntityReadAllowedForCreator() {
      String darId = "DAR-300";
      User creator = darCreatorUser(darId);
      assertDoesNotThrow(
          () -> service.checkAccess(creator, "dar", darId, null, OperationType.READ));
    }

    @Test
    void darEntityReadAllowedForAdmin() {
      String darId = "DAR-301";
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(darId)).thenReturn(dar);
      User admin = adminUser();
      assertDoesNotThrow(() -> service.checkAccess(admin, "dar", darId, null, OperationType.READ));
    }

    @Test
    void darEntityReadAllowedForChairperson() {
      String darId = "DAR-302";
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(darId)).thenReturn(dar);
      User chair = chairUser(null);
      assertDoesNotThrow(() -> service.checkAccess(chair, "dar", darId, null, OperationType.READ));
    }

    @Test
    void darEntityReadAllowedForMember() {
      String darId = "DAR-303";
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(darId)).thenReturn(dar);
      User member = memberUser();
      assertDoesNotThrow(() -> service.checkAccess(member, "dar", darId, null, OperationType.READ));
    }

    @Test
    void darEntityReadDeniedForNonPrivilegedNonCreator() {
      String darId = "DAR-304";
      DataAccessRequest dar = new DataAccessRequest();
      dar.setUserId(999);
      when(dataAccessRequestService.findByReferenceId(darId)).thenReturn(dar);
      User researcher = researcherUser();
      assertThrows(
          ForbiddenException.class,
          () -> service.checkAccess(researcher, "dar", darId, null, OperationType.READ));
    }

    @Test
    void dacEntityReadAllowedForAnyUser() {
      User researcher = researcherUser();
      assertDoesNotThrow(
          () -> service.checkAccess(researcher, "dac", "10", null, OperationType.READ));
    }

    @Test
    void datasetEntityReadAllowedForAdmin() {
      User admin = adminUser();
      assertDoesNotThrow(
          () -> service.checkAccess(admin, "dataset", "1", null, OperationType.READ));
    }

    @Test
    void datasetEntityReadAllowedForMember() {
      User member = memberUser();
      assertDoesNotThrow(
          () -> service.checkAccess(member, "dataset", "1", null, OperationType.READ));
    }

    @Test
    void datasetEntityReadDeniedForResearcher() {
      User researcher = researcherUser();
      assertThrows(
          ForbiddenException.class,
          () -> service.checkAccess(researcher, "dataset", "1", null, OperationType.READ));
    }

    @Test
    void studyEntityReadAllowedForAdmin() {
      User admin = adminUser();
      assertDoesNotThrow(() -> service.checkAccess(admin, "study", "1", null, OperationType.READ));
    }

    @Test
    void studyEntityReadAllowedForDataSubmitter() {
      User dataSubmitter = dataSubmitterUser();
      assertDoesNotThrow(
          () -> service.checkAccess(dataSubmitter, "study", "1", null, OperationType.READ));
    }

    @Test
    void studyEntityReadDeniedForResearcher() {
      User researcher = researcherUser();
      assertThrows(
          ForbiddenException.class,
          () -> service.checkAccess(researcher, "study", "1", null, OperationType.READ));
    }
  }

  // ---------------------------------------------------------------------------
  // Invalid entity
  // ---------------------------------------------------------------------------

  @Test
  void unknownEntityThrowsNotFoundException() {
    User admin = adminUser();
    assertThrows(
        jakarta.ws.rs.NotFoundException.class,
        () ->
            service.checkAccess(
                admin,
                "unknownEntity",
                "1",
                FileCategory.NIH_INSTITUTIONAL_CERTIFICATION,
                OperationType.READ));
  }

  // ---------------------------------------------------------------------------
  // Helper predicate unit tests
  // ---------------------------------------------------------------------------

  @Test
  void isDarCreatorReturnsTrueWhenUserIdMatches() {
    String darId = "DAR-500";
    User user = new User();
    user.setUserId(42);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(42);
    when(dataAccessRequestService.findByReferenceId(darId)).thenReturn(dar);

    service =
        new FileStorageObjectService(
            fileStorageObjectDAO, gcsService, datasetService, dacService, dataAccessRequestService);

    assertTrue(service.isDarCreator(user, darId));
  }

  @Test
  void isDarCreatorReturnsFalseWhenUserIdDiffers() {
    String darId = "DAR-501";
    User user = new User();
    user.setUserId(42);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(99);
    when(dataAccessRequestService.findByReferenceId(darId)).thenReturn(dar);

    service =
        new FileStorageObjectService(
            fileStorageObjectDAO, gcsService, datasetService, dacService, dataAccessRequestService);

    assertFalse(service.isDarCreator(user, darId));
  }

  @Test
  void isDacChairReturnsTrueForAdmin() {
    User admin = adminUser();
    assertTrue(service.isDacChair(admin, 10));
  }

  @Test
  void isDacChairReturnsTrueForScopedChair() {
    User chair = chairUser(10);
    assertTrue(service.isDacChair(chair, 10));
  }

  @Test
  void isDacChairReturnsFalseForChairOfDifferentDac() {
    User chair = chairUser(99);
    assertFalse(service.isDacChair(chair, 10));
  }

  @Test
  void isDacChairReturnsFalseForMember() {
    User member = memberUser();
    assertFalse(service.isDacChair(member, 10));
  }

  @Test
  void hasRoleReturnsTrueWhenRolePresent() {
    User admin = adminUser();
    assertTrue(service.hasRole(admin, UserRoles.ADMIN));
  }

  @Test
  void hasRoleReturnsFalseWhenRoleAbsent() {
    User researcher = researcherUser();
    assertFalse(service.hasRole(researcher, UserRoles.ADMIN));
  }
}
