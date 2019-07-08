package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DACUser;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

// TODO: Rename this class to UserDAO - see DUOS-344
@UseStringTemplate3StatementLocator
@RegisterMapper({DACUserMapper.class})
public interface DACUserDAO extends Transactional<DACUserDAO> {

    @SqlQuery("select * from dacuser where dacUserId = :userId")
    DACUser findDACUserById(@Bind("userId") Integer userId);

    @SqlQuery("select * from dacuser where dacUserId IN (<userIds>)")
    Collection<DACUser> findUsers(@BindIn("userIds") Collection<Integer> userIds);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name from dacuser u inner join user_role ur on ur.user_id = u.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = 'Chairperson'")
    DACUser findChairpersonUser();

    @SqlQuery("select u.* from dacuser u inner join user_role ur on ur.user_id = u.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = :roleName")
    List<DACUser> describeUsersByRole(@Bind("roleName") String roleName);

    @SqlQuery("select u.dacUserId from dacuser u inner join user_role ur on ur.user_id = u.dacUserId inner join roles r on r.roleId = ur.role_id where u.dacUserId = :userId and r.name = 'Chairperson'")
    Integer checkChairpersonUser(@Bind("userId") Integer userId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name from dacuser u inner join user_role ur on ur.user_id = u.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = 'Chairperson' or r.name = 'Member'")
    Set<DACUser> findDACUsersEnabledToVote();

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name, ur.status from dacuser u inner join user_role ur on ur.user_id = u.dacUserId inner join roles r on r.roleId = ur.role_id where  u.dacUserId IN (<userIds>)")
    Set<DACUser> findUsersWithRoles(@BindIn("userIds") Collection<Integer> userIds);

    @SqlQuery("select * from dacuser where email = :email")
    DACUser findDACUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser (email, displayName, createDate) values (:email, :displayName, :createDate)")
    @GetGeneratedKeys
    Integer insertDACUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("createDate") Date createDate);

    @SqlUpdate("update dacuser set email=:email, displayName=:displayName, additional_email=:additionalEmail where dacUserId = :userId")
    void updateDACUser(@Bind("email") String email,
                       @Bind("displayName") String displayName,
                       @Bind("userId") Integer userId,
                       @Bind("additionalEmail") String additionalEmail);

    @SqlUpdate("delete from dacuser where email = :email")
    void deleteDACUserByEmail(@Bind("email") String email);

    // TODO: This query can return many user ids, not a single one. See: DUOS-392
    @SqlQuery("select dr.user_id from user_role dr inner join roles r on r.roleId = dr.role_id where dr.user_id != :userId and r.roleId = :roleId")
    Integer findDACUserIdByRole(@Bind("roleId") Integer roleId, @Bind("userId") Integer userId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name, ur.status from dacuser u inner join user_role ur on ur.user_id = u.dacUserId " +
              "inner join roles r on r.roleId = ur.role_id order by createDate desc")
    Set<DACUser> findUsers();

    @SqlQuery("select count(*) from user_role dr inner join roles r on r.roleId = dr.role_id where r.name = 'Admin'")
    Integer verifyAdminUsers();

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name, ur.status from dacuser u inner join user_role ur on ur.user_id = u.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = :roleName and u.email_preference = :emailPreference")
    List<DACUser> describeUsersByRoleAndEmailPreference(@Bind("roleName") String roleName, @Bind("emailPreference") Boolean emailPreference);

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
    List<DACUser> getMembersApprovedToReplace(@Bind("userId") Integer userId, @BindIn("roleIds") List<Integer> includedRoles);

    @SqlQuery("SELECT u.* FROM dacuser u "
            + " INNER JOIN user_role ur ON u.dacUserId = ur.user_id "
            + " INNER JOIN roles r ON ur.role_id = r.roleId "
            + " WHERE u.dacUserId != :userId "
            + " AND r.name = 'DataOwner'")
    List<DACUser> getDataOwnersApprovedToReplace(@Bind("userId") Integer userId);

    @SqlUpdate("update dacuser set displayName = :displayName where dacUserId = :userId")
    void updateDACUser(@Bind("displayName") String displayName, @Bind("userId") Integer userId);

    @SqlUpdate("update dacuser set email_preference = :emailPreference where dacUserId = :userId")
    void updateEmailPreference(@Bind("emailPreference") Boolean emailPreference, @Bind("userId") Integer userId);

}
