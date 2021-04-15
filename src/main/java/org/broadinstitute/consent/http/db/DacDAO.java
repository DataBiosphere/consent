package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DacWithDatasetsReducer;
import org.broadinstitute.consent.http.db.mapper.RoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserRoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesReducer;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

@RegisterRowMapper(DacMapper.class)
public interface DacDAO extends Transactional<DacDAO> {

    /**
     * @return A Dac with Datasets
     */
    @RegisterBeanMapper(value = Dac.class)
    @RegisterBeanMapper(value = DataSetDTO.class)
    @UseRowReducer(DacWithDatasetsReducer.class)
    @SqlQuery("SELECT dac.*, " +
    "d.datasetid, d.name, DATE(d.createdate), d.objectid, d.active, d.needs_approval, d.alias, d.create_user_id, d.update_date, d.update_user_id, " +
    "ca.consentid, " +
    "c.translateduserestriction, c.datause " +
    "FROM dac " +
    "LEFT OUTER JOIN consents c on c.dac_id = dac.dac_id " +
    "LEFT OUTER JOIN consentassociations ca on ca.consentid = c.consentid " + 
    "LEFT OUTER JOIN dataset d on ca.datasetid = d.datasetid ")
    List<Dac> findAll();

    @SqlQuery("select distinct d.* from dac d " +
            " inner join user_role ur on ur.dac_id = d.dac_id " +
            " inner join dacuser du on ur.user_id = du.dacUserId " +
            " where du.email = :email ")
    List<Dac> findDacsForEmail(@Bind("email") String email);

    @UseRowMapper(UserWithRolesMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id is not null inner join roles r on r.roleId = ur.role_id")
    List<User> findAllDACUserMemberships();

    @UseRowMapper(UserWithRolesMapper.class)
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

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + "     u.dacuserid, u.email, u.displayname, u.createdate, u.additional_email, "
        + "     u.email_preference, u.status, u.rationale, "
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
        + " FROM dacuser u "
        + " INNER JOIN user_role ur ON ur.user_id = u.dacuserid "
        + " INNER JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE ur.dac_id = :dacId ")
    List<User> findMembersByDacId(@Bind("dacId") Integer dacId);

    @RegisterBeanMapper(value = User.class)
    @RegisterBeanMapper(value = UserRole.class)
    @UseRowReducer(UserWithRolesReducer.class)
    @SqlQuery("SELECT "
        + "     u.dacuserid, u.email, u.displayname, u.createdate, u.additional_email, "
        + "     u.email_preference, u.status, u.rationale, "
        + "     ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
        + " FROM dacuser u "
        + " INNER JOIN user_role ur ON ur.user_id = u.dacuserid "
        + " INNER JOIN roles r ON r.roleid = ur.role_id "
        + " WHERE ur.dac_id = :dacId "
        + " AND ur.role_id = :roleId ")
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

    /**
     * Find the Dacs for these datasets.
     *
     * DACs -> Consents -> Consent Associations -> DataSets
     *
     * @param datasetIds The list of dataset ids
     * @return All DACs that corresponds to the provided dataset ids
     */
    @RegisterRowMapper(DacMapper.class)
    @SqlQuery("SELECT d.*, a.datasetid " +
            " FROM dac d " +
            " INNER JOIN consents c ON d.dac_id = c.dac_id " +
            " INNER JOIN consentassociations a ON a.consentid = c.consentid " +
            " WHERE a.datasetid IN (<datasetIds>) ")
    Set<Dac> findDacsForDatasetIds(@BindList("datasetIds") List<Integer> datasetIds);

}
