package org.broadinstitute.consent.http.db;

import java.util.List;
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

@RegisterRowMapper(UserRoleMapper.class)
public interface UserRoleDAO extends Transactional<UserRoleDAO> {

  @SqlQuery("""
    SELECT * 
    FROM roles r
    INNER JOIN user_role ur ON ur.role_id = r.role_id 
    WHERE ur.user_id = :user_id
    """)
  List<UserRole> findRolesByUserId(@Bind("user_id") Integer userId); // r.roleId => role_id

  @SqlQuery("""
    SELECT DISTINCT name
    FROM roles r
    INNER JOIN user_role ur ON ur.role_id = r.role_id
    INNER JOIN users u ON u.user_id = ur.user_id
    WHERE LOWER(u.email) = LOWER(:email)
    """)
  List<String> findRoleNamesByUserEmail(@Bind("email") String email); // r.role_id

  @UseRowMapper(DatabaseRoleMapper.class)
  @SqlQuery("""
    SELECT * 
    FROM roles
    """)
  List<Role> findRoles();

  @SqlQuery("""
    SELECT role_id
    FROM roles
    WHERE name = :role_name
    """)
  Integer findRoleIdByName(@Bind("role_name") String roleName); // roleId in roles

  @SqlBatch("""
    INSERT INTO user_role (role_id, user_id) VALUES (:role_id, :user_id)
    """)
  void insertUserRoles(@BindBean List<UserRole> roles, @Bind("user_id") Integer userId);

  @SqlUpdate("""
    UPDATE user_role SET role_id = :new_role_id
    WHERE user_id = :user_id AND role_id = :existent_role_id
    """)
  void updateUserRoles(@Bind("new_role_id") Integer newRoleId,
      @Bind("user_id") Integer userId,
      @Bind("existent_role_id") Integer existentRoleId);

  @SqlUpdate("""
    DELETE FROM user_role WHERE user_id = :user_id AND role_id IN (<existent_roles>)
    """)
  void removeUserRoles(@Bind("user_id") Integer userId,
      @BindList("existent_roles") List<Integer> existentRoles); // what is existent_roles?

  @SqlUpdate("""
    INSERT INTO user_role (role_id, user_id) VALUES (:role_id, :user_id)
    """)
  void insertSingleUserRole(@Bind("role_id") Integer roleId, @Bind("user_id") Integer userId);

  @SqlUpdate("""
    DELETE FROM user_role WHERE user_id = :user_id AND role_id = :role_id
    """)
  void removeSingleUserRole(@Bind("user_id") Integer userId, @Bind("role_id") Integer roleId);

  @SqlQuery("""
    SELECT r.role_id 
    FROM roles r 
    INNER JOIN user_role ur on ur.role_id = r.role_id  
    WHERE ur.user_id = :user_id AND r.name = :name
    """)
  Integer findRoleByNameAndUser(@Bind("name") String name, @Bind("user_id") Integer id); // change r.roleId

  @SqlQuery("""
    SELECT *
    FROM user_role ur 
    INNER JOIN roles r ON r.role_id = ur.role_id
    WHERE ur.user_id = :user_id AND ur.role_id = :role_id
    """)
  UserRole findRoleByUserIdAndRoleId(@Bind("user_id") Integer userId,
      @Bind("role_id") Integer roleId); // change r.roleId

}
