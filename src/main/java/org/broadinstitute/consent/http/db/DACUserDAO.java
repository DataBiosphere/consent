package org.broadinstitute.consent.http.db;

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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

// TODO: Rename this class to UserDAO - see DUOS-344
@UseStringTemplate3StatementLocator
@RegisterMapper({DACUserMapper.class})
public interface DACUserDAO extends Transactional<DACUserDAO> {

    @SqlQuery("select * from dacuser where dacUserId = :dacUserId")
    DACUser findDACUserById(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from dacuser where dacUserId IN (<dacUserIds>)")
    Collection<DACUser> findUsers(@BindIn("dacUserIds") Collection<Integer> dacUserIds);

    @SqlQuery("select du.* from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = :roleName")
    List<DACUser> describeUsersByRole(@Bind("roleName") String roleName);

    @SqlQuery("select du.dacUserId from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where du.dacUserId = :dacUserId and r.name = 'Chairperson'")
    Integer checkChairpersonUser(@Bind("dacUserId") Integer dacUserId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id = :dacId inner join roles r on r.roleId = ur.role_id where r.name = 'Chairperson' or r.name = 'Member'")
    Set<DACUser> findDACUsersEnabledToVoteByDAC(@Bind("dacId") Integer dacId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId and ur.dac_id is null inner join roles r on r.roleId = ur.role_id where r.name = 'Chairperson' or r.name = 'Member'")
    Set<DACUser> findNonDACUsersEnabledToVote();

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where  du.dacUserId IN (<dacUserIds>)")
    Set<DACUser> findUsersWithRoles(@BindIn("dacUserIds") Collection<Integer> dacUserIds);

    @SqlQuery("select * from dacuser where email = :email")
    DACUser findDACUserByEmail(@Bind("email") String email);

    @SqlUpdate("insert into dacuser (email, displayName, createDate) values (:email, :displayName, :createDate)")
    @GetGeneratedKeys
    Integer insertDACUser(@Bind("email") String email,
                          @Bind("displayName") String displayName,
                          @Bind("createDate") Date createDate);

    @SqlUpdate("update dacuser set email=:email, displayName=:displayName, additional_email=:additionalEmail where dacUserId=:id")
    void updateDACUser(@Bind("email") String email,
                       @Bind("displayName") String displayName,
                       @Bind("id") Integer id,
                       @Bind("additionalEmail") String additionalEmail);

    @SqlUpdate("delete from dacuser where email = :email")
    void deleteDACUserByEmail(@Bind("email") String email);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId " +
              "inner join roles r on r.roleId = ur.role_id order by createDate desc")
    Set<DACUser> findUsers();

    @SqlQuery("select count(*) from user_role dr inner join roles r on r.roleId = dr.role_id where r.name = 'Admin'")
    Integer verifyAdminUsers();

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id from dacuser du inner join user_role ur on ur.user_id = du.dacUserId inner join roles r on r.roleId = ur.role_id where r.name = :roleName and du.email_preference = :emailPreference")
    List<DACUser> describeUsersByRoleAndEmailPreference(@Bind("roleName") String roleName, @Bind("emailPreference") Boolean emailPreference);

    @SqlUpdate("update dacuser set email_preference = :emailPreference where dacUserId = :userId")
    void updateEmailPreference(@Bind("emailPreference") Boolean emailPreference, @Bind("userId") Integer userId);

    @SqlUpdate("update dacuser set status = :status where dacUserId = :userId")
    void updateUserStatus(@Bind("status") Integer status, @Bind("userId") Integer userId);

    @SqlUpdate("update dacuser set rationale = :rationale where dacUserId = :userId")
    void updateUserRationale(@Bind("rationale") String rationale, @Bind("userId") Integer userId);

    @SqlQuery("select * from dacuser du "
            + " inner join user_role ur on du.dacUserId = ur.user_id "
            + " inner join roles r on ur.role_id = r.roleId "
            + " where du.email = :email "
            + " and r.roleId = :roleId")
    DACUser findDACUserByEmailAndRoleId(@Bind("email") String email, @Bind("roleId") Integer roleId);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id " +
            " from dacuser du " +
            " inner join user_role ur on ur.user_id = du.dacUserId " +
            " inner join roles r on r.roleId = ur.role_id and r.name in (<roleNames>) " +
            " inner join vote v on v.dacUserId = du.dacUserId and v.electionId in (<electionIds>) ")
    Set<DACUser> findUsersForElectionsByRoles(@BindIn("electionIds") List<Integer> electionIds,
                                               @BindIn("roleNames") List<String> roleNames);

    @Mapper(DACUserRoleMapper.class)
    @SqlQuery("select du.*, r.roleId, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id " +
            " from dacuser du " +
            " inner join user_role ur on ur.user_id = du.dacUserId " +
            " inner join roles r on r.roleId = ur.role_id and r.name in (<roleNames>) " +
            " inner join dac d on d.dac_id = ur.dac_id " +
            " inner join consents c on c.dac_id = d.dac_id " +
            " inner join consentassociations a on a.consentId = c.consentId " +
            " where a.dataSetId in (<datasetIds>) "
    )
    Set<DACUser> findUsersForDatasetsByRole(
            @BindIn("datasetIds") List<Integer> datasetIds,
            @BindIn("roleNames") List<String> roleNames);

}
