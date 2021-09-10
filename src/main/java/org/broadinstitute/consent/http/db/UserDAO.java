package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.UnregisteredUsersWithCardsReducer;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesReducer;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
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

@SuppressWarnings("SqlDialectInspection")
public interface UserDAO extends Transactional<UserDAO> {

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @RegisterBeanMapper(value = Institution.class, prefix = "i")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + "     u.dacuserid, u.email, u.displayname, u.createdate, u.additional_email, "
        + "     u.email_preference, u.status, u.rationale, u.institution_id, "
        + "     u.era_commons_id, "
        + Institution.QUERY_FIELDS_WITH_I_PREFIX + ", "
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name  "
        + " FROM dacuser u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.dacuserid "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " LEFT JOIN institution i ON u.institution_id = i.institution_id"
        + " WHERE u.dacuserid = :dacUserId")
    User findUserById(@Bind("dacUserId") Integer dacUserId);

    @RegisterBeanMapper(value = User.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("select * from dacuser where dacUserId IN (<dacUserIds>)")
    Collection<User> findUsers(@BindList("dacUserIds") Collection<Integer> dacUserIds);

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + "     u.dacuserid, u.email, u.displayname, u.createdate, u.additional_email, "
        + "     u.email_preference, u.status, u.rationale, u.institution_id, "
        + "     u.era_commons_id, "
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
        + " FROM dacuser u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.dacuserid "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE r.name = :name")
    List<User> describeUsersByRole(@Bind("name") String name);

    @SqlQuery("select du.dacUserId from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where du.dacUserId = :dacUserId and r.name = 'Chairperson'")
    Integer checkChairpersonUser(@Bind("dacUserId") Integer dacUserId);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id = :dacId inner join roles r on r.roleId = ur.role_id where r.name = 'Chairperson' or r.name = 'Member'")
    Set<User> findUsersEnabledToVoteByDAC(@Bind("dacId") Integer dacId);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id is null inner join roles r on r.roleId = ur.role_id where r.name = 'Chairperson' or r.name = 'Member'")
    Set<User> findNonDacUsersEnabledToVote();

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where  du.dacUserId IN (<dacUserIds>)")
    Set<User> findUsersWithRoles(@BindList("dacUserIds") Collection<Integer> dacUserIds);

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + "     u.dacuserid, u.email, u.displayname, u.createdate, u.additional_email, "
        + "     u.email_preference, u.status, u.rationale, u.institution_id, "
        + "     u.era_commons_id, "
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
        + " FROM dacuser u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.dacuserid "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE LOWER(u.email) = LOWER(:email)")
    User findUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser (email, displayName, createDate) values (:email, :displayName, :createDate)")
    @GetGeneratedKeys
    Integer insertUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("createDate") Date createDate);

    @SqlUpdate("UPDATE dacuser SET displayname=:displayName, additional_email=:additionalEmail, institution_id=:institutionId WHERE dacuserid=:id")
    void updateUser(@Bind("displayName") String displayName,
                       @Bind("id") Integer id,
                       @Bind("additionalEmail") String additionalEmail,
                       @Bind("institutionId") Integer institutionId);

    @SqlUpdate("delete from dacuser where dacuserid = :id")
    void deleteUserById(@Bind("id") Integer id);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("SELECT du.*, r.roleid, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, p.propertyvalue AS completed " +
            " FROM dacuser du " +
            " LEFT JOIN user_role ur ON ur.user_id = du.dacuserid " +
            " LEFT JOIN roles r ON r.roleid = ur.role_id " +
            " LEFT JOIN user_property p ON p.userid = du.dacuserid AND lower(propertykey) = 'completed' " +
            " ORDER BY createdate DESC ")
    Set<User> findUsers();

    @SqlQuery("select count(*) from user_role dr inner join roles r on r.roleId = dr.role_id where r.name = 'Admin'")
    Integer verifyAdminUsers();

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = :roleName and du.email_preference = :emailPreference")
    List<User> describeUsersByRoleAndEmailPreference(@Bind("roleName") String roleName, @Bind("emailPreference") Boolean emailPreference);

    @SqlUpdate("update dacuser set email_preference = :emailPreference where dacUserId = :userId")
    void updateEmailPreference(@Bind("emailPreference") Boolean emailPreference, @Bind("userId") Integer userId);

    @SqlUpdate("update dacuser set status = :status where dacUserId = :userId")
    void updateUserStatus(@Bind("status") Integer status, @Bind("userId") Integer userId);

    @SqlUpdate("update dacuser set rationale = :rationale where dacUserId = :userId")
    void updateUserRationale(@Bind("rationale") String rationale, @Bind("userId") Integer userId);

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + "     u.dacuserid, u.email, u.displayname, u.createdate, u.additional_email, "
        + "     u.email_preference, u.status, u.rationale, u.institution_id, "
        + "     u.era_commons_id, "
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
        + " FROM dacuser u "
        + " LEFT JOIN user_role ur ON ur.user_id = u.dacuserid "
        + " LEFT JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE LOWER(u.email) = LOWER(:email) "
        + " AND r.roleid = :roleId")
    User findUserByEmailAndRoleId(@Bind("email") String email, @Bind("roleId") Integer roleId);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id " +
            " from dacuser du " +
            " inner join user_role ur on ur.user_id = du.dacUserId " +
            " inner join roles r on r.roleId = ur.role_id and r.name in (<roleNames>) " +
            " inner join vote v on v.dacUserId = du.dacUserId and v.electionId in (<electionIds>) ")
    Set<User> findUsersForElectionsByRoles(@BindList("electionIds") List<Integer> electionIds,
                                           @BindList("roleNames") List<String> roleNames);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id " +
            " from dacuser du " +
            " inner join user_role ur on ur.user_id = du.dacUserId " +
            " inner join roles r on r.roleId = ur.role_id and r.name in (<roleNames>) " +
            " inner join dac d on d.dac_id = ur.dac_id " +
            " inner join consents c on c.dac_id = d.dac_id " +
            " inner join consentassociations a on a.consentId = c.consentId " +
            " where a.dataSetId in (<datasetIds>) "
    )
    Set<User> findUsersForDatasetsByRole(
            @BindList("datasetIds") List<Integer> datasetIds,
            @BindList("roleNames") List<String> roleNames);

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT du.*, r.name, ur.role_id, ur.user_role_id, ur.dac_id "
            + " FROM dacuser du "
            + " LEFT JOIN user_role ur ON ur.user_id = du.dacuserid " + " LEFT JOIN roles r ON r.roleid = ur.role_id "
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

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
    @RegisterBeanMapper(value = Institution.class, prefix = "lci")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery(
            " SELECT du.*, r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id, "
          + " lc.id AS lc_id , lc.user_id AS lc_user_id, lc.institution_id AS lc_institution_id, "
          + " lc.era_commons_id AS lc_era_commons_id, lc.user_name AS lc_user_name, lc.user_email AS lc_user_email, "
          + " lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date, "
          + " lc.update_user_id AS lc_update_user_id, "
          + Institution.QUERY_FIELDS_WITH_LCI_PREFIX
          + " FROM dacuser du"
          + " LEFT JOIN user_role ur ON ur.user_id = du.dacuserid "
          + " LEFT JOIN roles r ON r.roleid = ur.role_id "
          + " INNER JOIN library_card lc ON lc.user_id = du.dacuserid "
          + " LEFT JOIN institution lci ON lc.institution_id = lci.institution_id"
          + " WHERE (du.institution_id != :institutionId OR du.institution_id IS NULL) AND lc.institution_id = :institutionId")
    List<User> getUsersOutsideInstitutionWithCards(@Bind("institutionId") Integer institutionId);

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
    @RegisterBeanMapper(value = Institution.class, prefix = "i")
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery(
            //This will pull in users tied to the institution
            //Users will come with LCs issued by SOs institution (if any)
            " SELECT du.*, r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id, " +
                " lc.id AS lc_id , lc.user_id AS lc_user_id, lc.institution_id AS lc_institution_id, " +
                " lc.era_commons_id AS lc_era_commons_id, lc.user_name AS lc_user_name, lc.user_email AS lc_user_email, " +
                " lc.create_user_id AS lc_create_user_id, lc.create_date AS lc_create_date, " +
                " lc.update_user_id AS lc_update_user_id, " +
                Institution.QUERY_FIELDS_WITH_I_PREFIX +
            " FROM dacuser du " +
            " LEFT JOIN user_role ur ON ur.user_id = du.dacuserid " +
            " LEFT JOIN roles r ON r.roleid = ur.role_id " +
            " LEFT JOIN library_card lc ON lc.user_id = du.dacuserid AND lc.institution_id = :institutionId " +
            " LEFT JOIN institution i ON du.institution_id = i.institution_id" +
            " WHERE du.institution_id = :institutionId")
    List<User> getUsersFromInstitutionWithCards(@Bind("institutionId") Integer institutionId);

    @RegisterBeanMapper(value = User.class)

    //SO only endpoint (so far)
    //Meant to pull in users that have not yet been assigned an institution
    //(SOs can assign LCs to these users as well)
    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery(" SELECT du.*, r.name, ur.role_id, ur.user_role_id, ur.dac_id, ur.user_id " +
                " FROM dacuser du " +
                " LEFT JOIN user_role ur ON ur.user_id = du.dacuserid " +
                " LEFT JOIN roles r ON r.roleid = ur.role_id " +
                " WHERE du.institution_id IS NULL")
    List<User> getUsersWithNoInstitution();

    @RegisterBeanMapper(value = User.class)
    @SqlQuery("SELECT u.dacuserid, u.displayname, u.email FROM dacuser u "
      + " LEFT JOIN user_role ur ON ur.user_id = u.dacuserid "
      + " LEFT JOIN roles r ON r.roleid = ur.role_id "
      + " WHERE LOWER(r.name) = 'signingofficial' "
      + " AND u.institution_id = :institutionId")
    List<User> getSOsByInstitution(@Bind("institutionId") Integer institutionId);

    @SqlUpdate("UPDATE dacuser SET " +
      " era_commons_id = :eraCommonsId " +
      " WHERE dacuserid = :userId")
    void updateEraCommonsId(@Bind("userId") Integer userId, @Bind("eraCommonsId") String eraCommonsId);

}
