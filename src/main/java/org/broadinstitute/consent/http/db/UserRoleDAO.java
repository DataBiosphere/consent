package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.UserRole;
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

import java.util.List;

@UseStringTemplate3StatementLocator
@RegisterMapper({UserRoleMapper.class})
public interface UserRoleDAO extends Transactional<UserRoleDAO> {

    @SqlQuery("select * from roles r inner join user_role ur on ur.role_id = r.roleId where ur.user_id = :userId")
    List<UserRole> findRolesByUserId(@Bind("userId") Integer userId);

    @SqlQuery("select * from roles r inner join user_role ur on ur.role_id = r.roleId  " +
              "inner join dacuser u on u.dacUserId = ur.user_id where u.email = :email")
    List<UserRole> findRolesByUserEmail(@Bind("email") String email);

    @Mapper(DatabaseRoleMapper.class)
    @SqlQuery("select * from roles")
    List<Role> findRoles();

    @SqlQuery("select roleId from roles where name = :roleName")
    Integer findRoleIdByName(@Bind("roleName") String roleName);

    @SqlBatch("insert into user_role (role_id, user_id) values (:roleId, :userId)")
    void insertUserRoles(@BindBean List<UserRole> roles, @Bind("userId") Integer dacUserId);

    @SqlUpdate("update user_role set role_id = :newRoleId where user_id = :dacUserId and role_id = :existentRoleId")
    void updateUserRoles(@Bind("newRoleId") Integer newRoleId,
                         @Bind("dacUserId") Integer dacUserId,
                         @Bind("existentRoleId") Integer existentRoleId);

    @SqlUpdate("delete from user_role where user_id = :dacUserId and role_id IN (<existentRoles>)")
    void removeUserRoles(@Bind("dacUserId") Integer dacUserId,
                         @BindIn("existentRoles") List<Integer> existentRoles);

    @SqlUpdate("insert into user_role (role_id, user_id) values (:roleId, :userId)")
    void insertSingleUserRole(@Bind("roleId") Integer roleId, @Bind("userId") Integer userId);

    @SqlUpdate("delete from user_role where user_id = :dacUserId and role_id = :roleId")
    void removeSingleUserRole(@Bind("dacUserId") Integer dacUserId, @Bind("roleId") Integer roleId);

    @SqlQuery("select r.roleId from roles r inner join user_role ur on ur.role_id = r.roleId  where ur.user_id = :userId and r.name = :name")
    Integer findRoleByNameAndUser(@Bind("name") String name, @Bind("userId") Integer id);

    @SqlQuery("select * from user_role ur inner join roles r on r.roleId = ur.role_id where ur.user_id = :userId and ur.role_id = :roleId")
    UserRole findRoleByUserIdAndRoleId(@Bind("userId") Integer userId, @Bind("roleId") Integer roleId);

    @SqlUpdate("delete from user_role")
    void deleteAllUserRoles();

}
