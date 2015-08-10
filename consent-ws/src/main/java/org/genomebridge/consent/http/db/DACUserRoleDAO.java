package org.genomebridge.consent.http.db;


import org.genomebridge.consent.http.models.DACUserRole;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.List;


@UseStringTemplate3StatementLocator
@RegisterMapper({RoleMapper.class})
public interface DACUserRoleDAO extends Transactional<DACUserRoleDAO> {

    @SqlQuery("select * from roles r inner join user_role du on du.roleId = r.roleId  where du.dacUserId = :userId")
    List<DACUserRole> findRolesByUserId(@Bind("userId") Integer userId);


    @SqlQuery("select roleId from roles where name in (<rolesName>)")
    List<Integer> findRolesIdByName(@BindIn("rolesName") List<String> rolesName);


    @SqlQuery("select roleId from roles where name = :roleName")
    Integer findRoleIdByName(@Bind("roleName") String roleName);


    @SqlBatch("insert into user_role (roleId, dacUserId) values (:roleId, :dacUserId)")
    void insertUserRoles(@Bind("roleId") List<Integer> roleIds,
                         @Bind("dacUserId") Integer userId);

    @SqlUpdate("update user_role set roleId = :newRoleId where dacUserId = :dacUserId and roleId = :existentRoleId")
    void updateUserRoles(@Bind("newRoleId") Integer newRoleId,
                         @Bind("dacUserId") Integer dacUserId,
                         @Bind("existentRoleId") Integer existentRoleId);


}
