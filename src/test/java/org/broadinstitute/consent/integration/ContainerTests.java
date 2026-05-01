package org.broadinstitute.consent.integration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Config;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
public abstract class ContainerTests implements ConsentLogger {

  protected static final DropwizardAppExtension<ConsentConfiguration> APPLICATION =
      new DropwizardAppExtension<>(
          ConsentApplication.class, ResourceHelpers.resourceFilePath("consent-ci.yaml"));

  /**
   * WireMock server running on port 9999, which is the fixed base URL used by consent-ci.yaml for
   * every external service (Sam, ECM, GCS, etc.). Subclass tests stub specific paths on this server
   * before making authenticated API calls.
   */
  protected static final WireMockServer WIRE_MOCK = new WireMockServer(options().port(9999));

  /**
   * Guards the one-time database seed so it executes only on the first {@code @BeforeAll}
   * invocation across all concrete subclasses in the same JVM, avoiding redundant JDBI setup and
   * keeping the seed truly "once per test plan".
   */
  private static final AtomicBoolean SEEDED = new AtomicBoolean(false);

  // Note: never close the client returned here — the extension manages its lifetime.
  protected static Client getClient() {
    return APPLICATION.client();
  }

  /**
   * Starts WireMock and seeds the database once per JVM run via typed DAO calls.
   *
   * <p>{@link DropwizardExtensionsSupport} implements {@code BeforeAllCallback}, which JUnit 5
   * calls before {@code @BeforeAll} methods, so the application and its database are fully started
   * when this method executes. A static {@link AtomicBoolean} guard ensures the expensive JDBI
   * setup and seed inserts are performed only on the first invocation, even though
   * {@code @BeforeAll} fires once per concrete subclass.
   *
   * <p>Every insert operation is idempotent: rows are skipped when they already exist.
   */
  @BeforeAll
  static void seedDatabase() {
    if (!WIRE_MOCK.isRunning()) {
      WIRE_MOCK.start();
    }

    if (!SEEDED.compareAndSet(false, true)) {
      return;
    }

    ConsentConfiguration config = APPLICATION.getConfiguration();
    Environment environment = APPLICATION.getEnvironment();

    // Build a dedicated JDBI instance for seeding, using the same config/plugins as DAOTestHelper.
    Jdbi jdbi = new JdbiFactory().build(environment, config.getDataSourceFactory(), "seed");
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.installPlugin(new Gson2Plugin());
    jdbi.installPlugin(new GuavaPlugin());
    jdbi.getConfig().get(Gson2Config.class).setGson(GsonUtil.buildGson());

    UserDAO userDAO = jdbi.onDemand(UserDAO.class);
    UserRoleDAO userRoleDAO = jdbi.onDemand(UserRoleDAO.class);
    InstitutionDAO institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    DacDAO dacDAO = jdbi.onDemand(DacDAO.class);

    seedUsers(userDAO);
    int adminId = userDAO.findUserByEmail("ci-admin@example.com").getUserId();
    seedInstitution(institutionDAO, userDAO, adminId);
    seedNonDacRoles(userDAO, userRoleDAO);
    int dacId = seedDac(dacDAO, adminId);
    seedDacMembers(dacDAO, userDAO, userRoleDAO, dacId, adminId);
  }

  @AfterAll
  static void stopWireMock() {
    if (WIRE_MOCK.isRunning()) {
      WIRE_MOCK.stop();
    }
  }

  // -------------------------------------------------------------------------
  // Section 1 – Users
  // -------------------------------------------------------------------------

  /** Canonical synthetic users seeded into the CI database before any tests run. */
  public record CiUser(String email, String displayName) {}

  protected static final List<CiUser> CI_USERS =
      List.of(
          new CiUser("ci-admin@example.com", "CI Admin"),
          new CiUser("ci-signing-official@example.com", "CI Signing Official"),
          new CiUser("ci-it-director@example.com", "CI IT Director"),
          new CiUser("ci-data-submitter@example.com", "CI Data Submitter"),
          new CiUser("ci-researcher@example.com", "CI Researcher"),
          new CiUser("ci-chair@example.com", "CI DAC Chair"),
          new CiUser("ci-member@example.com", "CI DAC Member"));

