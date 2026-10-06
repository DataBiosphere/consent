package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.CreatedBucket;
import org.broadinstitute.consent.http.models.RoleUserCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountMetricsDAOTest extends DAOTestHelper {

  private static final Instant FROM = startOf(LocalDate.of(2026, 1, 1));
  private static final Instant TO = startOf(LocalDate.of(2027, 1, 1));

  private AccountMetricsDAO dao;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(AccountMetricsDAO.class);
  }

  @Test
  void bucketsUsersAndInstitutionsEitherSideOfAQuarterBoundary() {
    Integer creator = user(LocalDateTime.of(2026, 3, 31, 23, 0), UserRoles.ADMIN);
    user(LocalDateTime.of(2026, 4, 1, 1, 0), UserRoles.RESEARCHER);
    user(LocalDateTime.of(2026, 4, 2, 1, 0), UserRoles.RESEARCHER);
    institution(LocalDateTime.of(2026, 3, 31, 23, 0), creator);

    assertEquals(
        List.of(
            new CreatedBucket(startOf(LocalDate.of(2026, 1, 1)), 1),
            new CreatedBucket(startOf(LocalDate.of(2026, 4, 1)), 2)),
        dao.countUsersCreated(FROM, TO, "quarter"));
    assertEquals(
        List.of(new CreatedBucket(startOf(LocalDate.of(2026, 1, 1)), 1)),
        dao.countInstitutionsCreated(FROM, TO, "quarter"));
  }

  @Test
  void rangeIncludesItsStartAndExcludesItsEnd() {
    Integer creator = user(LocalDateTime.of(2025, 12, 31, 23, 59), UserRoles.ADMIN);
    user(LocalDateTime.of(2026, 1, 1, 0, 0), UserRoles.RESEARCHER);
    user(LocalDateTime.of(2027, 1, 1, 0, 0), UserRoles.RESEARCHER);
    institution(LocalDateTime.of(2027, 1, 1, 0, 0), creator);

    assertEquals(List.of(new CreatedBucket(FROM, 1)), dao.countUsersCreated(FROM, TO, "quarter"));
    assertTrue(dao.countInstitutionsCreated(FROM, TO, "quarter").isEmpty());
    assertEquals(
        List.of(new RoleUserCount(UserRoles.RESEARCHER.getRoleName(), 1)),
        dao.countUsersByRole(FROM, TO));
  }

  @Test
  void aUserCountsOnceInTheTotalAndOncePerRole() {
    LocalDateTime created = LocalDateTime.of(2026, 6, 1, 12, 0);
    user(created, UserRoles.RESEARCHER, UserRoles.SIGNINGOFFICIAL);
    Integer member = user(created, UserRoles.RESEARCHER);
    dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member, dac(member), member);
    dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member, dac(member), member);
    user(created);

    assertEquals(3, dao.countUsersCreated(FROM, TO, "quarter").getFirst().count());
    assertEquals(
        List.of(
            new RoleUserCount(UserRoles.RESEARCHER.getRoleName(), 2),
            new RoleUserCount(UserRoles.MEMBER.getRoleName(), 1),
            new RoleUserCount(UserRoles.SIGNINGOFFICIAL.getRoleName(), 1)),
        dao.countUsersByRole(FROM, TO));
  }

  private static Instant startOf(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  private static Date at(LocalDateTime time) {
    return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
  }

  private Integer user(LocalDateTime created, UserRoles... roles) {
    Integer userId =
        userDAO.insertUser(UUID.randomUUID() + "@example.org", "Name", null, at(created));
    for (UserRoles role : roles) {
      userRoleDAO.insertSingleUserRole(role.getRoleId(), userId);
    }
    return userId;
  }

  private void institution(LocalDateTime created, Integer creator) {
    institutionDAO.insertInstitution(
        "Institution " + UUID.randomUUID(),
        "itDirectorName",
        "itDirectorEmail",
        null,
        null,
        null,
        null,
        null,
        OrganizationType.NON_PROFIT.getValue(),
        creator,
        at(created));
  }

  private Integer dac(Integer creator) {
    return dacDAO.createDac("DAC " + UUID.randomUUID(), "description", creator);
  }
}
