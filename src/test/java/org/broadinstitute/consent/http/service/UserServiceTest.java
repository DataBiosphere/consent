package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserDAO userDAO;

  @Mock
  private UserPropertyDAO userPropertyDAO;

  @Mock
  private UserRoleDAO userRoleDAO;

  @Mock
  private VoteDAO voteDAO;


  @Mock
  private InstitutionDAO institutionDAO;

  @Mock
  private LibraryCardDAO libraryCardDAO;

  @Mock
  private AcknowledgementDAO acknowledgementDAO;

  @Mock
  private FileStorageObjectDAO fileStorageObjectDAO;

  @Mock
  private SamDAO samDAO;

  @Mock
  private UserServiceDAO userServiceDAO;

  @Mock
  private DaaDAO daaDAO;

  @Mock
  private EmailService emailService;

  @Mock
  private DraftService draftService;


  private UserService service;

  private void initService() {
    service = new UserService(userDAO, userPropertyDAO, userRoleDAO, voteDAO, institutionDAO,
        libraryCardDAO, acknowledgementDAO, fileStorageObjectDAO, samDAO, userServiceDAO, daaDAO,
        emailService, draftService);
  }

  @Test
  void testUpdateUserFieldsById() {
    UserRole admin = UserRoles.Admin();
    UserRole researcher = UserRoles.Researcher();
    UserRole chair = UserRoles.Chairperson();
    UserRole so = UserRoles.SigningOfficial();

    User user = new User();
    user.setUserId(1);

    // Note that we're starting out with 1 modifiable role (Admin) and 1 that is not (Chairperson)
    // and one role that should never be removed, but can be added (Researcher)
    // When we update this user, we'll ensure that the new roles are added, old roles are deleted,
    // and the researcher & chairperson roles remain.
    when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(
        List.of(admin, researcher, chair));
    when(userDAO.findUserById(any())).thenReturn(user);
    UserProperty prop = new UserProperty();
    prop.setPropertyValue("1");
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any())).thenReturn(
        List.of(prop));
    initService();
    try {
      UserUpdateFields fields = new UserUpdateFields();
      // We're modifying this user to have an SO role. This should leave in place
      // both the Researcher and Chairperson roles, but remove the Admin role.
      fields.setUserRoleIds(List.of(so.getRoleId()));
      fields.setDisplayName(RandomStringUtils.random(10, true, false));
      fields.setInstitutionId(1);
      fields.setEmailPreference(true);
      fields.setEraCommonsId(RandomStringUtils.random(10, true, false));
      fields.setSelectedSigningOfficialId(1);
      fields.setSuggestedSigningOfficial(RandomStringUtils.random(10, true, false));
      fields.setSuggestedInstitution(RandomStringUtils.random(10, true, false));
      fields.setDaaAcceptance(true);
      assertEquals(4, fields.buildUserProperties(user.getUserId()).size());
      service.updateUserFieldsById(fields, user.getUserId());
    } catch (Exception e) {
      fail(e.getMessage());
    }
    // We added 3 user property values, we should have props for them:
    verify(userDAO, times(1)).updateDisplayName(any(), any());
    verify(userDAO, times(1)).updateInstitutionId(any(), any());
    verify(userDAO, times(1)).updateEmailPreference(any(), any());
    verify(userDAO, times(1)).updateEraCommonsId(any(), any());
    verify(userPropertyDAO, times(1)).insertAll(any());
    // Verify role additions/deletions.
    verify(userRoleDAO, times(1)).insertUserRoles(List.of(so), 1);
    verify(userRoleDAO, times(1)).removeUserRoles(1, List.of(admin.getRoleId()));
  }

  @Test
  void testUpdateUserFieldsById_SendsEmailWhenSOInitalized() throws Exception {
    User user = new User();
    user.setUserId(1);

    when(userDAO.findUserById(any())).thenReturn(user);
    UserProperty prop = new UserProperty();
    prop.setPropertyValue("1");
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any())).thenReturn(
            List.of()) // first time, no SO id
        .thenReturn(List.of(prop)); // second time, has SO id
    initService();
    try {
      UserUpdateFields fields = new UserUpdateFields();
      fields.setSelectedSigningOfficialId(1);

      assertEquals(1, fields.buildUserProperties(user.getUserId()).size());
      service.updateUserFieldsById(fields, user.getUserId());
    } catch (Exception e) {
      fail(e.getMessage());
    }
    // We added 3 user property values, we should have props for them:
    verify(userDAO, never()).updateDisplayName(any(), any());
    verify(userDAO, never()).updateInstitutionId(any(), any());
    verify(userDAO, never()).updateEmailPreference(any(), any());
    verify(userDAO, never()).updateEraCommonsId(any(), any());
    verify(userPropertyDAO, times(1)).insertAll(any());

    verify(emailService, times(1)).sendNewResearcherMessage(any(), any());
  }

  @Test
  void testUpdateUserFieldsById_NoEmailOnSOChange() throws Exception {
    User user = new User();
    user.setUserId(1);

    when(userDAO.findUserById(any())).thenReturn(user);
    UserProperty prop1 = new UserProperty();
    prop1.setPropertyValue("1");
    UserProperty prop2 = new UserProperty();
    prop2.setPropertyValue("2");
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any())).thenReturn(
            List.of(prop1)) // first SO id
        .thenReturn(List.of(prop2)); // second SO id
    initService();
    try {
      UserUpdateFields fields = new UserUpdateFields();
      fields.setSelectedSigningOfficialId(2);

      assertEquals(1, fields.buildUserProperties(user.getUserId()).size());
      service.updateUserFieldsById(fields, user.getUserId());
    } catch (Exception e) {
      fail(e.getMessage());
    }
    // We added 3 user property values, we should have props for them:
    verify(userDAO, never()).updateDisplayName(any(), any());
    verify(userDAO, never()).updateInstitutionId(any(), any());
    verify(userDAO, never()).updateEmailPreference(any(), any());
    verify(userDAO, never()).updateEraCommonsId(any(), any());
    verify(userPropertyDAO, times(1)).insertAll(any());

    verify(emailService, never()).sendNewResearcherMessage(any(), any());
  }

  @Test
  void testUpdateUserFieldsById_NoEmailOnNoChange() throws Exception {
    User user = new User();
    user.setUserId(1);

    when(userDAO.findUserById(any())).thenReturn(user);
    UserProperty prop = new UserProperty();
    prop.setPropertyValue("1");
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any())).thenReturn(
            List.of(prop)) // first SO id
        .thenReturn(List.of(prop)); // second SO id
    initService();
    try {
      UserUpdateFields fields = new UserUpdateFields();
      fields.setSelectedSigningOfficialId(1);

      assertEquals(1, fields.buildUserProperties(user.getUserId()).size());
      service.updateUserFieldsById(fields, user.getUserId());
    } catch (Exception e) {
      fail(e.getMessage());
    }
    // We added 3 user property values, we should have props for them:
    verify(userDAO, never()).updateDisplayName(any(), any());
    verify(userDAO, never()).updateInstitutionId(any(), any());
    verify(userDAO, never()).updateEmailPreference(any(), any());
    verify(userDAO, never()).updateEraCommonsId(any(), any());
    verify(userPropertyDAO, times(1)).insertAll(any());

    verify(emailService, never()).sendNewResearcherMessage(any(), any());
  }

  @Test
  void createUserTest() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    when(userDAO.findUserById(any())).thenReturn(u);
    when(libraryCardDAO.findAllLibraryCardsByUserEmail(any())).thenReturn(Collections.emptyList());
    initService();
    try {
      service.createUser(u);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void createUserWithLibraryCardTest() {
    User u = generateUser();
    LibraryCard libraryCard = generateLibraryCard(u.getEmail());
    Integer institutionId = libraryCard.getInstitutionId();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    when(userDAO.findUserById(any())).thenReturn(u);
    when(libraryCardDAO.findAllLibraryCardsByUserEmail(u.getEmail())).thenReturn(
        List.of(libraryCard));
    initService();

    try {
      service.createUser(u);
    } catch (Exception e) {
      fail(e.getMessage());
    }

    assertEquals(institutionId, u.getInstitutionId());
    assertEquals(u.getUserId(), libraryCard.getUserId());
  }

  @Test
  void testCreateUserDuplicateEmail() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    when(userDAO.findUserByEmail(any())).thenReturn(u);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.createUser(u);
    });
  }

  @Test
  void testCreateUserNoDisplayName() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    u.setDisplayName(null);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.createUser(u);
    });
  }

  @Test
  void testCreateUserNoRoles() {
    User u = generateUser();
    when(userDAO.findUserById(any())).thenReturn(u);
    initService();
    User user = service.createUser(u);
    assertFalse(user.getRoles().isEmpty());
    assertEquals(UserRoles.RESEARCHER.getRoleId(), user.getRoles().get(0).getRoleId());
  }

  @Test
  void testCreateUserInvalidRoleCase1() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.CHAIRPERSON.getRoleId()));
    u.setRoles(roles);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.createUser(u);
    });
  }

  @Test
  void testCreateUserInvalidRoleCase2() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.MEMBER.getRoleId()));
    u.setRoles(roles);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.createUser(u);
    });
  }

  @Test
  void testCreateUserNoEmail() {
    User u = generateUser();
    u.setEmail(null);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.createUser(u);
    });
  }

  @Test
  void testFindUserById_HasLibraryCards() {
    User u = generateUser();
    LibraryCard one = generateLibraryCard(u);
    LibraryCard two = generateLibraryCard(u);
    List<LibraryCard> cards = List.of(one, two);
    when(userDAO.findUserById(any())).thenReturn(u);
    when(libraryCardDAO.findLibraryCardsByUserId(any())).thenReturn(cards);
    initService();

    User user = service.findUserById(u.getUserId());
    assertNotNull(user);
    assertNotNull(user.getLibraryCards());
    assertEquals(user.getLibraryCards().size(), 2);
    assertEquals(user.getLibraryCards().get(0).getId(), one.getId());
    assertEquals(user.getLibraryCards().get(1).getId(), two.getId());
  }

  @Test
  void testFindUserByIdNoRoles() {
    User u = generateUser();
    when(userDAO.findUserById(any())).thenReturn(u);
    initService();

    User user = service.findUserById(u.getUserId());
    assertNotNull(user);
    assertEquals(u.getEmail(), user.getEmail());
    assertNull(u.getRoles());
  }

  @Test
  void testFindUserByIdWithRoles() {
    User u = generateUser();
    List<UserRole> roleList = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()),
        generateRole(UserRoles.MEMBER.getRoleId()));
    u.setRoles(roleList);
    when(userDAO.findUserById(any())).thenReturn(u);
    initService();

    User user = service.findUserById(u.getUserId());
    assertNotNull(user);
    assertEquals(u.getEmail(), user.getEmail());
    assertFalse(u.getRoles().isEmpty());
    assertEquals(2, u.getRoles().size());
  }

  @Test
  void testFindUserByIdNotFound() {
    User u = generateUser();
    when(userDAO.findUserById(any())).thenReturn(null);
    initService();

    assertThrows(NotFoundException.class, () -> {
      service.findUserById(u.getUserId());
    });
  }

  @Test
  void testFindUserByEmailNoRoles() {
    User u = generateUser();
    when(userDAO.findUserByEmail(any())).thenReturn(u);
    initService();

    User user = service.findUserByEmail(u.getEmail());
    assertNotNull(user);
    assertEquals(u.getEmail(), user.getEmail());
    assertNull(u.getRoles());
  }

  @Test
  void testFindUserByEmailWithRoles() {
    User u = generateUser();
    List<UserRole> roleList = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()),
        generateRole(UserRoles.MEMBER.getRoleId()));
    u.setRoles(roleList);
    when(userDAO.findUserByEmail(any())).thenReturn(u);
    initService();

    User user = service.findUserByEmail(u.getEmail());
    assertNotNull(user);
    assertEquals(u.getEmail(), user.getEmail());
    assertFalse(u.getRoles().isEmpty());
    assertEquals(2, u.getRoles().size());
  }

  @Test
  void testFindUserByEmailNotFound() {
    User u = generateUser();
    when(userDAO.findUserByEmail(any())).thenReturn(null);
    initService();

    assertThrows(NotFoundException.class, () -> {
      service.findUserByEmail(u.getEmail());
    });
  }

  @Test
  void testDeleteUser() {
    User u = generateUser();
    doNothing().when(userPropertyDAO).deleteAllPropertiesByUser(any());
    when(userDAO.findUserByEmail(any())).thenReturn(u);
    initService();

    try {
      service.deleteUserByEmail(RandomStringUtils.random(10, true, false));
    } catch (Exception e) {
      fail("Should not fail: " + e.getMessage());
    }
  }

  @Test
  void testDeleteUserFailure() {
    when(userDAO.findUserByEmail(any())).thenThrow(new NotFoundException());
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.deleteUserByEmail(RandomStringUtils.random(10, true, false));
    });
  }

  @Test
  void testFindSOsByInstitutionId() {
    User u = generateUser();
    Integer institutionId = u.getInstitutionId();
    when(userDAO.getSOsByInstitution(any())).thenReturn(List.of(u, u, u));
    initService();
    List<SimplifiedUser> users = service.findSOsByInstitutionId(institutionId);
    assertEquals(3, users.size());
    assertEquals(u.getDisplayName(), users.get(0).displayName);
    assertEquals(u.getEmail(), users.get(0).email);
  }

  @Test
  void testFindSOsByInstitutionId_NullId() {
    initService();
    List<SimplifiedUser> users = service.findSOsByInstitutionId(null);
    assertEquals(0, users.size());
  }

  @Test
  void testFindUsersByInstitutionIdNullId() {
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.findUsersByInstitutionId(null);
    });
  }

  @Test
  void testFindUsersByInstitutionIdNullInstitution() {
    doThrow(new NotFoundException()).when(institutionDAO).findInstitutionById(anyInt());
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.findUsersByInstitutionId(1);
    });
  }

  @Test
  void testFindUsersByInstitutionIdSuccess() {
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(new Institution());
    initService();
    List<User> users = service.findUsersByInstitutionId(1);
    assertNotNull(users);
    assertTrue(users.isEmpty());
  }

  @Test
  void testFindUsersByInstitutionIdSuccessWithUsers() {
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(new Institution());
    when(userDAO.findUsersByInstitution(anyInt())).thenReturn(List.of(new User()));
    initService();
    List<User> users = service.findUsersByInstitutionId(1);
    assertNotNull(users);
    assertFalse(users.isEmpty());
  }

  @Test
  void testGetUsersByUserRole_SO() {
    User u = generateUser();
    u.setInstitutionId(1);
    LibraryCard lc = generateLibraryCard(u);
    u.setLibraryCards(List.of(lc));
    when(userDAO.getUsersFromInstitutionWithCards(anyInt())).thenReturn(List.of(u, new User()));
    initService();

    List<User> users = service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertNotNull(users);
    assertEquals(2, users.size());
    assertEquals(List.of(lc), users.get(0).getLibraryCards());
  }

  @Test
  void testGetUsersAsRoleSO_NoInstitution() {
    User u = generateUser();
    u.setInstitutionId(null);
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName());
    });
  }

  @Test
  void testGetUsersAsRoleAdmin() {
    User u1 = generateUser();
    User u2 = generateUser();
    User u3 = generateUser();
    List<User> returnedUsers = new ArrayList<>();
    returnedUsers.add(u1);
    if (!returnedUsers.contains(u2)) {
      returnedUsers.add(u2);
    }
    if (!returnedUsers.contains(u3)) {
      returnedUsers.add(u3);
    }
    LibraryCard lc = generateLibraryCard(u1);
    u1.setLibraryCards(List.of(lc));
    when(userDAO.findUsersWithLCsAndInstitution()).thenReturn(returnedUsers);
    initService();
    List<User> users = service.getUsersAsRole(u1, UserRoles.ADMIN.getRoleName());
    assertNotNull(users);
    assertEquals(returnedUsers.size(), users.size());
    assertEquals(List.of(lc), users.get(0).getLibraryCards());
    assertNull(users.get(1).getLibraryCards());
  }

  @Test
  void testGetUsersAsRoleInvalidRole() {
    User u1 = generateUser();
    initService();
    List<User> users = service.getUsersAsRole(u1, UserRoles.ADMIN.getRoleName());
    assertNotNull(users);
    assertEquals(0, users.size());
    assertEquals(Collections.emptyList(), users);
  }

  @Test
  void testGetUsersByDaaId() {
    User u1 = generateUser();
    int dacId = RandomUtils.nextInt(0, 50);
    Instant now = Instant.now();
    LibraryCard card = generateLibraryCard(u1);
    int daaId = daaDAO.createDaa(card.getUserId(), now, card.getUserId(), now, dacId);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    when(daaDAO.findById(any())).thenReturn(daa);
    when(userDAO.getUsersWithCardsByDaaId(any())).thenReturn(List.of(u1));
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId);
    initService();
    List<SimplifiedUser> users = service.getUsersByDaaId(daaId);
    assertNotNull(users);
    assertEquals(1, users.size());
    assertEquals(List.of(new SimplifiedUser(u1)), users);
  }

  @Test
  void testGetUsersByDaaIdMultipleUsers() {
    User u1 = generateUser();
    User u2 = generateUser();
    int dacId = RandomUtils.nextInt(0, 50);
    Instant now = Instant.now();
    LibraryCard card = generateLibraryCard(u1);
    LibraryCard card2 = generateLibraryCard(u2);
    int daaId = daaDAO.createDaa(card.getUserId(), now, card.getUserId(), now, dacId);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    when(daaDAO.findById(daaId)).thenReturn(daa);
    when(userDAO.getUsersWithCardsByDaaId(any())).thenReturn(List.of(u1, u2));
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId);
    libraryCardDAO.createLibraryCardDaaRelation(card2.getId(), daaId);
    initService();
    List<SimplifiedUser> users = service.getUsersByDaaId(daaId);
    assertNotNull(users);
    assertEquals(2, users.size());
    assertEquals(List.of(new SimplifiedUser(u1), new SimplifiedUser(u2)), users);
  }

  @Test
  void testGetUsersByDaaIdMultipleUsersMultipleDaas() {
    User u1 = generateUser();
    User u2 = generateUser();
    User u3 = generateUser();
    int dacId = RandomUtils.nextInt(0, 50);
    int dacId2 = RandomUtils.nextInt(0, 50);
    Instant now = Instant.now();
    LibraryCard card = generateLibraryCard(u1);
    LibraryCard card2 = generateLibraryCard(u2);
    LibraryCard card3 = generateLibraryCard(u3);
    int daaId = daaDAO.createDaa(card.getUserId(), now, card.getUserId(), now, dacId);
    int daaId2 = daaDAO.createDaa(card3.getUserId(), now, card3.getUserId(), now, dacId2);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa2.setDaaId(daaId2);
    when(daaDAO.findById(any())).thenReturn(daa, daa2);
    when(userDAO.getUsersWithCardsByDaaId(any())).thenReturn(List.of(u1, u2), List.of(u3));
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId);
    libraryCardDAO.createLibraryCardDaaRelation(card2.getId(), daaId);
    libraryCardDAO.createLibraryCardDaaRelation(card3.getId(), daaId2);
    initService();
    List<SimplifiedUser> users = service.getUsersByDaaId(daaId);
    assertNotNull(users);
    assertEquals(2, users.size());
    assertEquals(List.of(new SimplifiedUser(u1), new SimplifiedUser(u2)), users);

    List<SimplifiedUser> users2 = service.getUsersByDaaId(daaId2);
    assertNotNull(users2);
    assertEquals(1, users2.size());
    assertEquals(List.of(new SimplifiedUser(u3)), users2);
  }

  @Test
  void testGetUsersByDaaIdNoMatchingUsers() {
    User u1 = generateUser();
    int dacId = RandomUtils.nextInt(0, 50);
    Instant now = Instant.now();
    LibraryCard card = generateLibraryCard(u1);
    int daaId = daaDAO.createDaa(card.getUserId(), now, card.getUserId(), now, dacId);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    when(daaDAO.findById(any())).thenReturn(daa);
    initService();
    List<SimplifiedUser> users = service.getUsersByDaaId(daaId);
    assertNotNull(users);
    assertEquals(0, users.size());
    assertEquals(Collections.emptyList(), users);
  }

  @Test
  void testGetUsersByDaaIdNoMatchingDaa() {
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.getUsersByDaaId(RandomUtils.nextInt(10, 50));
    });
  }

  @Test
  void testFindUsersWithNoInstitution() {
    User user = generateUser();
    when(userDAO.getUsersWithNoInstitution()).thenReturn(List.of(user));
    initService();
    List<User> users = service.findUsersWithNoInstitution();
    assertNotNull(users);
    assertEquals(1, users.size());
    assertEquals(user.getUserId(), users.get(0).getUserId());
  }

  @Test
  void testFindUserWithPropertiesAsJsonObjectById() {
    User user = generateUser();
    UserStatusInfo info = new UserStatusInfo().setUserEmail(user.getEmail()).setEnabled(true)
        .setUserSubjectId("subjectId");
    AuthUser authUser = new AuthUser().setEmail(user.getEmail())
        .setAuthToken(RandomStringUtils.random(30, true, false)).setUserStatusInfo(info);
    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(List.of(new LibraryCard()));
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(anyInt(), any())).thenReturn(
        List.of(new UserProperty()));

    initService();
    JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser,
        user.getUserId());
    assertNotNull(userJson);
    assertTrue(userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
    assertTrue(
        userJson.get(UserService.RESEARCHER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
    assertTrue(userJson.get(UserService.USER_STATUS_INFO_FIELD).getAsJsonObject().isJsonObject());
  }

  @Test
  void testFindUserWithPropertiesAsJsonObjectByIdNonAuthUser() {
    User user = generateUser();
    UserStatusInfo info = new UserStatusInfo().setUserEmail(user.getEmail()).setEnabled(true)
        .setUserSubjectId("subjectId");
    AuthUser authUser = new AuthUser().setEmail("not the user's email address")
        .setAuthToken(RandomStringUtils.random(30, true, false)).setUserStatusInfo(info);
    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(List.of(new LibraryCard()));
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(anyInt(), any())).thenReturn(
        List.of(new UserProperty()));

    initService();
    JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser,
        user.getUserId());
    assertNotNull(userJson);
    assertTrue(userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
    assertTrue(
        userJson.get(UserService.RESEARCHER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
    assertNull(userJson.get(UserService.USER_STATUS_INFO_FIELD));
  }

  @Test
  void testFindOrCreateUser() throws Exception {
    User user = generateUser();
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail(user.getEmail());
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true)
        .setLdap(true);
    UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
    AuthUser authUser = new AuthUser().setEmail(user.getEmail())
        .setAuthToken(RandomStringUtils.random(30, true, false));

    when(userDAO.findUserByEmail(any())).thenReturn(user);
    when(samDAO.postRegistrationInfo(any())).thenReturn(status);
    initService();

    User existingUser = service.findOrCreateUser(authUser);
    assertEquals(existingUser, user);
  }

  @Test
  void testFindOrCreateUserNewUser() throws Exception {
    User user = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    user.setRoles(roles);
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail(user.getEmail());
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true)
        .setLdap(true);
    UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
    AuthUser authUser = new AuthUser().setName(user.getDisplayName()).setEmail(user.getEmail())
        .setAuthToken(RandomStringUtils.random(30, true, false));

    // mock findUserByEmail to throw the NFE on the first call (findOrCreateUser) and then return null (createUser)
    when(userDAO.findUserByEmail(authUser.getEmail())).thenThrow(new NotFoundException())
        .thenReturn(null);
    when(userDAO.insertUser(any(), any(), any())).thenReturn(user.getUserId());
    when(userDAO.findUserById(any())).thenReturn(user);
    when(samDAO.postRegistrationInfo(any())).thenReturn(status);
    initService();

    User newUser = service.findOrCreateUser(authUser);
    assertEquals(user.getEmail(), newUser.getEmail());
    verify(userRoleDAO, times(1)).insertUserRoles(any(), any());
    verify(libraryCardDAO, times(1)).findAllLibraryCardsByUserEmail(any());
    verify(userDAO, times(1)).insertUser(any(), any(), any());
  }

  @Test
  void insertUserRoleAndInstitution() {
    boolean encounteredException = false;
    Integer institutionId = 1;
    User testUser = generateUserWithoutInstitution();
    User returnUser = new User();
    returnUser.setUserId(testUser.getUserId());
    returnUser.setEmail(testUser.getEmail());
    returnUser.setDisplayName(testUser.getDisplayName());
    returnUser.setInstitutionId(1);
    UserRole role = UserRoles.Researcher();
    assertNotEquals(testUser.getInstitutionId(), returnUser.getInstitutionId());
    doNothing().when(userServiceDAO).insertRoleAndInstitutionTxn(any(), any(), any());
    when(userDAO.findUserById(anyInt())).thenReturn(returnUser);
    initService();
    try {
      service.insertRoleAndInstitutionForUser(role, institutionId, testUser.getUserId());
    } catch (Exception e) {
      encounteredException = true;
    }
    User fetchedUser = service.findUserById(testUser.getUserId());
    assertEquals(fetchedUser.getUserId(), testUser.getUserId());
    assertEquals(fetchedUser.getInstitutionId(), returnUser.getInstitutionId());
    assertFalse(encounteredException);
  }

  @Test
  void insertUserRoleAndInstitution_FailingTxn() {
    boolean encounteredException = false;
    Integer institutionId = 1;
    User testUser = generateUserWithoutInstitution();
    assertNull(testUser.getInstitutionId());
    UserRole role = UserRoles.Researcher();
    doThrow(new RuntimeException("txn error")).when(userServiceDAO)
        .insertRoleAndInstitutionTxn(any(), any(), any());
    initService();
    try {
      service.insertRoleAndInstitutionForUser(role, institutionId, testUser.getUserId());
    } catch (Exception e) {
      encounteredException = true;
    }
    assertTrue(encounteredException);
  }

  @Test
  void testFindUsersInJsonArray() {
    String json = "{users:[1,2,3]}";
    List<User> users = List.of(generateUser(), generateUser(), generateUser());
    when(userDAO.findUserById(anyInt())).thenReturn(users.get(0), users.get(1), users.get(2));
    initService();
    List<User> foundUsers = service.findUsersInJsonArray(json, "users");
    assertEquals(3, foundUsers.size());
  }

  @Test
  void testFindUsersInJsonArrayRemoveDuplicates() {
    String json = "{users:[1,1,2,3]}";
    List<User> users = List.of(generateUser(), generateUser(), generateUser());
    when(userDAO.findUserById(anyInt())).thenReturn(users.get(0), users.get(1), users.get(2));
    initService();
    List<User> foundUsers = service.findUsersInJsonArray(json, "users");
    assertEquals(3, foundUsers.size());
  }

  @Test
  void testFindUsersInJsonArrayEmptyArray() {
    String json = "{users:[]}";
    initService();
    List<User> foundUsers = service.findUsersInJsonArray(json, "users");
    assertTrue(foundUsers.isEmpty());
  }

  @Test
  void testFindUsersInJsonArrayInvalidJson() {
    // Missing closing bracket
    String json = "{users:[1,2,3}";
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.findUsersInJsonArray(json, "users");
    });
  }

  @Test
  void testFindUsersInJsonArrayInvalidKey() {
    String json = "{users:[1,2,3]}";
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.findUsersInJsonArray(json, "invalidKey");
    });
  }

  private User generateUserWithoutInstitution() {
    User u = generateUser();
    u.setInstitutionId(null);
    return u;
  }

  private User generateUser() {
    User u = new User();
    int i1 = RandomUtils.nextInt(10, 50);
    int i2 = RandomUtils.nextInt(10, 50);
    int i3 = RandomUtils.nextInt(5, 25);
    String email =
        RandomStringUtils.randomAlphabetic(i1) + "@" + RandomStringUtils.randomAlphabetic(i2) + "."
            + RandomStringUtils.randomAlphabetic(i3);
    String displayName =
        RandomStringUtils.randomAlphabetic(i1) + " " + RandomStringUtils.randomAlphabetic(i2);
    u.setEmail(email);
    u.setDisplayName(displayName);
    u.setUserId(RandomUtils.nextInt(1, 100));
    u.setInstitutionId(RandomUtils.nextInt(1, 100));
    return u;
  }

  private LibraryCard generateLibraryCard(String email) {
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setId(RandomUtils.nextInt(1, 10));
    libraryCard.setInstitutionId(RandomUtils.nextInt(1, 10));
    libraryCard.setUserEmail(email);
    libraryCard.setUserName(RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(1, 10)));
    return libraryCard;
  }

  private LibraryCard generateLibraryCard(User user) {
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setId(RandomUtils.nextInt(1, 10));
    libraryCard.setUserId(user.getUserId());
    libraryCard.setInstitutionId(RandomUtils.nextInt(1, 10));
    return libraryCard;
  }

  private UserRole generateRole(int roleId) {
    UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
    assert rolesEnum != null;
    return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
  }

}
