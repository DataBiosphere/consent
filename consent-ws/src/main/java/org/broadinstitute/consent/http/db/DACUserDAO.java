package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
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

@UseStringTemplate3StatementLocator
@RegisterMapper({DACUserMapper.class})
public interface DACUserDAO extends Transactional<DACUserDAO> {

    @SqlQuery("select * from dacuser  where dacUserId = :dacUserId")
    DACUser findDACUserById(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select  *  from dacuser where  dacUserId IN (<dacUserIds>)")
    Collection<DACUser> findUsers(@BindIn("dacUserIds") Collection<Integer> dacUserIds);

    @SqlQuery("select u.* from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where r.name = 'Chairperson'")
    DACUser findChairpersonUser();

    @SqlQuery("select u.* from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where r.name = :roleName")
    List<DACUser> describeUsersByRole(@Bind("roleName") String roleName);

    @SqlQuery("select u.dacUserId from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where u.dacUserId = :dacUserId and r.name = 'Chairperson'")
    Integer checkChairpersonUser(@Bind("dacUserId") Integer dacUserId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*,r.roleId, r.name from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where r.name = 'Chairperson' or r.name = 'Member'")
    Set<DACUser> findDACUsersEnabledToVote();

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*,r.roleId, r.name, du.status from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where  u.dacUserId IN (<dacUserIds>)")
    Set<DACUser> findUsersWithRoles(@BindIn("dacUserIds") Collection<Integer> dacUserIds);

    @SqlQuery("select * from dacuser where email = :email")
    DACUser findDACUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser (email, displayName, createDate) values (:email, :displayName, :createDate)")
    @GetGeneratedKeys
    Integer insertDACUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("createDate") Date createDate);

    @SqlUpdate("update dacuser set email=:email, displayName=:displayName where dacUserId=:id")
    @GetGeneratedKeys
    Integer updateDACUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("id") Integer id);

    @SqlUpdate("delete from dacuser where email = :email")
    void deleteDACUserByEmail(@Bind("email") String email);

    @SqlQuery("select dr.dacUserId from user_role dr inner join roles r on r.roleId = dr.roleId where dr.dacUserId != :dacUserId and r.roleId = :roleId")
    Integer findDACUserIdByRole(@Bind("roleId") Integer roleId, @Bind("dacUserId") Integer dacUserId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name, du.status from dacuser u inner join user_role du on du.dacUserId = u.dacUserId " +
              "inner join roles r on r.roleId = du.roleId order by createDate desc")
    Set<DACUser> findUsers();

    @SqlQuery("select count(*) from user_role dr inner join roles r on r.roleId = dr.roleId where r.name = 'Admin'")
    Integer verifyAdminUsers();

    @SqlQuery("select u.* from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where r.name = :roleName and du.email_preference = :emailPreference")
    List<DACUser> describeUsersByRoleAndEmailPreference(@Bind("roleName") String roleName, @Bind("emailPreference") Boolean emailPreference);

    @SqlQuery("Select * from dacuser d where d.dacUserId NOT IN "
            + "("
            + "    Select v.dacUserId From vote v Where v.electionId IN "
            + "    ("
            + "        Select e.electionId from election e where e.electionType != 'DataSet' and (e.status = 'Open' or e.status = 'Final')"
            + "        and exists "
            + "        ("
            + "            Select * from vote v where v.electionId = e.electionId and v.dacUserId = :dacUserId"
            + "        )"
            + "        and "
            + "        ( "
            + "            (select count(*) from vote v2 where v2.electionId = e.electionId and v2.type = 'DAC') \\< 5 "
            + "        )"
            + "    ) "
            + "group by v.dacUserId"
            + ") AND d.dacUserId NOT IN (Select ur.dacUserId from user_role ur where ur.roleId in (<roleIds>))")
    List<DACUser> getMembersApprovedToReplace(@Bind("dacUserId") Integer dacUserId, @BindIn("roleIds") List<Integer> includedRoles);

    @SqlQuery("SELECT * FROM dacuser du "
            + " INNER JOIN user_role ur ON du.dacUserId = ur.dacUserId "
            + " INNER JOIN roles r ON ur.roleId = r.roleId "
            + " WHERE du.dacUserId != :dacUserId "
            + " AND r.name = 'DataOwner'")
    List<DACUser> getDataOwnersApprovedToReplace(@Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("update dacuser set displayName=:displayName where dacUserId = :id")
    @GetGeneratedKeys
    Integer updateDACUser(@Bind("displayName") String displayName,
                          @Bind("id") Integer id);

}
