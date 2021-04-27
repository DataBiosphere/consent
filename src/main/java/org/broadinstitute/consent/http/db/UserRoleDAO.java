package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DatabaseRoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserRoleMapper;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.List;

@RegisterRowMapper(UserRoleMapper.class)
public interface UserRoleDAO extends Transactional<UserRoleDAO> {

    @SqlQuery("select * from roles r inner join user_role ur on ur.role_id = r.roleId where ur.user_id = :userId")
    List<UserRole> findRolesByUserId(@Bind("userId") Integer userId);

    @SqlQuery("SELECT DISTINCT name " +
            "  FROM roles r " +
            "  INNER JOIN user_role ur ON ur.role_id = r.roleid " +
            "  INNER JOIN dacuser u ON u.dacuserid = ur.user_id " +
            "  WHERE LOWER(u.email) = LOWER(:email)")
    List<String> findRoleNamesByUserEmail(@Bind("email") String email);

    @UseRowMapper(DatabaseRoleMapper.class)
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
                         @BindList("existentRoles") List<Integer> existentRoles);

    @SqlUpdate("insert into user_role (role_id, user_id) values (:roleId, :userId)")
    void insertSingleUserRole(@Bind("roleId") Integer roleId, @Bind("userId") Integer userId);

    @SqlUpdate("delete from user_role where user_id = :dacUserId and role_id = :roleId")
    void removeSingleUserRole(@Bind("dacUserId") Integer dacUserId, @Bind("roleId") Integer roleId);

    @SqlQuery("select r.roleId from roles r inner join user_role ur on ur.role_id = r.roleId  where ur.user_id = :userId and r.name = :name")
    Integer findRoleByNameAndUser(@Bind("name") String name, @Bind("userId") Integer id);

    @SqlQuery("select * from user_role ur inner join roles r on r.roleId = ur.role_id where ur.user_id = :userId and ur.role_id = :roleId")
    UserRole findRoleByUserIdAndRoleId(@Bind("userId") Integer userId, @Bind("roleId") Integer roleId);

}
