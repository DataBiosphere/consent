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
    WHERE ur.user_id = :userId
    """)
  List<UserRole> findRolesByUserId(@Bind("userId") Integer userId);

  @SqlQuery("""
    SELECT DISTINCT name
    FROM roles r
    INNER JOIN user_role ur ON ur.role_id = r.role_id
    INNER JOIN users u ON u.user_id = ur.user_id
    WHERE LOWER(u.email) = LOWER(:email)
    """)
  List<String> findRoleNamesByUserEmail(@Bind("email") String email);

  @UseRowMapper(DatabaseRoleMapper.class)
  @SqlQuery("""
    SELECT * 
    FROM roles
    """)
  List<Role> findRoles();

  @SqlQuery("""
    SELECT role_id
    FROM roles
    WHERE name = :roleName
    """)
  Integer findRoleIdByName(@Bind("roleName") String roleName);

  @SqlBatch("""
    INSERT INTO user_role (role_id, user_id) VALUES (:roleId, :userId)
    """)
  void insertUserRoles(@BindBean List<UserRole> roles, @Bind("userId") Integer userId);

  @SqlUpdate("""
    UPDATE user_role SET role_id = :newRoleId
    WHERE user_id = :userId AND role_id = :existentRoleId
    """)
  void updateUserRoles(@Bind("newRoleId") Integer newRoleId,
      @Bind("userId") Integer userId,
      @Bind("existentRoleId") Integer existentRoleId);

  @SqlUpdate("""
    DELETE FROM user_role WHERE user_id = :userId AND role_id IN (<existentRoles>)
    """)
  void removeUserRoles(@Bind("userId") Integer userId,
      @BindList("existentRoles") List<Integer> existentRoles);

  @SqlUpdate("""
    INSERT INTO user_role (role_id, user_id) VALUES (:roleId, :userId)
    """)
  void insertSingleUserRole(@Bind("roleId") Integer roleId, @Bind("userId") Integer userId);

  @SqlUpdate("""
    DELETE FROM user_role WHERE user_id = :userId AND role_id = :roleId
    """)
  void removeSingleUserRole(@Bind("userId") Integer userId, @Bind("roleId") Integer roleId);

  @SqlQuery("""
    SELECT r.role_id 
    FROM roles r 
    INNER JOIN user_role ur on ur.role_id = r.role_id  
    WHERE ur.user_id = :userId AND r.name = :name
    """)
  Integer findRoleByNameAndUser(@Bind("name") String name, @Bind("userId") Integer id);

  @SqlQuery("""
    SELECT *
    FROM user_role ur 
    INNER JOIN roles r ON r.role_id = ur.role_id
    WHERE ur.user_id = :userId AND ur.role_id = :roleId
    """)
  UserRole findRoleByUserIdAndRoleId(@Bind("userId") Integer userId,
      @Bind("roleId") Integer roleId);

}
