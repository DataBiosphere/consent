package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.models.User;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

@UseStringTemplate3StatementLocator
@RegisterMapper({UserMapper.class})
public interface UserDAO extends Transactional<UserDAO> {

    @SqlQuery("select * from dacuser where dacUserId = :userId")
    User findUserById(@Bind("userId") Integer userId);

    @SqlQuery("select * from dacuser where dacUserId IN (<userIds>)")
    Collection<User> findUsers(@BindIn("userIds") Collection<Integer> userIds);

    @Mapper(UserToUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name from dacuser u inner join user_role du on du.user_id = u.dacUserId inner join roles r on r.roleId = du.role_id where r.name = 'Chairperson'")
    User findChairpersonUser();

    @SqlQuery("select u.* from dacuser u inner join user_role du on du.user_id = u.dacUserId inner join roles r on r.roleId = du.role_id where r.name = :roleName")
    List<User> describeUsersByRole(@Bind("roleName") String roleName);

    @SqlQuery("select u.dacUserId from dacuser u inner join user_role du on du.user_id = u.dacUserId inner join roles r on r.roleId = du.role_id where u.dacUserId = :userId and r.name = 'Chairperson'")
    Integer checkChairpersonUser(@Bind("userId") Integer userId);

    @Mapper(UserToUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name from dacuser u inner join user_role du on du.user_id = u.dacUserId inner join roles r on r.roleId = du.role_id where r.name = 'Chairperson' or r.name = 'Member'")
    Set<User> findUsersEnabledToVote();

    @Mapper(UserToUserRoleMapper.class)
    @SqlQuery("select u.*,r.roleId, r.name, du.status from dacuser u inner join user_role du on du.user_id = u.dacUserId inner join roles r on r.roleId = du.role_id where  u.dacUserId IN (<userIds>)")
    Set<User> findUsersWithRoles(@BindIn("userIds") Collection<Integer> userIds);

    @SqlQuery("select * from dacuser where email = :email")
    User findUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser (email, displayName, createDate) values (:email, :displayName, :createDate)")
    @GetGeneratedKeys
    Integer insertUser(@Bind("email") String email,
                       @Bind("displayName") String displayName,
                       @Bind("createDate") Date createDate);

    @SqlUpdate("update dacuser set email=:email, displayName=:displayName, additional_email=:additionalEmail where dacUserId=:userId")
    void updateUser(@Bind("email") String email,
                    @Bind("displayName") String displayName,
                    @Bind("userId") Integer userId,
                    @Bind("additionalEmail") String additionalEmail);

    @SqlUpdate("delete from dacuser where email = :email")
    void deleteUserByEmail(@Bind("email") String email);

    // TODO: This query can return many user ids, not a single one. See: DUOS-392
    @SqlQuery("select dr.user_id from user_role dr inner join roles r on r.roleId = dr.role_id where dr.user_id != :userId and r.roleId = :roleId")
    Integer findUserIdByRole(@Bind("roleId") Integer roleId, @Bind("userId") Integer userId);

    @Mapper(UserToUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name, du.status from dacuser u inner join user_role du on du.user_id = u.dacUserId " +
              "inner join roles r on r.roleId = du.role_id order by createDate desc")
    Set<User> findUsers();

    @SqlQuery("select count(*) from user_role dr inner join roles r on r.roleId = dr.role_id where r.name = 'Admin'")
    Integer verifyAdminUsers();

    @Mapper(UserToUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name, du.status from dacuser u inner join user_role du on du.user_id = u.dacUserId inner join roles r on r.roleId = du.role_id where r.name = :roleName and du.email_preference = :emailPreference")
    List<User> describeUsersByRoleAndEmailPreference(@Bind("roleName") String roleName, @Bind("emailPreference") Boolean emailPreference);

    @SqlQuery("Select * from dacuser d where d.dacUserId NOT IN "
            + "("
            + "    Select v.dacUserId From vote v Where v.electionId IN "
            + "    ("
            + "        Select e.electionId from election e where e.electionType != 'DataSet' and (e.status = 'Open' or e.status = 'Final')"
            + "        and exists "
            + "        ("
            + "            Select * from vote v where v.electionId = e.electionId and v.dacUserId = :userId"
            + "        )"
            + "        and "
            + "        ( "
            + "            (select count(*) from vote v2 where v2.electionId = e.electionId and v2.type = 'DAC') \\< 5 "
            + "        )"
            + "    ) "
            + "group by v.dacUserId"
            + ") AND d.dacUserId NOT IN (Select ur.user_id from user_role ur where ur.role_id in (<roleIds>))")
    List<User> getMembersApprovedToReplace(@Bind("userId") Integer userId, @BindIn("roleIds") List<Integer> includedRoles);

    @SqlQuery("SELECT * FROM dacuser du "
            + " INNER JOIN user_role ur ON du.dacUserId = ur.user_id "
            + " INNER JOIN roles r ON ur.role_id = r.roleId "
            + " WHERE du.dacUserId != :userId "
            + " AND r.name = 'DataOwner'")
    List<User> getDataOwnersApprovedToReplace(@Bind("userId") Integer userId);

    @SqlUpdate("update dacuser set displayName=:displayName where dacUserId = :userId")
    void updateUser(@Bind("displayName") String displayName,
                    @Bind("userId") Integer userId);
}
