package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.db.mapper.ApprovalExpirationTimeMapper;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;



@RegisterRowMapper(ApprovalExpirationTimeMapper.class)
public interface ApprovalExpirationTimeDAO extends Transactional<ApprovalExpirationTimeDAO> {

    @SqlUpdate("insert into approval_expiration_time (create_date, amount_of_days, user_id) values " +
            "( :createDate, :amountOfDays, :userId)")
    @GetGeneratedKeys
    Integer insertApprovalExpirationTime(
                           @Bind("createDate") Date createDate,
                           @Bind("amountOfDays") Integer amountOfDays,
                           @Bind("userId") Integer userId);

    @SqlUpdate("update approval_expiration_time set update_date = :updateDate, amount_of_days = :amountOfDays, user_id = :userId where id = :id ")
    void updateApprovalExpirationTime(@Bind("id") Integer id,
                            @Bind("amountOfDays") Integer amountOfDays,
                            @Bind("updateDate") Date updateDate,
                            @Bind("userId") Integer userId);

    @SqlQuery("select et.*, du.displayName  from  approval_expiration_time et inner join dacuser du on du.dacUserId = et.user_id where  id = :id")
    ApprovalExpirationTime findApprovalExpirationTimeById(@Bind("id") Integer id);

    @SqlQuery("select et.*, du.displayName   from  approval_expiration_time et inner join dacuser du on du.dacUserId = et.user_id")
    ApprovalExpirationTime findApprovalExpirationTime();

    @SqlUpdate("delete from approval_expiration_time where id = :id")
    void deleteApprovalExpirationTime(@Bind("id") Integer id);


}
