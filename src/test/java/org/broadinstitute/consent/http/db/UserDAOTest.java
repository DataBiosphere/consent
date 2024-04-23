package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.Test;

class UserDAOTest extends DAOTestHelper {

  @Test
  void testFindDACUserById() {
    User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
    assertNotNull(user);
    assertFalse(user.getRoles().isEmpty());

    userRoleDAO.insertSingleUserRole(UserRoles.ADMIN.getRoleId(), user.getUserId());
    userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), user.getUserId());

    User user2 = userDAO.findUserById(user.getUserId());
    assertNotNull(user2);
    assertEquals(user.getEmail(), user2.getEmail());

    // Assert roles are fetched correctly
    assertTrue(user2.getRoles().stream()
        .anyMatch(r -> r.getRoleId().equals(UserRoles.ALUMNI.getRoleId())));
    assertTrue(user2.getRoles().stream()
        .anyMatch(r -> r.getRoleId().equals(UserRoles.ADMIN.getRoleId())));
    assertTrue(user2.getRoles().stream()
        .anyMatch(r -> r.getRoleId().equals(UserRoles.RESEARCHER.getRoleId())));

    //assert institution base data is present if available
    User user3 = createUserWithInstitution();
    User queriedUser3 = userDAO.findUserById(user3.getUserId());
    assert (queriedUser3.getUserId()).equals(user3.getUserId());
    assertNotNull(queriedUser3.getInstitutionId());
    assert (queriedUser3.getInstitution().getId()).equals(user3.getInstitution().getId());
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
    users.forEach(u -> assertFalse(u.getRoles().isEmpty(),
        "User: " + u.getUserId() + " has no roles"));
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
    assertTrue(user1.getRoles().stream()
        .anyMatch(r -> r.getRoleId().equals(UserRoles.ALUMNI.getRoleId())));
    assertTrue(user1.getRoles().stream()
        .anyMatch(r -> r.getRoleId().equals(UserRoles.ADMIN.getRoleId())));
    assertTrue(user1.getRoles().stream()
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
  void testInsertUser() {
    // No-op ... tested in `createUser()`
  }

  @Test
  void testUpdateUser_case1() {
    User user = createUser();
    Institution firstInstitute = createInstitution();
    userDAO.updateUser(
        "Dac User Test",
        user.getUserId(),
        firstInstitute.getId()
    );
    User user2 = userDAO.findUserById(user.getUserId());
    assertEquals(user2.getInstitution().getId(), firstInstitute.getId());
  }

  @Test
  void testDeleteUserById() {
    // No-op ... tested in `tearDown()`
  }

  @Test
  void testFindUsersWithLCsAndInstitution() {
    User user = createUserWithInstitution();
    libraryCardDAO.insertLibraryCard(user.getUserId(), user.getInstitutionId(), "asdf",
        user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());

    User user2 = createUserWithInstitution();
    libraryCardDAO.insertLibraryCard(user2.getUserId(), user.getInstitutionId(), "asdf",
        user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());

    List<User> users = userDAO.findUsersWithLCsAndInstitution();
    assertNotNull(users);
    assertFalse(users.isEmpty());
    assertEquals(2, users.size());
    assertNotNull(users.get(0).getInstitution());
    assertNotNull(users.get(0).getLibraryCards());
    assertEquals(1, users.get(0).getLibraryCards().size());
    assertNotNull(users.get(1).getInstitution());
    assertNotNull(users.get(1).getLibraryCards());
    assertEquals(1, users.get(1).getLibraryCards().size());

  }

  @Test
  void testDescribeUsersByRoleAndEmailPreference() {
    User researcher = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
    userDAO.updateEmailPreference(researcher.getUserId(), true);
    Collection<User> researchers = userDAO.describeUsersByRoleAndEmailPreference("Researcher",
        true);
    assertFalse(researchers.isEmpty());
  }

  @Test
  void testUpdateEmailPreference() {
    User researcher = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
    userDAO.updateEmailPreference(researcher.getUserId(), true);
    User u1 = userDAO.findUserById(researcher.getUserId());
    assertTrue(u1.getEmailPreference());
    userDAO.updateEmailPreference(researcher.getUserId(), false);
    User u2 = userDAO.findUserById(researcher.getUserId());
    assertFalse(u2.getEmailPreference());
  }

  @Test
  void testUpdateInstitutionId() {
    User researcher = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
    Integer institutionId = institutionDAO.insertInstitution("Institution", "it director",
        "it director email", null, null, null, null, null, null, researcher.getUserId(),
        new Date());
    userDAO.updateInstitutionId(researcher.getUserId(), institutionId);
    User u1 = userDAO.findUserById(researcher.getUserId());
    assertEquals(institutionId, u1.getInstitutionId());
  }

  @Test
  void testUpdateDisplayName() {
    User researcher = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
    String newName = RandomStringUtils.random(10, true, false);
    userDAO.updateDisplayName(researcher.getUserId(), newName);
    User u1 = userDAO.findUserById(researcher.getUserId());
    assertEquals(newName, u1.getDisplayName());
  }

  @Test
  void testFindUserByEmailAndRoleId() {
    User chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
    User user = userDAO.findUserByEmailAndRoleId(chair.getEmail(),
        UserRoles.CHAIRPERSON.getRoleId());
    assertNotNull(user);
    assertEquals(chair.getUserId(), user.getUserId());
    assertEquals(chair.getDisplayName(), user.getDisplayName());
  }

  @Test
  void testFindUsersForDatasetsByRole() {
    Dataset dataset = createDataset();
    Dac dac = createDac();
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    Set<User> users = userDAO.findUsersForDatasetsByRole(
        Collections.singletonList(dataset.getDataSetId()),
        Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
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
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    Set<User> users = userDAO.findUsersForDatasetsByRole(
        Collections.singletonList(dataset.getDataSetId()),
        Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
    assertNotNull(users);
    assertTrue(users.isEmpty());
  }

  @Test
  void testFindUsersByInstitution() {
    Integer institutionId = createUserWithInstitution().getInstitutionId();
    List<User> beforeList = userDAO.findUsersByInstitution(institutionId);
    //should not change results since they are not in the institution
    createUser();
    createUser();
    List<User> afterList = userDAO.findUsersByInstitution(institutionId);
    assertEquals(1, beforeList.size());
    assertEquals(beforeList, afterList);
  }

  @Test
  void testGetSOsByInstitution() {
    //user with institutionId and SO role
    User user = createUserWithInstitution();
    Integer institutionId = user.getInstitutionId();
    String displayName = user.getDisplayName();
    String email = user.getEmail();
    List<User> users = userDAO.getSOsByInstitution(institutionId);
    assertEquals(1, users.size());
    assertEquals(displayName, users.get(0).getDisplayName());
    assertEquals(email, users.get(0).getEmail());

    List<User> differentInstitutionUsers = userDAO.getSOsByInstitution(institutionId + 1);
    assertEquals(0, differentInstitutionUsers.size());
  }

  @Test
  void testGetUsersFromInstitutionWithCards() {
    LibraryCard card = createLibraryCard();
    Integer institutionId = card.getInstitutionId();
    Integer userId = card.getUserId();
    List<User> users = userDAO.getUsersFromInstitutionWithCards(institutionId);
    assertEquals(1, users.size());
    User returnedUser = users.get(0);
    assertEquals(userId, returnedUser.getUserId());

    LibraryCard returnedCard = returnedUser.getLibraryCards().get(0);
    assertEquals(card.getId(), returnedCard.getId());
    assertEquals(userId, returnedCard.getUserId());
  }

  @Test
  void testGetUsersWithNoInstitution() {
    createUserWithInstitution();
    User user = createUser();
    List<User> users = userDAO.getUsersWithNoInstitution();
    assertEquals(1, users.size());
    assertEquals(user.getUserId(), users.get(0).getUserId());
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
      assertTrue(found.getRoles().stream().anyMatch(
          (existingRole) -> (
              role.getRoleId().equals(existingRole.getRoleId())
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

  private Dac createDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Institution createInstitution() {
    User createUser = createUser();
    Integer id = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        createUser.getUserId(),
        createUser.getCreateDate());
    Institution institution = institutionDAO.findInstitutionById(id);
    User updateUser = createUser();
    institutionDAO.updateInstitutionById(
        id,
        institution.getName(),
        institution.getItDirectorEmail(),
        institution.getItDirectorName(),
        institution.getInstitutionUrl(),
        institution.getDunsNumber(),
        institution.getOrgChartUrl(),
        institution.getVerificationUrl(),
        institution.getVerificationFilename(),
        institution.getOrganizationType().getValue(),
        updateUser.getUserId(),
        new Date()
    );
    return institutionDAO.findInstitutionById(id);
  }

  private LibraryCard createLibraryCard() {
    Integer institutionId = createInstitution().getId();
    String email = RandomStringUtils.randomAlphabetic(11);
    Integer userId = userDAO.insertUser(email, "displayName", new Date());
    userDAO.updateUser(email, userId, institutionId);
    String stringValue = "value";
    Integer id = libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue,
        stringValue, userId, new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  private User createUserWithRoleInDac(Integer roleId, Integer dacId) {
    User user = createUserWithRole(roleId);
    dacDAO.addDacMember(roleId, user.getUserId(), dacId);
    return user;
  }

  private User createUserWithRole(Integer roleId) {
    int i1 = RandomUtils.nextInt(5, 10);
    int i2 = RandomUtils.nextInt(5, 10);
    int i3 = RandomUtils.nextInt(3, 5);
    String email = RandomStringUtils.randomAlphabetic(i1) +
        "@" +
        RandomStringUtils.randomAlphabetic(i2) +
        "." +
        RandomStringUtils.randomAlphabetic(i3);
    Integer userId = userDAO.insertUser(email, "display name", new Date());
    userRoleDAO.insertSingleUserRole(roleId, userId);
    return userDAO.findUserById(userId);
  }

  private User createUserWithInstitution() {
    int i1 = RandomUtils.nextInt(5, 10);
    String email = RandomStringUtils.randomAlphabetic(i1);
    String name = RandomStringUtils.randomAlphabetic(10);
    Integer userId = userDAO.insertUser(email, name, new Date());
    Integer institutionId = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        userId,
        new Date());
    userDAO.updateUser(name, userId, institutionId);
    userRoleDAO.insertSingleUserRole(7, userId);
    return userDAO.findUserById(userId);
  }

}
