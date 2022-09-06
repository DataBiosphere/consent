package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.UnregisteredUsersWithCardsReducer;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesReducer;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface UserDAO extends Transactional<UserDAO> {

    String QUERY_FIELD_SEPARATOR = ", ";

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @RegisterBeanMapper(value = Institution.class, prefix = "i")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
        + Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name  "
        + " FROM users u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " LEFT JOIN institution i ON u.institution_id = i.institution_id"
        + " WHERE u.user_id = :userId")
    User findUserById(@Bind("userId") Integer userId);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + User.QUERY_FIELDS_WITH_U_PREFIX
        + " FROM users u WHERE u.user_id IN (<userIds>)")
    Collection<User> findUsers(@BindList("userIds") Collection<Integer> userIds);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
        + " FROM users u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE r.name = :name")
    List<User> describeUsersByRole(@Bind("name") String name);

    @SqlQuery("select du.user_id from users du inner join user_role ur on ur.user_id = du.user_id inner join roles r on r.roleId = ur.role_id where du.user_id = :userId and r.name = 'Chairperson'")
    Integer checkChairpersonUser(@Bind("userId") Integer userId);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
        + " r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id "
        + " FROM users u "
        + " INNER JOIN user_role ur ON ur.user_id = u.user_id AND ur.dac_id = :dacId "
        + " INNER JOIN roles r ON r.roleId = ur.role_id "
        + " WHERE r.name = 'Chairperson' OR r.name = 'Member'")
    Set<User> findUsersEnabledToVoteByDAC(@Bind("dacId") Integer dacId);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("select "
        + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
        + " r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id "
        + " FROM users u "
        + " INNER JOIN user_role ur ON ur.user_id = u.user_id AND ur.dac_id is null "
        + " INNER JOIN roles r on r.roleId = ur.role_id "
        + " WHERE r.name = 'Chairperson' OR r.name = 'Member'")
    Set<User> findNonDacUsersEnabledToVote();

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from users du inner join user_role ur on ur.user_id = du.user_id inner join roles r on r.roleId = ur.role_id where  du.user_id IN (<userIds>)")
    Set<User> findUsersWithRoles(@BindList("userIds") Collection<Integer> userIds);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class, prefix = "ur")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
        + "     ur.user_role_id as ur_user_role_id, ur.user_id as ur_user_id, "
        + "     ur.role_id as ur_role_id, ur.dac_id as ur_dac_id, r.name as ur_name "
        + " FROM users u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE LOWER(u.email) = LOWER(:email)")
    User findUserByEmail(@Bind("email") String email);

    @SqlUpdate("INSERT INTO users (email, display_name, create_date) values (:email, :displayName, :createDate)")
    @GetGeneratedKeys
    Integer insertUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("createDate") Date createDate);

    @SqlUpdate("UPDATE users SET display_name=:displayName, institution_id=:institutionId WHERE user_id=:id")
    void updateUser(@Bind("displayName") String displayName,
                       @Bind("id") Integer id,
                       @Bind("institutionId") Integer institutionId);

    @SqlUpdate("delete from users where user_id = :id")
    void deleteUserById(@Bind("id") Integer id);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name, "
        + "     p.propertyid AS up_property_id, p.propertykey AS up_property_key, p.propertyvalue AS up_property_value, p.userid AS up_user_id "
        + " FROM users u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " LEFT JOIN user_property p ON p.userid = u.user_id "
        + " ORDER BY u.create_date DESC ")
    Set<User> findUsers();

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from users du inner join user_role ur on ur.user_id = du.user_id inner join roles r on r.roleId = ur.role_id where r.name = :roleName and du.email_preference = :emailPreference")
    List<User> describeUsersByRoleAndEmailPreference(@Bind("roleName") String roleName, @Bind("emailPreference") Boolean emailPreference);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
        + " FROM users u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE LOWER(u.email) = LOWER(:email) "
        + " AND r.roleid = :roleId")
    User findUserByEmailAndRoleId(@Bind("email") String email, @Bind("roleId") Integer roleId);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id " +
            " from users du " +
            " inner join user_role ur on ur.user_id = du.user_id " +
            " inner join roles r on r.roleId = ur.role_id and r.name in (<roleNames>) " +
            " inner join vote v on v.dacUserId = du.user_id and v.electionId in (<electionIds>) ")
    Set<User> findUsersForElectionsByRoles(@BindList("electionIds") List<Integer> electionIds,
                                           @BindList("roleNames") List<String> roleNames);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id " +
            " from users du " +
            " inner join user_role ur on ur.user_id = du.user_id " +
            " inner join roles r on r.roleId = ur.role_id and r.name in (<roleNames>) " +
            " inner join dac d on d.dac_id = ur.dac_id " +
            " inner join dataset ds on ds.dac_id = d.dac_id " +
            " where ds.dataset_id in (<datasetIds>) "
    )
    Set<User> findUsersForDatasetsByRole(
            @BindList("datasetIds") List<Integer> datasetIds,
            @BindList("roleNames") List<String> roleNames);

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT du.*, r.name, ur.role_id, ur.user_role_id, ur.dac_id "
            + " FROM users du "
            + " LEFT JOIN user_role ur ON ur.user_id = du.user_id " + " LEFT JOIN roles r ON r.roleid = ur.role_id "
            + " WHERE du.institution_id = :institutionId")
    List<User> findUsersByInstitution(@Bind("institutionId") Integer institutionId);

    @RegisterBeanMapper(value = LibraryCard.class)
    @RegisterBeanMapper(value = Institution.class, prefix = "lci")
    @UseRowReducer(UnregisteredUsersWithCardsReducer.class)
    @SqlQuery(" SELECT lc.*, " +
            Institution.QUERY_FIELDS_WITH_LCI_PREFIX +
            " FROM library_card lc " +
            " LEFT JOIN institution lci ON lc.institution_id = lci.institution_id" +
            " WHERE lc.user_id IS NULL " +
            " AND lc.institution_id = :institutionId")
    List<User> getCardsForUnregisteredUsers(@Bind("institutionId") Integer institutionId);

    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
    @RegisterBeanMapper(value = Institution.class, prefix = "lci")
    @RegisterBeanMapper(value = Institution.class, prefix = "i")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery(
            //This will pull in users tied to the institution
            //Users will come with LCs issued by SOs institution (if any)
            " SELECT " +
                User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR +
                " r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id, " +
                " lc.id AS lc_id , lc.user_id AS lc_user_id, lc.institution_id AS lc_institution_id, " +
                " lc.era_commons_id AS lc_era_commons_id, lc.user_name AS lc_user_name, lc.user_email AS lc_user_email, " +
                " lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date, " +
                " lc.update_user_id AS lc_update_user_id, " +
                Institution.QUERY_FIELDS_WITH_LCI_PREFIX + ", " +
                Institution.QUERY_FIELDS_WITH_I_PREFIX +
            " FROM users u " +
            " LEFT JOIN user_role ur ON ur.user_id = u.user_id " +
            " LEFT JOIN roles r ON r.roleid = ur.role_id " +
            " LEFT JOIN library_card lc ON lc.user_id = u.user_id AND lc.institution_id = :institutionId " +
            " LEFT JOIN institution lci ON lc.institution_id = lci.institution_id" +
            " LEFT JOIN institution i ON u.institution_id = i.institution_id" +
            " WHERE u.institution_id = :institutionId")
    List<User> getUsersFromInstitutionWithCards(@Bind("institutionId") Integer institutionId);

    @RegisterBeanMapper(value = User.class)

    //SO only endpoint (so far)
    //Meant to pull in users that have not yet been assigned an institution
    //(SOs can assign LCs to these users as well)
    @RegisterBeanMapper(value = User.class, prefix = "u")
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery(" SELECT " +
                User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR +
                " r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id " +
                " FROM users u " +
                " LEFT JOIN user_role ur ON ur.user_id = u.user_id " +
                " LEFT JOIN roles r ON r.roleid = ur.role_id " +
                " WHERE u.institution_id IS NULL")
    List<User> getUsersWithNoInstitution();

    @RegisterBeanMapper(value = User.class)
    @SqlQuery("SELECT u.user_id, u.display_name, u.email FROM users u "
      + " LEFT JOIN user_role ur ON ur.user_id = u.user_id "
      + " LEFT JOIN roles r ON r.roleid = ur.role_id "
      + " WHERE LOWER(r.name) = 'signingofficial' "
      + " AND u.institution_id = :institutionId")
    List<User> getSOsByInstitution(@Bind("institutionId") Integer institutionId);

    @SqlUpdate("update users set email_preference = :emailPreference WHERE user_id = :userId")
    void updateEmailPreference(@Bind("userId") Integer userId, @Bind("emailPreference") Boolean emailPreference);

    @SqlUpdate("UPDATE users SET era_commons_id = :eraCommonsId WHERE user_id = :userId")
    void updateEraCommonsId(@Bind("userId") Integer userId, @Bind("eraCommonsId") String eraCommonsId);

    @SqlUpdate("UPDATE users SET institution_id = :institutionId WHERE user_id = :userId")
    void updateInstitutionId(@Bind("userId") Integer userId, @Bind("institutionId") Integer institutionId);

    @SqlUpdate("UPDATE users SET display_name = :displayName WHERE user_id = :userId")
    void updateDisplayName(@Bind("userId") Integer userId, @Bind("displayName") String displayName);

}
