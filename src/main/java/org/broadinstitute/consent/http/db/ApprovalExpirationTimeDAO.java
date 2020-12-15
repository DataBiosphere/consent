package org.broadinstitute.consent.http.db;

import java.util.Date;
import org.broadinstitute.consent.http.db.mapper.ApprovalExpirationTimeMapper;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(ApprovalExpirationTimeMapper.class)
public interface ApprovalExpirationTimeDAO extends Transactional<ApprovalExpirationTimeDAO> {

  @SqlUpdate(
      "INSERT INTO approval_expiration_time (create_date, amount_of_days, user_id) VALUES (:createDate, :amountOfDays, :userId)")
  @GetGeneratedKeys
  Integer insertApprovalExpirationTime(
      @Bind("createDate") Date createDate,
      @Bind("amountOfDays") Integer amountOfDays,
      @Bind("userId") Integer userId);

  @SqlUpdate(
      "UPDATE approval_expiration_time SET update_date = :updateDate, amount_of_days = :amountOfDays, user_id = :userId WHERE id = :id")
  void updateApprovalExpirationTime(
      @Bind("id") Integer id,
      @Bind("amountOfDays") Integer amountOfDays,
      @Bind("updateDate") Date updateDate,
      @Bind("userId") Integer userId);

  @SqlQuery(
      "SELECT et.*, du.displayname FROM approval_expiration_time et INNER JOIN dacuser du ON du.dacuserid = et.user_id WHERE id = :id")
  ApprovalExpirationTime findApprovalExpirationTimeById(@Bind("id") Integer id);

  @SqlQuery(
      "SELECT et.*, du.displayname FROM approval_expiration_time et INNER JOIN dacuser du ON du.dacuserid = et.user_id")
  ApprovalExpirationTime findApprovalExpirationTime();

  @SqlUpdate("DELETE FROM approval_expiration_time WHERE id = :id")
  void deleteApprovalExpirationTime(@Bind("id") Integer id);
}
