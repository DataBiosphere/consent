package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.enumeration.UserFields.ERA_EXPIRATION_DATE;
import static org.broadinstitute.consent.http.enumeration.UserFields.ERA_STATUS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.LibraryCardRequiredException;
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
import org.broadinstitute.consent.http.service.dao.DraftServiceDAO;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.jdbi.v3.core.transaction.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest extends AbstractTestHelper {

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
  private DraftServiceDAO draftServiceDAO;

  @Mock
  private InstitutionService institutionService;
  @Mock
  private DACAutomationRuleDAO ruleDAO;


  private UserService service;

  @BeforeEach
  void initService() {
    service = new UserService(userDAO, userPropertyDAO, userRoleDAO, voteDAO, institutionDAO,
        libraryCardDAO, acknowledgementDAO, fileStorageObjectDAO, samDAO, userServiceDAO, daaDAO,
        draftServiceDAO, institutionService, ruleDAO);
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

    UserUpdateFields fields = new UserUpdateFields();
    // We're modifying this user to have an SO role. This should leave in place
    // both the Researcher and Chairperson roles, but remove the Admin role.
    fields.setUserRoleIds(List.of(so.getRoleId()));
    fields.setDisplayName(randomAlphabetic(10));
    fields.setInstitutionId(1);
    fields.setEmailPreference(true);
    fields.setEraCommonsId(randomAlphabetic(10));
    fields.setDaaAcceptance(true);
    assertEquals(1, fields.buildUserProperties(user.getUserId()).size());
    service.updateUserFieldsById(fields, user.getUserId());

    // We added 3 user property values, we should have props for them:
    verify(userDAO, times(1)).updateDisplayName(any(), any());
    verify(userDAO, times(1)).updateEmailPreference(any(), any());
    verify(userDAO, times(1)).updateEraCommonsId(any(), any());
    verify(userPropertyDAO, times(1)).insertAll(any());
    // Verify role additions/deletions.
    verify(userRoleDAO, times(1)).insertUserRoles(List.of(so), 1);
    verify(userRoleDAO, times(1)).removeUserRoles(1, List.of(admin.getRoleId()));
  }

  @Test
  void createUserTest() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    when(userDAO.findUserByEmail(u.getEmail())).thenReturn(null);
    when(libraryCardDAO.findLibraryCardByUserEmail(u.getEmail())).thenReturn(null);
    try {
      service.createUser(u);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void createUserWithLibraryCardTest() {
    User u = generateUser();
    LibraryCard lc = generateLibraryCard(u.getEmail());
    lc.setUserName(u.getDisplayName());
    Institution institution = new Institution();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    when(libraryCardDAO.findLibraryCardByUserEmail(u.getEmail())).thenReturn(lc);
    when(institutionService.findInstitutionForEmail(u.getEmail())).thenReturn(institution);

    service.createUser(u);

    verify(libraryCardDAO).updateLibraryCardById(
        eq(lc.getId()),
        eq(u.getUserId()),
        eq(lc.getUserName()),
        eq(lc.getUserEmail()),
        eq(u.getUserId()),
        any());
  }

  @Test
  void testCreateUserDuplicateEmail() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    when(userDAO.findUserByEmail(any())).thenReturn(u);
    assertThrows(BadRequestException.class, () -> service.createUser(u));
  }

  @Test
  void testCreateUserNoDisplayName() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    u.setDisplayName(null);
    assertThrows(BadRequestException.class, () -> service.createUser(u));
  }

  @Test
  void testHasValidERACommonsCredentials() {
    User u = generateUser();
    u.setEraCommonsId(randomAlphabetic(10));
    LibraryCard lc = generateLibraryCard(u.getEmail());
    u.setLibraryCard(lc);
    UserProperty eraStatus = new UserProperty(1, u.getUserId(), ERA_STATUS.getValue(), "true");
    //standard practice is that these expire in 30 days.
    Timestamp eraExpirationTime = new Timestamp(
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
    UserProperty eraExpirationDate = new UserProperty(2, u.getUserId(),
        ERA_EXPIRATION_DATE.getValue(), Long.toString(eraExpirationTime.getTime()));
    List<UserProperty> userProperties = new ArrayList<>();
    userProperties.add(eraStatus);
    userProperties.add(eraExpirationDate);
    u.setProperties(userProperties);
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(u.getUserId(),
        UserFields.getValues())).thenReturn(u.getProperties());
    assertDoesNotThrow(() -> service.validateActiveERACredentials(u));
  }

  @Test
  void testValidateERACommonsCredentialsMissingLibraryCards() {
    User u = generateUser();
    UserProperty eraStatus = new UserProperty(1, u.getUserId(), ERA_STATUS.getValue(), "true");
    //standard practice is that these expire in 30 days.
    Timestamp eraExpirationTime = new Timestamp(
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
    UserProperty eraExpirationDate = new UserProperty(2, u.getUserId(),
        ERA_EXPIRATION_DATE.getValue(), Long.toString(eraExpirationTime.getTime()));
    List<UserProperty> userProperties = new ArrayList<>();
    userProperties.add(eraStatus);
    userProperties.add(eraExpirationDate);
    u.setProperties(userProperties);
    assertThrows(LibraryCardRequiredException.class,
        () -> service.validateActiveERACredentials(u));
  }

  @Test
  void testValidateERACommonsCredentialsMissingERACommonsId() {
    User u = generateUser();
    u.setEraCommonsId(null);
    LibraryCard lc = generateLibraryCard(u.getEmail());
    u.setLibraryCard(lc);
    UserProperty eraStatus = new UserProperty(1, u.getUserId(), ERA_STATUS.getValue(), "true");
    //standard practice is that these expire in 30 days.
    Timestamp eraExpirationTime = new Timestamp(
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
    UserProperty eraExpirationDate = new UserProperty(2, u.getUserId(),
        ERA_EXPIRATION_DATE.getValue(), Long.toString(eraExpirationTime.getTime()));
    List<UserProperty> userProperties = new ArrayList<>();
    userProperties.add(eraStatus);
    userProperties.add(eraExpirationDate);
    u.setProperties(userProperties);
    assertThrows(BadRequestException.class,
        () -> service.validateActiveERACredentials(u));
  }

  @Test
  void testValidateRACommonsCredentialsMissingERAStatusShouldFail() {
    User u = generateUser();
    LibraryCard lc = generateLibraryCard(u.getEmail());
    u.setLibraryCard(lc);
    //standard practice is that these expire in 30 days.
    Timestamp eraExpirationTime = new Timestamp(
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
    UserProperty eraExpirationDate = new UserProperty(2, u.getUserId(),
        ERA_EXPIRATION_DATE.getValue(), Long.toString(eraExpirationTime.getTime()));
    List<UserProperty> userProperties = new ArrayList<>();
    userProperties.add(eraExpirationDate);
    u.setProperties(userProperties);
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(u.getUserId(),
        UserFields.getValues())).thenReturn(u.getProperties());
    assertThrows(BadRequestException.class,
        () -> service.validateActiveERACredentials(u));
  }

  @Test
  void testValidateERACommonsCredentialsMissingERAStatusAndExpirationShouldFail() {
    User u = generateUser();
    LibraryCard lc = generateLibraryCard(u.getEmail());
    u.setLibraryCard(lc);
    List<UserProperty> userProperties = new ArrayList<>();
    u.setProperties(userProperties);
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(u.getUserId(),
        UserFields.getValues())).thenReturn(u.getProperties());
    assertThrows(BadRequestException.class,
        () -> service.validateActiveERACredentials(u));
  }

  @Test
  void testValidateERACommonsCredentialsWithExpiredERAExpirationDateShouldFail() {
    User u = generateUser();
    LibraryCard lc = generateLibraryCard(u.getEmail());
    u.setLibraryCard(lc);
    UserProperty eraStatus = new UserProperty(1, u.getUserId(), ERA_STATUS.getValue(), "true");
    // set expiration date to 30 days ago!
    Timestamp eraExpirationTime = new Timestamp(
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
    UserProperty eraExpirationDate = new UserProperty(2, u.getUserId(),
        ERA_EXPIRATION_DATE.getValue(), Long.toString(eraExpirationTime.getTime()));
    List<UserProperty> userProperties = new ArrayList<>();
    userProperties.add(eraStatus);
    userProperties.add(eraExpirationDate);
    u.setProperties(userProperties);
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(u.getUserId(),
        UserFields.getValues())).thenReturn(u.getProperties());
    assertThrows(BadRequestException.class,
        () -> service.validateActiveERACredentials(u));
  }

  @Test
  void testCreateUserNoRoles() {
    User u = generateUser();
    assertTrue(CollectionUtils.isEmpty(u.getRoles()));
    int userId = 123;
    when(userDAO.insertUser(eq(u.getEmail()), eq(u.getDisplayName()), eq(u.getInstitutionId()),
        any())).thenReturn(userId);
    service.createUser(u);
    verify(userRoleDAO).insertUserRoles(List.of(UserRoles.Researcher()), userId);
  }

  @Test
  void testCreateUserInvalidRoleCase1() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.CHAIRPERSON.getRoleId()));
    u.setRoles(roles);
    assertThrows(BadRequestException.class, () -> service.createUser(u));
  }

  @Test
  void testCreateUserInvalidRoleCase2() {
    User u = generateUser();
    List<UserRole> roles = List.of(generateRole(UserRoles.MEMBER.getRoleId()));
    u.setRoles(roles);
    assertThrows(BadRequestException.class, () -> service.createUser(u));
  }

  @Test
  void testCreateUserNoEmail() {
    User u = generateUser();
    u.setEmail(null);
    assertThrows(BadRequestException.class, () -> service.createUser(u));
  }

  @Test
  void testFindUserById_HasLibraryCards() {
    User u = generateUser();
    LibraryCard one = generateLibraryCard(u);
    u.setLibraryCard(one);
    when(userDAO.findUserById(any())).thenReturn(u);

    User user = service.findUserById(u.getUserId());
    assertNotNull(user);
    assertNotNull(user.getLibraryCard());
    assertEquals(one, user.getLibraryCard());
  }

  @Test
  void testFindUserByIdNoRoles() {
    User u = generateUser();
    when(userDAO.findUserById(any())).thenReturn(u);

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

    assertThrows(NotFoundException.class, () -> service.findUserById(u.getUserId()));
  }

  @Test
  void testFindUserByEmailNoRoles() {
    User u = generateUser();
    when(userDAO.findUserByEmail(any())).thenReturn(u);

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

    assertThrows(NotFoundException.class, () -> service.findUserByEmail(u.getEmail()));
  }

  @Test
  void testDeleteUser() {
    User u = generateUser();
    doNothing().when(userPropertyDAO).deleteAllPropertiesByUser(any());
    when(userDAO.findUserByEmail(any())).thenReturn(u);

    try {
      service.deleteUserByEmail(randomAlphabetic(10), randomInt(1,100));
      verify(draftServiceDAO).deleteDraftsByUser(u);
      verify(ruleDAO, atLeastOnce()).auditedDeleteAllDACRuleSettingForUser(anyInt(), anyInt());
    } catch (Exception e) {
      fail("Should not fail: " + e.getMessage());
    }
  }

  @Test
  void testDeleteUserFailure() {
    when(userDAO.findUserByEmail(any())).thenThrow(new NotFoundException());
    assertThrows(NotFoundException.class, () -> service.deleteUserByEmail(randomAlphabetic(10), randomInt(1,100)));
  }

  @Test
  void testFindSOsByInstitutionId() {
    User u = generateUser();
    Integer institutionId = u.getInstitutionId();
    when(userDAO.getSOsByInstitution(any())).thenReturn(List.of(u, u, u));
    List<SimplifiedUser> users = service.findSOsByInstitutionId(institutionId);
    assertEquals(3, users.size());
    assertEquals(u.getDisplayName(), users.get(0).getDisplayName());
    assertEquals(u.getEmail(), users.get(0).getEmail());
  }

  @Test
  void testFindSOsByInstitutionId_NullId() {
    List<SimplifiedUser> users = service.findSOsByInstitutionId(null);
    assertEquals(0, users.size());
  }

  @Test
  void testFindUsersByInstitutionIdNullId() {
    assertThrows(IllegalArgumentException.class, () -> service.findUsersByInstitutionId(null));
  }

  @Test
  void testFindUsersByInstitutionIdNullInstitution() {
    doThrow(new NotFoundException()).when(institutionDAO).findInstitutionById(anyInt());
    assertThrows(NotFoundException.class, () -> service.findUsersByInstitutionId(1));
  }

  @Test
  void testFindUsersByInstitutionIdSuccess() {
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(new Institution());
    List<User> users = service.findUsersByInstitutionId(1);
    assertNotNull(users);
    assertTrue(users.isEmpty());
  }

  @Test
  void testFindUsersByInstitutionIdSuccessWithUsers() {
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(new Institution());
    when(userDAO.findUsersByInstitution(anyInt())).thenReturn(List.of(new User()));
    List<User> users = service.findUsersByInstitutionId(1);
    assertNotNull(users);
    assertFalse(users.isEmpty());
  }

  @Test
  void testGetUsersByUserRole_SO() {
    User u = generateUser();
    u.setInstitutionId(1);
    LibraryCard lc = generateLibraryCard(u);
    u.setLibraryCard(lc);
    when(userDAO.getUsersFromInstitutionWithCards(anyInt())).thenReturn(List.of(u, new User()));

    List<User> users = service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertNotNull(users);
    assertEquals(2, users.size());
    assertSame(lc, users.get(0).getLibraryCard());
  }

  @Test
  void testGetUsersAsRoleSO_NoInstitution() {
    User u = generateUser();
    u.setInstitutionId(null);
    assertThrows(NotFoundException.class,
        () -> service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName()));
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
    u1.setLibraryCard(lc);
    when(userDAO.findUsersWithLCsAndInstitution()).thenReturn(returnedUsers);
    List<User> users = service.getUsersAsRole(u1, UserRoles.ADMIN.getRoleName());
    assertNotNull(users);
    assertEquals(returnedUsers.size(), users.size());
    assertEquals(lc, users.get(0).getLibraryCard());
    assertNull(users.get(1).getLibraryCard());
  }

  @Test
  void testGetUsersAsRoleInvalidRole() {
    User u1 = generateUser();
    List<User> users = service.getUsersAsRole(u1, UserRoles.ADMIN.getRoleName());
    assertNotNull(users);
    assertEquals(0, users.size());
    assertEquals(Collections.emptyList(), users);
  }

  @Test
  void testGetUsersByDaaId() {
    User u1 = generateUser();
    int dacId = randomInt(0, 50);
    Instant now = Instant.now();
    LibraryCard card = generateLibraryCard(u1);
    int daaId = daaDAO.createDaa(card.getUserId(), now, card.getUserId(), now, dacId);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    when(daaDAO.findById(any())).thenReturn(daa);
    when(userDAO.getUsersWithCardsByDaaId(any())).thenReturn(List.of(u1));
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId);
    List<SimplifiedUser> users = service.getUsersByDaaId(daaId);
    assertNotNull(users);
    assertEquals(1, users.size());
    assertEquals(List.of(new SimplifiedUser(u1)), users);
  }

  @Test
  void testGetUsersByDaaIdMultipleUsers() {
    User u1 = generateUser();
    User u2 = generateUser();
    int dacId = randomInt(0, 50);
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
    int dacId = randomInt(0, 50);
    int dacId2 = randomInt(0, 50);
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
    int dacId = randomInt(0, 50);
    Instant now = Instant.now();
    LibraryCard card = generateLibraryCard(u1);
    int daaId = daaDAO.createDaa(card.getUserId(), now, card.getUserId(), now, dacId);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    when(daaDAO.findById(any())).thenReturn(daa);
    List<SimplifiedUser> users = service.getUsersByDaaId(daaId);
    assertNotNull(users);
    assertEquals(0, users.size());
    assertEquals(Collections.emptyList(), users);
  }

  @Test
  void testGetUsersByDaaIdNoMatchingDaa() {
    assertThrows(NotFoundException.class, () -> service.getUsersByDaaId(randomInt(10, 50)));
  }

  @Test
  void testFindUsersWithNoInstitution() {
    User user = generateUser();
    when(userDAO.getUsersWithNoInstitution()).thenReturn(List.of(user));
    List<User> users = service.findUsersWithNoInstitution();
    assertNotNull(users);
    assertEquals(1, users.size());
    assertEquals(user.getUserId(), users.get(0).getUserId());
  }

  @Test
  void testFindUserWithPropertiesAsJsonObjectById() {
    User user = generateUser();
    user.setLibraryCard(new LibraryCard());
    UserStatusInfo info = new UserStatusInfo().setUserEmail(user.getEmail()).setEnabled(true)
        .setUserSubjectId("subjectId");
    AuthUser authUser = new AuthUser().setEmail(user.getEmail())
        .setAuthToken(randomAlphabetic(30)).setUserStatusInfo(info);
    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(anyInt(), any())).thenReturn(
        List.of(new UserProperty()));

    JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser,
        user.getUserId());
    assertNotNull(userJson);
    assertTrue(userJson.get(UserService.LIBRARY_CARD_FIELD).getAsJsonObject().isJsonObject());
    assertTrue(userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
    assertTrue(
        userJson.get(UserService.USER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
    assertTrue(userJson.get(UserService.USER_STATUS_INFO_FIELD).getAsJsonObject().isJsonObject());
  }

  @Test
  void testFindUserWithPropertiesAsJsonObjectByIdNonAuthUser() {
    User user = generateUser();
    user.setLibraryCard(new LibraryCard());
    UserStatusInfo info = new UserStatusInfo().setUserEmail(user.getEmail()).setEnabled(true)
        .setUserSubjectId("subjectId");
    AuthUser authUser = new AuthUser().setEmail("not the user's email address")
        .setAuthToken(randomAlphabetic(30)).setUserStatusInfo(info);
    when(userDAO.findUserById(anyInt())).thenReturn(user);
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(anyInt(), any())).thenReturn(
        List.of(new UserProperty()));

    JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser,
        user.getUserId());
    assertNotNull(userJson);
    assertTrue(userJson.get(UserService.LIBRARY_CARD_FIELD).getAsJsonObject().isJsonObject());
    assertTrue(userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
    assertTrue(
        userJson.get(UserService.USER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
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
        .setAuthToken(randomAlphabetic(30));

    when(userDAO.findUserByEmail(any())).thenReturn(user);
    when(samDAO.postRegistrationInfo(any())).thenReturn(status);

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
        .setAuthToken(randomAlphabetic(30));
    Institution institution = new Institution();

    // mock findUserByEmail to throw the NFE on the first call (findOrCreateUser)
    when(userDAO.findUserByEmail(authUser.getEmail())).thenThrow(new NotFoundException())
        .thenReturn(null);
    when(
        userDAO.insertUser(eq(authUser.getEmail()), eq(authUser.getName()), eq(institution.getId()),
            any())).thenReturn(user.getUserId());
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    when(samDAO.postRegistrationInfo(authUser)).thenReturn(status);
    when(institutionService.findInstitutionForEmail(user.getEmail())).thenReturn(institution);

    User newUser = service.findOrCreateUser(authUser);
    assertEquals(user.getEmail(), newUser.getEmail());
    verify(userRoleDAO).insertUserRoles(any(), any());
    verify(libraryCardDAO).findLibraryCardByUserEmail(any());
  }

  @Test
  void insertUserRoleAndInstitution() {
    Institution institution = new Institution();
    institution.setId(1);
    User testUser = generateUserWithoutInstitution();
    Integer testUserId = testUser.getUserId();
    UserRole role = UserRoles.Researcher();
    when(institutionService.findInstitutionForEmail(testUser.getEmail())).thenReturn(institution);
    service.insertRoleAndInstitutionForUser(role, testUser);
    verify(userServiceDAO).insertRoleAndInstitutionTxn(role, institution.getId(), testUserId);
  }

  @Test
  void insertUserRoleAndInstitution_roleOnly() {
    User testUser = generateUser();
    UserRole role = UserRoles.Researcher();
    service.insertRoleAndInstitutionForUser(role, testUser);
    verifyNoInteractions(institutionService);
    verifyNoInteractions(userServiceDAO);
    verify(userRoleDAO).insertSingleUserRole(role.getRoleId(), testUser.getUserId());
  }

  @Test
  void insertServiceAccountUserRole() {
    User testUser = generateUser();
    UserRole role = UserRoles.ServiceAccount();
    service.insertRoleAndInstitutionForUser(role, testUser);
    verifyNoInteractions(institutionService);
    verifyNoInteractions(userServiceDAO);
    verify(userRoleDAO).insertSingleUserRole(role.getRoleId(), testUser.getUserId());
  }

  @Test
  void insertUserRoleAndInstitution_FailingTxn() {
    Institution institution = new Institution();
    institution.setId(1);
    User testUser = generateUserWithoutInstitution();
    assertNull(testUser.getInstitutionId());
    UserRole role = UserRoles.Researcher();
    when(institutionService.findInstitutionForEmail(testUser.getEmail())).thenReturn(institution);
    doThrow(new TransactionException("txn error")).when(userServiceDAO)
        .insertRoleAndInstitutionTxn(role, institution.getId(), testUser.getUserId());
    assertThrows(TransactionException.class,
        () -> service.insertRoleAndInstitutionForUser(role, testUser));
  }

  @Test
  void insertUserRoleAndInstitution_FailingInstitution() {
    User testUser = generateUserWithoutInstitution();
    UserRole role = UserRoles.Researcher();
    assertThrows(BadRequestException.class,
        () -> service.insertRoleAndInstitutionForUser(role, testUser));
  }

  @Test
  void testFindUsersInJsonArray() {
    String json = "{users:[1,2,3]}";
    List<User> users = List.of(generateUser(), generateUser(), generateUser());
    when(userDAO.findUserById(anyInt())).thenReturn(users.get(0), users.get(1), users.get(2));
    List<User> foundUsers = service.findUsersInJsonArray(json, "users");
    assertEquals(3, foundUsers.size());
  }

  @Test
  void testFindUsersInJsonArrayRemoveDuplicates() {
    String json = "{users:[1,1,2,3]}";
    List<User> users = List.of(generateUser(), generateUser(), generateUser());
    when(userDAO.findUserById(anyInt())).thenReturn(users.get(0), users.get(1), users.get(2));
    List<User> foundUsers = service.findUsersInJsonArray(json, "users");
    assertEquals(3, foundUsers.size());
  }

  @Test
  void testFindUsersInJsonArrayEmptyArray() {
    String json = "{users:[]}";
    List<User> foundUsers = service.findUsersInJsonArray(json, "users");
    assertTrue(foundUsers.isEmpty());
  }

  @Test
  void testFindUsersInJsonArrayInvalidJson() {
    // Missing closing bracket
    String json = "{users:[1,2,3}";
    assertThrows(BadRequestException.class, () -> service.findUsersInJsonArray(json, "users"));
  }

  @Test
  void testFindUsersInJsonArrayInvalidKey() {
    String json = "{users:[1,2,3]}";
    assertThrows(BadRequestException.class, () -> service.findUsersInJsonArray(json, "invalidKey"));
  }

  @Test
  void hasLibraryCard() {
    User testUser = generateUser();
    testUser.setLibraryCard(new LibraryCard());
    assertTrue(service.hasLibraryCard(testUser));
  }

  @Test
  void hasLibraryCard_NoLibraryCard() {
    User testUser = generateUser();
    assertFalse(service.hasLibraryCard(testUser));
  }

  @Test
  void hasMatchingInstitutionInDatabase() {
    Institution institution = new Institution();
    institution.setId(1);
    assertTrue(service.hasMatchingInstitutionInDatabase(institution, institution));
  }

  @Test
  void hasMatchingInstitutionInDatabase_NoInstitutionInDB() {
    Institution institution = new Institution();
    institution.setId(1);
    assertFalse(service.hasMatchingInstitutionInDatabase(null, institution));
  }

  @Test
  void hasMatchingInstitutionInDatabase_NoInstitutionEmailMap() {
    Institution institution = new Institution();
    institution.setId(1);
    assertFalse(service.hasMatchingInstitutionInDatabase(institution, null));
  }

  @Test
  void handleUserWithInstitutionInMap() {
    User testUser = generateUser();
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitutionId(1);
    assertFalse(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
  }

  @Test
  void handleUserWithInstitutionInMap_DifferentInDatabase() {
    User testUser = generateUser();
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitutionId(2);
    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userDAO).updateInstitutionId(testUser.getUserId(), institutionFromEmail.getId());
  }

  @Test
  void handleUserWithInstitutionInMap_DifferentInDatabaseWithLibraryCard() {
    User testUser = generateUser();
    User signingOfficial = generateUser();
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    Institution institutionFromDatabase = new Institution();
    institutionFromDatabase.setId(2);
    testUser.setInstitutionId(institutionFromDatabase.getId());

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionService.findInstitutionForEmail(signingOfficial.getEmail())).thenReturn(institutionFromDatabase);

    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userServiceDAO).updateInstitutionAndClearLibraryCardForUser(testUser.getUserId(), institutionFromEmail.getId());
  }

  @Test
  void handleUserWithInstitutionInMap_DifferentInDatabaseWithLibraryCard_SO_NFE() {
    User testUser = generateUser();
    User signingOfficial = generateUser();
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    Institution institutionFromDatabase = new Institution();
    institutionFromDatabase.setId(2);

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(null);

    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userServiceDAO).updateInstitutionAndClearLibraryCardForUser(testUser.getUserId(), institutionFromEmail.getId());
  }

  @Test
  void handleUserWithInstitutionInMap_SameInDatabaseWithLC() {
    User testUser = generateUser();
    User signingOfficial = generateUser();
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    testUser.setInstitutionId(1);


    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionService.findInstitutionForEmail(signingOfficial.getEmail())).thenReturn(institutionFromEmail);

    assertFalse(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
  }


  @Test
  void handleUserWithInstitutionInMap_SameInDatabaseWithLCFromDifferentOrg() {
    User testUser = generateUser();
    User signingOfficial = generateUser();
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    testUser.setInstitutionId(1);


    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionService.findInstitutionForEmail(signingOfficial.getEmail())).thenReturn(null);

    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userDAO, times(0)).updateInstitutionId(any(), any());
    verify(libraryCardDAO).deleteAllLibraryCardsByUser(testUser.getUserId());
  }

  @Test
  void needsLibraryCardRemovedForUser() {
    User testUser = generateUser();
    Institution institution = new Institution();
    assertFalse(service.needsLibraryCardRemovedForUser(testUser, institution.getId()));
  }

  @Test
  void needsLibraryCardRemovedForUser_SO_NFE() {
    User testUser = generateUser();
    Institution institution = new Institution();
    institution.setId(testUser.getInstitutionId());
    User signingOfficial = generateUser();
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(null);
    assertTrue(service.needsLibraryCardRemovedForUser(testUser, institution.getId()));
  }

  @Test
  void needsLibraryCardRemovedForUser_SO_DifferentInstitution() {
    User testUser = generateUser();
    User signingOfficial = generateUser();
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);

    Institution soInstitution = new Institution();
    soInstitution.setId(2);

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionService.findInstitutionForEmail(signingOfficial.getEmail())).thenReturn(soInstitution);
    assertTrue(service.needsLibraryCardRemovedForUser(testUser, institutionFromEmail.getId()));
  }

  public static Stream<Arguments> testEnforceInstitutionAndLibraryCardVariations() {
    User testUser = generateUser();
    Institution institution1 = new Institution();
    institution1.setId(1);
    Institution institution2 = new Institution();
    institution2.setId(2);
    LibraryCard libraryCards1 = new LibraryCard();
    LibraryCard libraryCard2 = new LibraryCard();
    libraryCard2.setCreateUserId(1);
    return Stream.of(
        Arguments.of(institution1, testUser, libraryCards1, true),
        Arguments.of(institution2, testUser, libraryCards1, true),
        Arguments.of(institution1, testUser, libraryCard2, false),
        Arguments.of(institution1, testUser, null, false),
        Arguments.of(null, testUser, libraryCards1, true),
        Arguments.of(null, testUser, null, false)
    );
  }

  @ParameterizedTest
  @MethodSource
  void testEnforceInstitutionAndLibraryCardVariations(
      Institution institutionFromMap, User testUser, LibraryCard card, boolean expectsUserMod) {
    testUser.setLibraryCard(card);
    User alteredUser = new User();
    alteredUser.setEmail(testUser.getEmail());
    if (institutionFromMap != null) {
      when(institutionService.findInstitutionIdForEmail(testUser.getEmail()))
          .thenReturn(institutionFromMap.getId());
    } else {
      when(institutionService.findInstitutionIdForEmail(testUser.getEmail()))
          .thenReturn(null);
    }
    if (expectsUserMod) {
      when(userDAO.findUserByEmail(testUser.getEmail())).thenReturn(testUser, alteredUser);
      validateAlteredUserIsReturned(
          testUser, service.enforceInstitutionAndLibraryCardRules(testUser.getEmail()));
    } else {
      when(userDAO.findUserByEmail(testUser.getEmail())).thenReturn(testUser);
      validateUserIsUnmodified(
          testUser, service.enforceInstitutionAndLibraryCardRules(testUser.getEmail()));
    }
  }

  private void validateUserIsUnmodified(User left, User right) {
    assertEquals(left.getEmail(), right.getEmail());
    assertEquals(left.getInstitutionId(), right.getInstitutionId());
    assertEquals(left.getLibraryCard(), right.getLibraryCard());
    assertEquals(left.getInstitutionId(), right.getInstitutionId());
  }

  private void validateAlteredUserIsReturned(User left, User right) {
    assertEquals(left.getEmail(), right.getEmail());
    assertNotEquals(left.getInstitutionId(), right.getInstitutionId());
  }

  private User generateUserWithoutInstitution() {
    User u = generateUser();
    u.setInstitutionId(null);
    return u;
  }

  private static User generateUser() {
    User u = new User();
    int i1 = randomInt(10, 50);
    int i2 = randomInt(10, 50);
    int i3 = randomInt(5, 25);
    String email = randomAlphabetic(i1) + "@" + randomAlphabetic(i2) + "." + randomAlphabetic(i3);
    String displayName = randomAlphabetic(i1) + " " + randomAlphabetic(i2);
    u.setEmail(email);
    u.setEraCommonsId(email);
    u.setDisplayName(displayName);
    u.setUserId(randomInt(1, 100));
    u.setInstitutionId(randomInt(1, 100));
    return u;
  }

  private LibraryCard generateLibraryCard(String email) {
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setId(randomInt(1, 10));
    libraryCard.setUserEmail(email);
    libraryCard.setUserName(randomAlphabetic(randomInt(1, 10)));
    return libraryCard;
  }

  private LibraryCard generateLibraryCard(User user) {
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setId(randomInt(1, 10));
    libraryCard.setUserId(user.getUserId());
    return libraryCard;
  }

  private UserRole generateRole(int roleId) {
    UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
    assert rolesEnum != null;
    return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
  }

}
