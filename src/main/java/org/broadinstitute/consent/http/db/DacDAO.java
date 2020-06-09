package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(DacMapper.class)
public interface DacDAO extends Transactional<DacDAO> {

    @SqlQuery("select * from dac")
    List<Dac> findAll();

    @SqlQuery("select distinct d.* from dac d " +
            " inner join user_role ur on ur.dac_id = d.dac_id " +
            " inner join dacuser du on ur.user_id = du.dacUserId " +
            " where du.email = :email ")
    List<Dac> findDacsForEmail(@Bind("email") String email);

    @UseRowMapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id is not null inner join roles r on r.roleId = ur.role_id")
    List<User> findAllDACUserMemberships();

    @UseRowMapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du " +
              " inner join user_role ur on ur.user_id = du.dacUserId " +
              " inner join roles r on r.roleId = ur.role_id " +
              " where lower(du.displayName) like concat('%', lower(:term), '%') " +
              " or lower(du.email) like concat('%', lower(:term), '%') " +
              " or lower(du.additional_email) like concat('%', lower(:term), '%') ")
    List<User> findAllDACUsersBySearchString(@Bind("term") String term);

    @SqlQuery("select * from dac where dac_id = :dacId")
    Dac findById(@Bind("dacId") Integer dacId);

    @SqlUpdate("insert into dac (name, description, create_date) values (:name, :description, :createDate)")
    @GetGeneratedKeys
    Integer createDac(@Bind("name") String name, @Bind("description") String description, @Bind("createDate") Date createDate);

    @SqlUpdate("update dac set name = :name, description = :description, update_date = :updateDate where dac_id = :dacId")
    void updateDac(
            @Bind("name") String name,
            @Bind("description") String description,
            @Bind("updateDate") Date updateDate,
            @Bind("dacId") Integer dacId);

    @SqlUpdate("delete from user_role where dac_id = :dacId")
    void deleteDacMembers(@Bind("dacId") Integer dacId);

    @SqlUpdate("delete from dac where dac_id = :dacId")
    void deleteDac(@Bind("dacId") Integer dacId);

    @UseRowMapper(DACUserMapper.class)
    @SqlQuery("select du.* from dacuser du " +
              "inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id = :dacId")
    List<User> findMembersByDacId(@Bind("dacId") Integer dacId);

    @UseRowMapper(DACUserMapper.class)
    @SqlQuery("select du.* from dacuser du " +
              "inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id = :dacId and ur.role_id = :roleId")
    List<User> findMembersByDacIdAndRoleId(@Bind("dacId") Integer dacId, @Bind("roleId") Integer roleId);

    @SqlUpdate("insert into user_role (role_id, user_id, dac_id) values (:roleId, :userId, :dacId)")
    void addDacMember(@Bind("roleId") Integer roleId, @Bind("userId") Integer userId, @Bind("dacId") Integer dacId);

    @SqlUpdate("delete from user_role where user_role_id = :userRoleId")
    void removeDacMember(@Bind("userRoleId") Integer userRoleId);

    @UseRowMapper(RoleMapper.class)
    @SqlQuery("select * from roles where roleId = :roleId")
    Role getRoleById(@Bind("roleId") Integer roleId);

    @UseRowMapper(UserRoleMapper.class)
    @SqlQuery("select ur.*, r.name from user_role ur inner join roles r on ur.role_id = r.roleId where ur.user_id = :userId")
    List<UserRole> findUserRolesForUser(@Bind("userId") Integer userId);

    @UseRowMapper(UserRoleMapper.class)
    @SqlQuery("select ur.*, r.name from user_role ur inner join roles r on ur.role_id = r.roleId where ur.user_id in (<userIds>)")
    List<UserRole> findUserRolesForUsers(@BindList("userIds") List<Integer> userIds);

}
