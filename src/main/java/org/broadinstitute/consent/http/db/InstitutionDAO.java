package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.InstitutionMapper;
import org.broadinstitute.consent.http.db.mapper.InstitutionWithUsersReducer;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(InstitutionMapper.class)
public interface InstitutionDAO extends Transactional<InstitutionDAO> {

  @SqlUpdate("INSERT INTO institution " +
    " (institution_name, it_director_name, it_director_email, create_user, create_date) " +
    " VALUES (:institutionName, :itDirectorName, :itDirectorEmail, :createUser, :createDate)")
  @GetGeneratedKeys
  Integer insertInstitution(@Bind("institutionName") String institutionName,
                            @Bind("itDirectorName") String itDirectorName,
                            @Bind("itDirectorEmail") String itDirectorEmail,
                            @Bind("createUser") Integer createUser,
                            @Bind("createDate") Date createDate);

  @SqlUpdate("UPDATE institution SET " +
    " institution_id = :institutionId, " +
    " institution_name = :institutionName, " +
    " it_director_name = :itDirectorName, " +
    " it_director_email = :itDirectorEmail, " +
    " update_user = :updateUser, " +
    " update_date = :updateDate " +
    " WHERE institution_id = :institutionId")
  void updateInstitutionById(@Bind("institutionId") Integer institutionId,
                         @Bind("institutionName") String institutionName,
                         @Bind("itDirectorName") String itDirectorName,
                         @Bind("itDirectorEmail") String itDirectorEmail,
                         @Bind("updateUser") Integer updateUser,
                         @Bind("updateDate") Date updateDate);

  @SqlUpdate("DELETE FROM institution WHERE institution_id = :institutionId")
  void deleteInstitutionById(@Bind("institutionId") Integer institutionId);

  @SqlQuery("SELECT * FROM institution WHERE institution_id = :institutionId")
  Institution findInstitutionById(@Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = User.class, prefix = "u2")
  @UseRowReducer(InstitutionWithUsersReducer.class)
  @SqlQuery("SELECT i.*, u.*, u2.dacuserid AS updateUserId, u2.email AS updateUserEmail, " +
  " u2.displayName AS updateUserName, u2.createdate AS updateUserCreateDate, " +
  " u2.additional_email as updateUserAdditionalEmail, u2.email_preference as updateUserEmailPreference, " +
  " u2.status as updateUserStatus, u2.rationale as updateUserRationale " +
  " FROM institution i" +
  " LEFT JOIN dacuser u ON u.dacuserid = i.create_user" +
  " LEFT JOIN dacuser u2 ON u2.dacuserid = i.update_user")
  List<Institution> findAllInstitutions();
}
