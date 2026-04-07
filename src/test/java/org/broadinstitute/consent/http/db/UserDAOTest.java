package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDAOTest extends DAOTestHelper {

  @Test
  void testFindUserById() {
    User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
    assertNotNull(user);
    assertFalse(user.getRoles().isEmpty());

    userRoleDAO.insertSingleUserRole(UserRoles.ADMIN.getRoleId(), user.getUserId());
    userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), user.getUserId());

    User user2 = userDAO.findUserById(user.getUserId());
    assertNotNull(user2);
    assertEquals(user.getEmail(), user2.getEmail());

    // Assert roles are fetched correctly
    assertTrue(
        user2.getRoles().stream()
            .anyMatch(r -> r.getRoleId().equals(UserRoles.ALUMNI.getRoleId())));
    assertTrue(
        user2.getRoles().stream().anyMatch(r -> r.getRoleId().equals(UserRoles.ADMIN.getRoleId())));
    assertTrue(
        user2.getRoles().stream()
            .anyMatch(r -> r.getRoleId().equals(UserRoles.RESEARCHER.getRoleId())));

    // assert institution base data is present if available
    User user3 = createUserWithInstitution();
    User queriedUser3 = userDAO.findUserById(user3.getUserId());
    assert (queriedUser3.getUserId()).equals(user3.getUserId());
    assertNotNull(queriedUser3.getInstitutionId());
    assert (queriedUser3.getInstitution().getId()).equals(user3.getInstitution().getId());
  }

  @Test
  void testFindUserWithPropertiesById() {
    User user = createUserWithInstitution();
    int lcId =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    int dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), new Date());
    int daaId =
        daaDAO.createDaa(user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), dacId);
    daaDAO.createDacDaaRelation(dacId, daaId, user.getUserId());
    libraryCardDAO.createLibraryCardDaaRelation(lcId, daaId);
    int dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), new Date());
    int daaId2 =
        daaDAO.createDaa(user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), dacId2);
    libraryCardDAO.createLibraryCardDaaRelation(lcId, daaId2);
    UserProperty eraExpProp = new UserProperty();
    eraExpProp.setPropertyKey(UserFields.ERA_EXPIRATION_DATE.getValue());
    eraExpProp.setPropertyValue(Instant.now().toString());
    eraExpProp.setUserId(user.getUserId());
    UserProperty eraAuthProp = new UserProperty();
    eraAuthProp.setPropertyKey(UserFields.ERA_STATUS.getValue());
    eraAuthProp.setPropertyValue("true");
    eraAuthProp.setUserId(user.getUserId());
    userPropertyDAO.insertAll(List.of(eraExpProp, eraAuthProp));

    User foundUser = userDAO.findUserWithPropertiesById(user.getUserId(), UserFields.getValues());
    assertNotNull(foundUser);
    assertFalse(foundUser.getRoles().isEmpty());
    assertEquals(lcId, foundUser.getLibraryCard().getId());
    assertEquals(user.getInstitutionId(), foundUser.getInstitutionId());
    assertFalse(foundUser.getProperties().isEmpty());
    assertTrue(
        foundUser.getProperties().stream()
            .anyMatch(
                p ->
                    p.getPropertyKey().equals(UserFields.ERA_EXPIRATION_DATE.getValue())
                        && p.getPropertyValue().equals(eraExpProp.getPropertyValue())));
    assertTrue(
        foundUser.getProperties().stream()
            .anyMatch(
                p ->
                    p.getPropertyKey().equals(UserFields.ERA_STATUS.getValue())
                        && p.getPropertyValue().equals(eraAuthProp.getPropertyValue())));
    assertTrue(foundUser.getLibraryCard().getDaaIds().contains(daaId));
    assertTrue(foundUser.getLibraryCard().getDaaIds().contains(daaId2));
  }

  @Test
  void testFindUserByIdWithLibraryCard() {
    LibraryCard libraryCard = createLibraryCard();
    User user = userDAO.findUserById(libraryCard.getUserId());
    assertNotNull(user);
    assertEquals(libraryCard, user.getLibraryCard());
  }

  @Test
  void testFindUserByEmailWithLibraryCard() {
    LibraryCard libraryCard = createLibraryCard();
    User user = userDAO.findUserByEmail(libraryCard.getUserEmail());
    assertNotNull(user);
    assertEquals(libraryCard, user.getLibraryCard());
  }

  @Test
  void testFindUsers_withIdCollection() {
    User user = createUser();
    Collection<User> users = userDAO.findUsers(Collections.singletonList(user.getUserId()));
    assertNotNull(users);
    assertFalse(users.isEmpty());
    assertEquals(1, users.size());
  }

  @Test
  void testDescribeUsersByRole() {
    createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    createUserWithRole(UserRoles.MEMBER.getRoleId());

    List<User> members = userDAO.describeUsersByRole(UserRoles.MEMBER.getRoleName());
    assertFalse(members.isEmpty());

    List<User> chairs = userDAO.describeUsersByRole(UserRoles.CHAIRPERSON.getRoleName());
    assertFalse(chairs.isEmpty());

    // Only case where we don't set up users by default.
    List<User> alumni = userDAO.describeUsersByRole(UserRoles.ALUMNI.getRoleName());
    assertTrue(alumni.isEmpty());

    List<User> admins = userDAO.describeUsersByRole(UserRoles.ADMIN.getRoleName());
    assertTrue(admins.isEmpty());

    List<User> researchers = userDAO.describeUsersByRole(UserRoles.RESEARCHER.getRoleName());
    assertTrue(researchers.isEmpty());
  }

  @Test
  void testCheckChairpersonUser() {
    User chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    User member = createUserWithRole(UserRoles.MEMBER.getRoleId());
    assertNotNull(userDAO.checkChairpersonUser(chair.getUserId()));
    assertNull(userDAO.checkChairpersonUser(member.getUserId()));
  }

  @Test
  void testFindDACUsersEnabledToVoteByDacEmpty() {
    Dac dac = createDac();
    Collection<User> users = userDAO.findUsersEnabledToVoteByDAC(dac.getDacId());
    assertTrue(users.isEmpty());
  }

  @Test
  void testFindDACUsersEnabledToVoteByDacNotEmpty() {
    Dac dac = createDac();
    User chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId());
    Collection<User> users = userDAO.findUsersEnabledToVoteByDAC(dac.getDacId());
    assertFalse(users.isEmpty());
  }

  @Test
  void testFindNonDACUsersEnabledToVote() {
    createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    createUserWithRole(UserRoles.MEMBER.getRoleId());
    Collection<User> users = userDAO.findNonDacUsersEnabledToVote();
    assertFalse(users.isEmpty());
  }

  @Test
  void testFindUsersWithRoles() {
    User chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    userRoleDAO.insertSingleUserRole(UserRoles.MEMBER.getRoleId(), chair.getUserId());
    Collection<Integer> userIds = Collections.singletonList(chair.getUserId());
    Collection<User> users = userDAO.findUsersWithRoles(userIds);
    users.forEach(
        u -> assertFalse(u.getRoles().isEmpty(), "User: " + u.getUserId() + " has no roles"));
    assertEquals(1, users.size());
    User user = users.stream().findFirst().orElse(null);
    assertNotNull(user);
    assertEquals(2, user.getRoles().size());
  }

  @Test
  void testFindUserByEmail() {
    User user = createUser();
    userRoleDAO.insertSingleUserRole(UserRoles.ALUMNI.getRoleId(), user.getUserId());
    userRoleDAO.insertSingleUserRole(UserRoles.ADMIN.getRoleId(), user.getUserId());
    userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), user.getUserId());
    User user1 = userDAO.findUserByEmail(user.getEmail());
    assertNotNull(user1);

    // Assert roles are fetched correctly
    assertTrue(
        user1.getRoles().stream()
            .anyMatch(r -> r.getRoleId().equals(UserRoles.ALUMNI.getRoleId())));
    assertTrue(
        user1.getRoles().stream().anyMatch(r -> r.getRoleId().equals(UserRoles.ADMIN.getRoleId())));
    assertTrue(
        user1.getRoles().stream()
            .anyMatch(r -> r.getRoleId().equals(UserRoles.RESEARCHER.getRoleId())));

    User user2 = userDAO.findUserByEmail("no.one@nowhere.com");
    assertNull(user2);
  }

  @Test
  void testFindUserByEmails() {
    User user1 = createUser();
    userRoleDAO.insertSingleUserRole(UserRoles.ADMIN.getRoleId(), user1.getUserId());
    User user2 = createUser();
    userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), user2.getUserId());

    // Find only the first two users, ensure that we're not getting all 3
    List<User> users = userDAO.findUsersByEmailList(List.of(user1.getEmail(), user2.getEmail()));
    assertNotNull(users);
    assertFalse(users.isEmpty());
    assertEquals(2, users.size());
    assertTrue(users.contains(user1));
    assertTrue(users.contains(user2));
  }

  @Test
  void testFindUsersWithLCsAndInstitution() {
    // Creates an Admin and an SO, and returns the SO
    User signingOfficial = createUserWithInstitution();
    // Creates a researcher
    User user =
        createUserWithRole(UserRoles.RESEARCHER.getRoleId(), signingOfficial.getInstitutionId());
    int dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), new Date());
    Instant now = Instant.now();
    int daaId = daaDAO.createDaa(user.getUserId(), now, user.getUserId(), now, dacId);
    int lcId1 =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(),
            user.getDisplayName(),
            user.getEmail(),
            signingOfficial.getUserId(),
            new Date());
    libraryCardDAO.createLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId1, daaId);

    // Creates another admin and another SO
    User signingOfficial2 = createUserWithInstitution();
    // Creates a researcher
    User user2 =
        createUserWithRole(UserRoles.RESEARCHER.getRoleId(), signingOfficial2.getInstitutionId());
    int lcId2 =
        libraryCardDAO.insertLibraryCard(
            user2.getUserId(),
            user2.getDisplayName(),
            user2.getEmail(),
            signingOfficial2.getUserId(),
            new Date());
    libraryCardDAO.createLibraryCardDaaRelation(
        user2.getUserId(), signingOfficial2.getUserId(), lcId2, daaId);

    List<User> users = userDAO.findUsersWithLCsAndInstitution();
    // Filter out non-researchers since those are the ones we've added LCs to and are under test.
    users = users.stream().filter(u -> u.hasUserRole(UserRoles.RESEARCHER)).toList();
    assertEquals(2, users.size());
    users.forEach(
        u -> {
          assertNotNull(u.getInstitution());
          assertNotNull(u.getLibraryCard());
        });
  }

  @Test
  void testFindUsersByRoleId() {
    User researcher = createUser();
    userDAO.updateEmailPreference(researcher.getUserId(), true);
    Collection<User> researchers = userDAO.findUsersByRoleId(UserRoles.RESEARCHER.getRoleId());
    assertFalse(researchers.isEmpty());
  }

  @Test
  void testUpdateEmailPreference() {
    User researcher = createUser();
    userDAO.updateEmailPreference(researcher.getUserId(), true);
    User u1 = userDAO.findUserById(researcher.getUserId());
    assertTrue(u1.getEmailPreference());
    userDAO.updateEmailPreference(researcher.getUserId(), false);
    User u2 = userDAO.findUserById(researcher.getUserId());
    assertFalse(u2.getEmailPreference());
  }

  @Test
  void testUpdateInstitutionId() {
    User researcher = createUser();
    Integer institutionId =
        institutionDAO.insertInstitution(
            "Institution",
            "it director",
            "it director email",
            null,
            null,
            null,
            null,
            null,
            null,
            researcher.getUserId(),
            new Date());
    userDAO.updateInstitutionId(researcher.getUserId(), institutionId);
    User u1 = userDAO.findUserById(researcher.getUserId());
    assertEquals(institutionId, u1.getInstitutionId());
  }

  @Test
  void testUpdateDisplayName() {
    User researcher = createUser();
    String newName = randomAlphabetic(10);
    userDAO.updateDisplayName(researcher.getUserId(), newName);
    User u1 = userDAO.findUserById(researcher.getUserId());
    assertEquals(newName, u1.getDisplayName());
  }

  @Test
  void testUpdateDisplayNameInvalidChars() {
    User researcher = createUser();
    String newName = "invalid\0name";
    assertThrows(
        UnableToExecuteStatementException.class,
        () -> userDAO.updateDisplayName(researcher.getUserId(), newName));
  }

  @Test
  void testUpdateData() {
    User researcher = createUser();
    Map<String, Object> researcherData = researcher.getUserData();
    researcherData.put("test", "test");
    userDAO.updateData(researcher.getUserId(), GsonUtil.getInstance().toJson(researcherData));

    User researcherFromDb = userDAO.findUserById(researcher.getUserId());
    assertEquals(researcherData, researcherFromDb.getUserData());
  }

  @Test
  void testFindUsersForDatasetsByRole() {
    Dataset dataset = createDataset();
    Dac dac = createDac();
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());

    Set<User> users =
        userDAO.findUsersForDatasetsByRole(
            Collections.singletonList(dataset.getDatasetId()),
            Collections.singletonList(UserRoles.CHAIRPERSON.getRoleId()));
    Optional<User> foundUser = users.stream().findFirst();
    assertNotNull(users);
    assertFalse(users.isEmpty());
    assertEquals(1, users.size());
    assertEquals(user.getUserId(), foundUser.get().getUserId());
  }

  @Test
  void testFindUsersForDatasetsByRoleNotFound() {
    Dataset dataset = createDataset();
    Dac dac = createDac();
    createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());

    Set<User> users =
        userDAO.findUsersForDatasetsByRole(
            Collections.singletonList(dataset.getDatasetId()),
            Collections.singletonList(UserRoles.CHAIRPERSON.getRoleId()));
    assertNotNull(users);
    assertTrue(users.isEmpty());
  }

  @Test
  void testFindUsersByInstitution() {
    Integer institutionId = createUserWithInstitution().getInstitutionId();
    List<User> beforeList = userDAO.findUsersByInstitution(institutionId);
    // should not change results since they are not in the institution
    createUser();
    createUser();
    List<User> afterList = userDAO.findUsersByInstitution(institutionId);
    assertEquals(1, beforeList.size());
    assertEquals(beforeList, afterList);
  }

  @Test
  void testGetSOsByInstitution() {
    // user with institutionId and SO role
    User user = createUserWithInstitution();
    Integer institutionId = user.getInstitutionId();
    String displayName = user.getDisplayName();
    String email = user.getEmail();
    List<User> users = userDAO.getSOsByInstitution(institutionId);
    assertEquals(1, users.size());
    assertEquals(displayName, users.getFirst().getDisplayName());
    assertEquals(email, users.getFirst().getEmail());

    List<User> differentInstitutionUsers = userDAO.getSOsByInstitution(institutionId + 1);
    assertEquals(0, differentInstitutionUsers.size());
  }

  @Test
  void testGetUsersFromInstitutionWithCards() {
    User signingOfficial = createUser();
    int dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), new Date());
    Instant now = Instant.now();
    LibraryCard card = createLibraryCard();
    int daaId = daaDAO.createDaa(card.getUserId(), now, card.getUserId(), now, dacId);
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId);
    User lcUser = userDAO.findUserById(card.getUserId());
    Integer userId = card.getUserId();
    List<User> users = userDAO.getUsersFromInstitutionWithCards(lcUser.getInstitutionId());
    assertEquals(1, users.size());
    User returnedUser = users.getFirst();
    assertEquals(userId, returnedUser.getUserId());

    LibraryCard returnedCard = returnedUser.getLibraryCard();
    assertEquals(card.getId(), returnedCard.getId());
    assertEquals(userId, returnedCard.getUserId());
  }

  @Test
  void testGetUsersWithCardsByDaaId() {
    User signingOfficial = createUser();
    int dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), new Date());
    Instant now = Instant.now();
    LibraryCard card1 = createLibraryCard();
    int daaId1 = daaDAO.createDaa(card1.getUserId(), now, card1.getUserId(), now, dacId);
    libraryCardDAO.createLibraryCardDaaRelation(
        card1.getUserId(), signingOfficial.getUserId(), card1.getId(), daaId1);
    LibraryCard card2 = createLibraryCard();
    int daaId2 = daaDAO.createDaa(card2.getUserId(), now, card2.getUserId(), now, dacId);
    libraryCardDAO.createLibraryCardDaaRelation(
        card2.getUserId(), signingOfficial.getUserId(), card2.getId(), daaId2);

    List<User> daa1UserList = userDAO.getUsersWithCardsByDaaId(daaId1);
    assertEquals(1, daa1UserList.size());

    List<User> daa2UserList = userDAO.getUsersWithCardsByDaaId(daaId2);
    assertEquals(1, daa2UserList.size());
    assertNotEquals(daa1UserList, daa2UserList);

    User daa1User = daa1UserList.getFirst();
    assertEquals(card1, daa1User.getLibraryCard());
    assertEquals(daa1User.getLibraryCard().getDaaIds(), List.of(daaId1));

    User daa2User = daa2UserList.getFirst();
    assertEquals(card2, daa2User.getLibraryCard());
    assertEquals(daa2User.getLibraryCard().getDaaIds(), List.of(daaId2));
  }

  @Test
  void testUpdateEraCommonsId() {
    User u = createUser();
    String era = u.getEraCommonsId();
    assertNull(era);
    userDAO.updateEraCommonsId(u.getUserId(), "newEraCommonsId");
    User updated = userDAO.findUserById(u.getUserId());
    assertEquals("newEraCommonsId", updated.getEraCommonsId());
  }

  @Test
  void testCanAddAllRoles() {
    User u = createUser();

    UserRoles[] roles = UserRoles.values();

    for (UserRoles role : roles) {
      u.addRole(new UserRole(role.getRoleId(), role.getRoleName()));
    }

    userRoleDAO.insertUserRoles(u.getRoles(), u.getUserId());

    User found = userDAO.findUserById(u.getUserId());

    for (UserRoles role : roles) {
      // ensure that each role exists on user
      assertTrue(
          found.getRoles().stream()
              .anyMatch(
                  existingRole ->
                      (role.getRoleId().equals(existingRole.getRoleId())
                          && role.getRoleName().equals(existingRole.getName()))));
    }
  }

  @Test
  void testCanBeChairOfTwoDACs() {
    User u = createUser();
    Dac dac1 = createDac();
    Dac dac2 = createDac();
    UserRole chairperson1 = UserRoles.Chairperson();
    chairperson1.setDacId(dac1.getDacId());
    chairperson1.setUserId(u.getUserId());
    UserRole chairperson2 = UserRoles.Chairperson();
    chairperson2.setDacId(dac2.getDacId());
    chairperson2.setUserId(u.getUserId());
    assertNotEquals(chairperson1, chairperson2);

    u.addRole(chairperson1);
    u.addRole(chairperson2);
    assertEquals(3, u.getRoles().size());
    assertTrue(u.getRoles().contains(chairperson1));
    assertTrue(u.getRoles().contains(chairperson2));

    dacDAO.addDacMember(chairperson1.getRoleId(), u.getUserId(), chairperson1.getDacId());
    dacDAO.addDacMember(chairperson2.getRoleId(), u.getUserId(), chairperson2.getDacId());

    User found = userDAO.findUserById(u.getUserId());
    assertEquals(3, found.getRoles().size());
    assertTrue(found.getRoles().stream().anyMatch(chairperson1::equals));
    assertTrue(found.getRoles().contains(chairperson1));
    assertTrue(found.getRoles().stream().anyMatch(chairperson2::equals));
    assertTrue(found.getRoles().contains(chairperson2));
  }

  @Test
  void testFindAllEmailReceivingThinlyPopulatedUsers() {
    createUser();
    createUser();
    User u3 = createUser();

    userDAO.updateEmailPreference(u3.getUserId(), false);

    AtomicInteger count = new AtomicInteger();
    AtomicBoolean user3Found = new AtomicBoolean(false);
    Integer emailType = EmailType.NEW_STUDY_DIGEST.getTypeInt();
    String referenceId = "referenceId";

    userDAO
        .getHandle()
        .getJdbi()
        .useHandle(
            ignored -> {
              ResultIterable<User> users =
                  userDAO.allEmailReceivingThinlyPopulatedUsers(emailType, referenceId);
              for (User user : users) {
                count.getAndIncrement();
                if (user.getUserId().equals(u3.getUserId())) {
                  user3Found.set(true);
                } else {
                  // simulate having sent a message so we won't send one again.
                  mailMessageDAO.insert(
                      referenceId,
                      null,
                      user.getUserId(),
                      emailType,
                      Instant.now(),
                      "",
                      null,
                      null,
                      Instant.now());
                }
              }
            });
    assertEquals(2, count.get());
    assertFalse(user3Found.get());

    // verify entries that were "sent messages" don't show up in the list again.
    count.set(0);
    userDAO
        .getHandle()
        .getJdbi()
        .useHandle(
            ignored -> {
              ResultIterable<User> users =
                  userDAO.allEmailReceivingThinlyPopulatedUsers(emailType, referenceId);
              for (User user : users) {
                count.getAndIncrement();
                if (user.getUserId().equals(u3.getUserId())) {
                  user3Found.set(true);
                }
              }
            });
    assertEquals(0, count.get());
  }

  private Dac createDac() {
    Integer id =
        dacDAO.createDac(
            "Test_" + randomAlphanumeric(20), "Test_" + randomAlphanumeric(20), new Date());
    return dacDAO.findById(id);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private LibraryCard createLibraryCard() {
    User user = createUserWithInstitution();
    Integer id =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }
}