  private static void seedUsers(UserDAO userDAO) {
    Date now = new Date();
    CI_USERS.forEach(
        u -> {
          if (userDAO.findUserByEmail(u.email()) == null) {
            userDAO.insertUser(u.email(), u.displayName(), null, now);
          }
        });
  }

  // -------------------------------------------------------------------------
  // Section 2 – Institution
  // -------------------------------------------------------------------------

  private static void seedInstitution(InstitutionDAO institutionDAO, UserDAO userDAO, int adminId) {
    List<Institution> existing = institutionDAO.findInstitutionsByName("CI Test Institution");
    int institutionId =
        existing.isEmpty()
            ? institutionDAO.insertInstitution(
                "CI Test Institution",
                "CI IT Director",
                "ci-it-director@example.com",
                null,
                null,
                null,
                null,
                null,
                null,
                adminId,
                new Date())
            : existing.getFirst().getId();

    // Link researcher and signing official to the institution if not already set.
    for (String email : List.of("ci-researcher@example.com", "ci-signing-official@example.com")) {
      User user = userDAO.findUserByEmail(email);
      if (user.getInstitutionId() == null) {
        userDAO.updateInstitutionId(user.getUserId(), institutionId);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Section 3 – Non-DAC user roles
  // -------------------------------------------------------------------------

  private static void seedNonDacRoles(UserDAO userDAO, UserRoleDAO userRoleDAO) {
    record RoleAssignment(String roleName, String userEmail) {}
    List.of(
            new RoleAssignment("Admin", "ci-admin@example.com"),
            new RoleAssignment("SigningOfficial", "ci-signing-official@example.com"),
            new RoleAssignment("ITDirector", "ci-it-director@example.com"),
            new RoleAssignment("DataSubmitter", "ci-data-submitter@example.com"),
            new RoleAssignment("Researcher", "ci-researcher@example.com"))
        .forEach(
            ra -> {
              int roleId = userRoleDAO.findRoleIdByName(ra.roleName());
              int userId = userDAO.findUserByEmail(ra.userEmail()).getUserId();
              if (userRoleDAO.findRoleByUserIdAndRoleId(userId, roleId) == null) {
                userRoleDAO.insertSingleUserRole(roleId, userId);
              }
            });
  }

  // -------------------------------------------------------------------------
  // Section 4 – DAC (CREATE audit written atomically by createDac)
  // -------------------------------------------------------------------------

  private static int seedDac(DacDAO dacDAO, int adminId) {
    return dacDAO.findAll().stream()
        .filter(d -> "CI Test DAC".equals(d.getName()))
        .findFirst()
        .map(Dac::getDacId)
        .orElseGet(
            () -> dacDAO.createDac("CI Test DAC", "Test DAC for CI integration tests", adminId));
  }

  // -------------------------------------------------------------------------
  // Section 5 – DAC member assignments (ADD audit written atomically by addDacMember)
  // -------------------------------------------------------------------------

  private static void seedDacMembers(
      DacDAO dacDAO, UserDAO userDAO, UserRoleDAO userRoleDAO, int dacId, int adminId) {
    Set<Integer> presentMemberIds =
        dacDAO.findMembersByDacId(dacId).stream().map(User::getUserId).collect(Collectors.toSet());

    record DacMember(String roleName, String userEmail) {}
    List.of(
            new DacMember(UserRoles.CHAIRPERSON.getRoleName(), "ci-chair@example.com"),
            new DacMember(UserRoles.MEMBER.getRoleName(), "ci-member@example.com"))
        .forEach(
            dm -> {
              int roleId = userRoleDAO.findRoleIdByName(dm.roleName());
              int userId = userDAO.findUserByEmail(dm.userEmail()).getUserId();
              if (!presentMemberIds.contains(userId)) {
                dacDAO.addDacMember(roleId, userId, dacId, adminId);
              }
            });
  }
}
