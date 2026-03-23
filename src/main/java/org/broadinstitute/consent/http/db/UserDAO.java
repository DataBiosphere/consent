package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesReducer;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface UserDAO extends Transactional<UserDAO> {

  String QUERY_FIELD_SEPARATOR = ", ";

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      """
      SELECT
          u.user_id as u_user_id,
          u.email as u_email,
          u.display_name as u_display_name,
          u.create_date as u_create_date,
          u.email_preference as u_email_preference,
          u.institution_id as u_institution_id,
          u.era_commons_id as u_era_commons_id,
          i.institution_id as i_id,
          i.institution_name as i_name,
          i.it_director_name as i_it_director_name,
          i.it_director_email as i_it_director_email,
          i.create_date as i_create_date,
          i.update_date as i_update_date,
          ur.user_role_id as ur_user_role_id, ur.user_id as ur_user_id,
          ur.role_id as ur_role_id, ur.dac_id as ur_dac_id, r.name as ur_name,
          lc.id AS lc_id, lc.user_id AS lc_user_id,
          lc.user_name AS lc_user_name, lc.user_email AS lc_user_email,
          lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date,
          lc.update_user_id AS lc_update_user_id,
          ld.daa_id as lc_daa_id
      FROM users u
      LEFT JOIN user_role ur ON ur.user_id = u.user_id
      LEFT JOIN roles r ON r.role_id = ur.role_id
      LEFT JOIN institution i ON u.institution_id = i.institution_id
      LEFT JOIN library_card lc ON lc.user_id = u.user_id
      LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
      WHERE u.user_id = :userId
      """)
  User findUserById(@Bind("userId") Integer userId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      """
      SELECT
          u.user_id as u_user_id,
          u.email as u_email,
          u.display_name as u_display_name,
          u.create_date as u_create_date,
          u.email_preference as u_email_preference,
          u.institution_id as u_institution_id,
          u.era_commons_id as u_era_commons_id,
          i.institution_id as i_id,
          i.institution_name as i_name,
          i.it_director_name as i_it_director_name,
          i.it_director_email as i_it_director_email,
          i.create_date as i_create_date,
          i.update_date as i_update_date,
          ur.user_role_id as ur_user_role_id, ur.user_id as ur_user_id,
          ur.role_id as ur_role_id, ur.dac_id as ur_dac_id, r.name as ur_name,
          lc.id AS lc_id, lc.user_id AS lc_user_id,
          lc.user_name AS lc_user_name, lc.user_email AS lc_user_email,
          lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date,
          lc.update_user_id AS lc_update_user_id,
          ld.daa_id as lc_daa_id,
          up.property_id as up_property_id, up.property_key as up_property_key,
          up.property_value as up_property_value
      FROM users u
      LEFT JOIN user_role ur ON ur.user_id = u.user_id
      LEFT JOIN roles r ON r.role_id = ur.role_id
      LEFT JOIN institution i ON u.institution_id = i.institution_id
      LEFT JOIN library_card lc ON lc.user_id = u.user_id
      LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
      LEFT JOIN user_property up ON up.user_id = u.user_id AND up.property_key IN (<keys>)
      WHERE u.user_id = :userId
      """)
  User findUserWithPropertiesById(
      @Bind("userId") Integer userId,
      @BindList(value = "keys", onEmpty = EmptyHandling.NULL_STRING) List<String> keys);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT " + User.QUERY_FIELDS_WITH_U_PREFIX + " FROM users u WHERE u.user_id IN (<userIds>)")
  Collection<User> findUsers(
      @BindList(value = "userIds", onEmpty = EmptyHandling.NULL_STRING)
          Collection<Integer> userIds);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT "
          + User.QUERY_FIELDS_WITH_U_PREFIX
          + QUERY_FIELD_SEPARATOR
          + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
          + " FROM users u "
          + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
          + " LEFT JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE r.name = :name")
  List<User> describeUsersByRole(@Bind("name") String name);

  @SqlQuery(
      "select du.user_id from users du inner join user_role ur on ur.user_id = du.user_id inner join roles r on r.role_id = ur.role_id where du.user_id = :userId and r.name = 'Chairperson'")
  Integer checkChairpersonUser(@Bind("userId") Integer userId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT "
          + User.QUERY_FIELDS_WITH_U_PREFIX
          + QUERY_FIELD_SEPARATOR
          + " r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id "
          + " FROM users u "
          + " INNER JOIN user_role ur ON ur.user_id = u.user_id AND ur.dac_id = :dacId "
          + " INNER JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE r.name = 'Chairperson' OR r.name = 'Member'")
  Set<User> findUsersEnabledToVoteByDAC(@Bind("dacId") Integer dacId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "select "
          + User.QUERY_FIELDS_WITH_U_PREFIX
          + QUERY_FIELD_SEPARATOR
          + " r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id "
          + " FROM users u "
          + " INNER JOIN user_role ur ON ur.user_id = u.user_id AND ur.dac_id is null "
          + " INNER JOIN roles r on r.role_id = ur.role_id "
          + " WHERE r.name = 'Chairperson' OR r.name = 'Member'")
  Set<User> findNonDacUsersEnabledToVote();

  @UseRowMapper(UserWithRolesMapper.class)
  @SqlQuery(
      "select du.*, r.role_id, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from users du inner join user_role ur on ur.user_id = du.user_id inner join roles r on r.role_id = ur.role_id where  du.user_id IN (<userIds>)")
  Set<User> findUsersWithRoles(
      @BindList(value = "userIds", onEmpty = EmptyHandling.NULL_STRING)
          Collection<Integer> userIds);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class, prefix = "ur")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      """
      SELECT
          u.user_id as u_user_id,
          u.email as u_email,
          u.display_name as u_display_name,
          u.create_date as u_create_date,
          u.email_preference as u_email_preference,
          u.institution_id as u_institution_id,
          u.era_commons_id as u_era_commons_id,
          i.institution_id as i_id,
          i.institution_name as i_name,
          i.it_director_name as i_it_director_name,
          i.it_director_email as i_it_director_email,
          i.create_date as i_create_date,
          i.update_date as i_update_date,
          ur.user_role_id as ur_user_role_id, ur.user_id as ur_user_id,
          ur.role_id as ur_role_id, ur.dac_id as ur_dac_id, r.name as ur_name,
          lc.id AS lc_id, lc.user_id AS lc_user_id,
          lc.user_name AS lc_user_name, lc.user_email AS lc_user_email,
          lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date,
          lc.update_user_id AS lc_update_user_id,
          ld.daa_id as lc_daa_id
      FROM users u
      LEFT JOIN user_role ur ON ur.user_id = u.user_id
      LEFT JOIN roles r ON r.role_id = ur.role_id
      LEFT JOIN institution i ON u.institution_id = i.institution_id
      LEFT JOIN library_card lc ON lc.user_id = u.user_id
      LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
      WHERE LOWER(u.email) = LOWER(:email)
      """)
  User findUserByEmail(@Bind("email") String email);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @SqlQuery(
      """
      SELECT
          u.user_id as u_user_id,
          u.email as u_email,
          u.display_name as u_display_name,
          u.create_date as u_create_date,
          u.email_preference as u_email_preference,
          u.institution_id as u_institution_id,
          u.era_commons_id as u_era_commons_id
      FROM users u
      LEFT OUTER JOIN email_entity ee ON ee.user_id = u.user_id AND ee.email_type = :emailType AND ee.entity_reference_id = :referenceId
      WHERE u.email_preference = true
        AND ee.entity_reference_id IS NULL
      """)
  ResultIterable<User> allEmailReceivingThinlyPopulatedUsers(
      @Bind("emailType") Integer emailType, @Bind("referenceId") String referenceId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class, prefix = "ur")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      """
      SELECT
          u.user_id as u_user_id, u.email as u_email, u.display_name as u_display_name,
          u.create_date as u_create_date, u.email_preference as u_email_preference,
          u.institution_id as u_institution_id, u.era_commons_id as u_era_commons_id,
          ur.user_role_id as ur_user_role_id, ur.user_id as ur_user_id,
          ur.role_id as ur_role_id, ur.dac_id as ur_dac_id, r.name as ur_name
      FROM users u
      LEFT JOIN user_role ur ON ur.user_id = u.user_id
      LEFT JOIN roles r ON r.role_id = ur.role_id
      WHERE LOWER(u.email) ILIKE ANY (array[<emails>])
      """)
  List<User> findUsersByEmailList(
      @BindList(value = "emails", onEmpty = EmptyHandling.NULL_STRING) List<String> emails);

  @SqlUpdate(
      "INSERT INTO users (email, display_name, institution_id, create_date) values (:email, :displayName, :institutionId, :createDate)")
  @GetGeneratedKeys
  Integer insertUser(
      @Bind("email") String email,
      @Bind("displayName") String displayName,
      @Bind("institutionId") Integer institutionId,
      @Bind("createDate") Date createDate);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      // This will pull in users tied to the institution
      // Users will come with LCs issued by SOs institution (if any)
      """
          SELECT DISTINCT
          u.user_id as u_user_id,
          u.email as u_email,
          u.display_name as u_display_name,
          u.create_date as u_create_date,
          u.email_preference as u_email_preference,
          u.institution_id as u_institution_id,
          u.era_commons_id as u_era_commons_id,
          r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id,
          lc.id AS lc_id , lc.user_id AS lc_user_id,
          lc.user_name AS lc_user_name, lc.user_email AS lc_user_email,
          lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date,
          lc.update_user_id AS lc_update_user_id,
          ld.daa_id as lc_daa_id,
          i.institution_id as i_id,
          i.institution_name as i_name,
          i.it_director_name as i_it_director_name,
          i.it_director_email as i_it_director_email,
          i.create_date as i_create_date,
          i.update_date as i_update_date
          FROM users u
          LEFT JOIN user_role ur ON ur.user_id = u.user_id
          LEFT JOIN roles r ON r.role_id = ur.role_id
          LEFT JOIN library_card lc ON lc.user_id = u.user_id
          LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
          LEFT JOIN institution i ON u.institution_id = i.institution_id
        """)
  List<User> findUsersWithLCsAndInstitution();

  @UseRowMapper(UserWithRolesMapper.class)
  @SqlQuery(
      """
      SELECT u.*, r.role_id, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id
      FROM users u
      INNER JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id = :roleId
      INNER JOIN roles r ON r.role_id = ur.role_id
    """)
  List<User> findUsersByRoleId(@Bind("roleId") Integer roleId);

  @UseRowMapper(UserWithRolesMapper.class)
  @SqlQuery(
      """
      SELECT u.*, r.role_id, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id
      FROM users u
      INNER JOIN user_role ur ON ur.user_id = u.user_id AND ur.role_id in (<roleIds>)
      INNER JOIN roles r on r.role_id = ur.role_id
      INNER JOIN dac d ON d.dac_id = ur.dac_id
      INNER JOIN dataset ds ON ds.dac_id = d.dac_id
      WHERE ds.dataset_id in (<datasetIds>)
    """)
  Set<User> findUsersForDatasetsByRole(
      @BindList(value = "datasetIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> datasetIds,
      @BindList(value = "roleIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> roleIds);

  @RegisterBeanMapper(value = User.class)
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT du.*, r.name, ur.role_id, ur.user_role_id, ur.dac_id "
          + " FROM users du "
          + " LEFT JOIN user_role ur ON ur.user_id = du.user_id "
          + " LEFT JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE du.institution_id = :institutionId")
  List<User> findUsersByInstitution(@Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      // This will pull in users tied to the institution
      // Users will come with LCs issued by SOs institution (if any)
      """
          SELECT
          u.user_id as u_user_id,
          u.email as u_email,
          u.display_name as u_display_name,
          u.create_date as u_create_date,
          u.email_preference as u_email_preference,
          u.institution_id as u_institution_id,
          u.era_commons_id as u_era_commons_id,
          r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id,
          lc.id AS lc_id , lc.user_id AS lc_user_id,
          lc.user_name AS lc_user_name, lc.user_email AS lc_user_email,
          lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date,
          lc.update_user_id AS lc_update_user_id,
          ld.daa_id as lc_daa_id,
          i.institution_id as i_id,
          i.institution_name as i_name,
          i.it_director_name as i_it_director_name,
          i.it_director_email as i_it_director_email,
          i.create_date as i_create_date,
          i.update_date as i_update_date
          FROM users u
          LEFT JOIN user_role ur ON ur.user_id = u.user_id
          LEFT JOIN roles r ON r.role_id = ur.role_id
          LEFT JOIN library_card lc ON lc.user_id = u.user_id
          LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
          LEFT JOIN institution i ON u.institution_id = i.institution_id
          WHERE u.institution_id = :institutionId
        """)
  List<User> getUsersFromInstitutionWithCards(@Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      """
          SELECT DISTINCT ON (u.user_id)
          u.user_id as u_user_id,
          u.email as u_email,
          u.display_name as u_display_name,
          u.create_date as u_create_date,
          u.email_preference as u_email_preference,
          u.institution_id as u_institution_id,
          u.era_commons_id as u_era_commons_id,
          r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id,
          lc.id AS lc_id , lc.user_id AS lc_user_id,
          lc.user_name AS lc_user_name, lc.user_email AS lc_user_email,
          lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date,
          lc.update_user_id AS lc_update_user_id,
          ld.daa_id as lc_daa_id,
          i.institution_id as i_id,
          i.institution_name as i_name,
          i.it_director_name as i_it_director_name,
          i.it_director_email as i_it_director_email,
          i.create_date as i_create_date,
          i.update_date as i_update_date
          FROM users u
          LEFT JOIN user_role ur ON ur.user_id = u.user_id
          LEFT JOIN roles r ON r.role_id = ur.role_id
          LEFT JOIN library_card lc ON lc.user_id = u.user_id
          LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
          LEFT JOIN institution i ON u.institution_id = i.institution_id
          WHERE ld.daa_id = :daaId
        """)
  List<User> getUsersWithCardsByDaaId(@Bind("daaId") Integer daaId);

  @RegisterBeanMapper(value = User.class)
  @SqlQuery(
      "SELECT u.user_id, u.display_name, u.email FROM users u "
          + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
          + " LEFT JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE LOWER(r.name) = 'signingofficial' "
          + " AND u.institution_id = :institutionId")
  List<User> getSOsByInstitution(@Bind("institutionId") Integer institutionId);

  @SqlUpdate("update users set email_preference = :emailPreference WHERE user_id = :userId")
  void updateEmailPreference(
      @Bind("userId") Integer userId, @Bind("emailPreference") Boolean emailPreference);

  @SqlUpdate("UPDATE users SET era_commons_id = :eraCommonsId WHERE user_id = :userId")
  void updateEraCommonsId(
      @Bind("userId") Integer userId, @Bind("eraCommonsId") String eraCommonsId);

  @SqlUpdate("UPDATE users SET institution_id = :institutionId WHERE user_id = :userId")
  void updateInstitutionId(
      @Bind("userId") Integer userId, @Bind("institutionId") Integer institutionId);

  @SqlUpdate("UPDATE users SET display_name = :displayName WHERE user_id = :userId")
  void updateDisplayName(@Bind("userId") Integer userId, @Bind("displayName") String displayName);
}
