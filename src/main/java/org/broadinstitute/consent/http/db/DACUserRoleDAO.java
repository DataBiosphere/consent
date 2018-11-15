package org.broadinstitute.consent.http.db;


import java.util.List;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.Role;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;


@UseStringTemplate3StatementLocator
@RegisterMapper({RoleMapper.class})
public interface DACUserRoleDAO extends Transactional<DACUserRoleDAO> {

    @SqlQuery("select * from roles r inner join user_role du on du.roleId = r.roleId  where du.dacUserId = :userId")
    List<DACUserRole> findRolesByUserId(@Bind("userId") Integer userId);

    @SqlQuery("select * from roles r inner join user_role du on du.roleId = r.roleId  " +
              "inner join dacuser u on u.dacUserId = du.dacUserId where u.email = :email")
    List<DACUserRole> findRolesByUserEmail(@Bind("email") String email);

    @Mapper(DatabaseRoleMapper.class)
    @SqlQuery("select * from roles")
    List<Role> findRoles();

    @SqlQuery("select roleId from roles where name = :roleName")
    Integer findRoleIdByName(@Bind("roleName") String roleName);

    @SqlBatch("insert into user_role (roleId, dacUserId, email_preference) values (:roleId, :dacUserId, :emailPreference)")
    void insertUserRoles(@BindBean List<DACUserRole> roles, @Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("update user_role set roleId = :newRoleId where dacUserId = :dacUserId and roleId = :existentRoleId")
    void updateUserRoles(@Bind("newRoleId") Integer newRoleId,
                         @Bind("dacUserId") Integer dacUserId,
                         @Bind("existentRoleId") Integer existentRoleId);

    @SqlUpdate("delete from user_role where dacUserId = :dacUserId and roleId IN (<existentRoles>)")
    void removeUserRoles(@Bind("dacUserId") Integer dacUserId,
                         @BindIn("existentRoles") List<Integer> existentRoles);

    @SqlUpdate("insert into user_role (roleId, dacUserId, email_preference) values (:roleId, :dacUserId, :emailPreference)")
    void insertSingleUserRole(@Bind("roleId") Integer roleId, @Bind("dacUserId") Integer dacUserId, @Bind("emailPreference") Boolean emailPreference);

    @SqlUpdate("update user_role set email_preference = :emailPreference where roleId =:roleId and  dacUserId = :dacUserId")
    void updateEmailPreferenceUserRole(@BindBean DACUserRole role, @Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("delete from user_role where dacUserId = :dacUserId and roleId = :roleId")
    void removeSingleUserRole(@Bind("dacUserId") Integer dacUserId, @Bind("roleId") Integer roleId);

    @SqlQuery("select  r.roleId from roles r inner join user_role du on du.roleId = r.roleId  where du.dacUserId = :userId and r.name = :name")
    Integer findRoleByNameAndUser(@Bind("name") String name, @Bind("userId") Integer id);

    @SqlUpdate("update user_role set status = :status, rationale = :rationale where dacUserId = :userId and roleId = :roleId")
    void updateUserRoleStatus(@Bind("userId") Integer userId, @Bind("roleId") Integer roleId, @Bind("status") Integer status, @Bind("rationale") String rationale);

    @SqlQuery("select * from  user_role ur  inner join roles r on r.roleId = ur.roleId where ur.dacUserId = :userId and ur.roleId = :roleId")
    DACUserRole findRoleByUserIdAndRoleId(@Bind("userId") Integer userId, @Bind("roleId") Integer roleId);
}