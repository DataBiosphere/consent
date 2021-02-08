package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.mapper.UserMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(UserMapper.class)
public interface UserDAO extends Transactional<UserDAO> {

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleid, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id " +
            " from dacuser du " +
            " left join user_role ur on ur.user_id = du.dacuserid " +
            " left join roles r on r.roleid = ur.role_id " +
            " where du.dacuserid = :dacUserId")
    User findUserById(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from dacuser where dacUserId IN (<dacUserIds>)")
    Collection<User> findUsers(@BindList("dacUserIds") Collection<Integer> dacUserIds);

    @SqlQuery("select du.* from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = :roleName")
    List<User> describeUsersByRole(@Bind("roleName") String roleName);

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

    @SqlQuery("select * from dacuser where email = :email")
    User findUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser (email, displayName, createDate) values (:email, :displayName, :createDate)")
    @GetGeneratedKeys
    Integer insertUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("createDate") Date createDate);

    @SqlUpdate("update dacuser set email=:email, displayName=:displayName, additional_email=:additionalEmail where dacUserId=:id")
    void updateUser(@Bind("email") String email,
                       @Bind("displayName") String displayName,
                       @Bind("id") Integer id,
                       @Bind("additionalEmail") String additionalEmail);

    @SqlUpdate("delete from dacuser where email = :email")
    void deleteUserByEmail(@Bind("email") String email);

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

    @SqlQuery("select * from dacuser du "
            + " inner join user_role ur on du.dacUserId = ur.user_id "
            + " inner join roles r on ur.role_id = r.roleId "
            + " where du.email = :email "
            + " and r.roleId = :roleId")
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

}
