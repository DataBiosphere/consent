package org.genomebridge.consent.http.db;


import org.genomebridge.consent.http.models.DACUser;
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

@UseStringTemplate3StatementLocator
@RegisterMapper({DACUserMapper.class})
public interface DACUserDAO extends Transactional<DACUserDAO> {

    @SqlQuery("select * from dacuser  where dacUserId = :dacUserId")
    DACUser findDACUserById(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select  *  from dacuser where  dacUserId IN (<dacUserIds>)")
    Collection<DACUser> findUsers(@BindIn("dacUserIds") Collection<Integer> dacUserIds);

    @SqlQuery("select u.* from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where r.name = 'Chairperson'")
    DACUser findChairpersonUser();

    @SqlQuery("select u.dacUserId from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where u.dacUserId = :dacUserId and r.name = 'Chairperson'")
    Integer checkChairpersonUser(@Bind("dacUserId") Integer dacUserId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*,r.roleId, r.name from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId where r.name = 'Chairperson' or r.name = 'Member'")
    Set<DACUser>  findDACUsersEnabledToVote();

    @SqlQuery("select * from dacuser where email = :email")
    DACUser findDACUserByEmail(@Bind("email") String email);

    @SqlQuery("select du.email from dacuser du where du.dacUserId in (<dacUserIds>)")
    Collection<String> describeUsersEmails(@BindIn("dacUserIds") List<Integer> dacUserIds);

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

    @SqlQuery("select dr.dacUserId from user_role dr inner join roles r on r.roleId = dr.roleId where dr.dacUserId <> :dacUserId and r.roleId = :roleId")
    Integer findDACUserIdByRole(@Bind("roleId") Integer roleId, @Bind("dacUserId") Integer dacUserId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select u.*, r.roleId, r.name from dacuser u inner join user_role du on du.dacUserId = u.dacUserId inner join roles r on r.roleId = du.roleId order by createDate desc")
    Set<DACUser> findUsers();

    @SqlQuery("select count(*) from user_role dr inner join roles r on r.roleId = dr.roleId where r.name = 'Admin'")
    Integer verifyAdminUsers();
}



