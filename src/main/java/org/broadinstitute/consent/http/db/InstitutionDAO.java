package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.InstitutionMapper;
import org.broadinstitute.consent.http.db.mapper.InstitutionWithUsersReducer;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
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

  @SqlQuery("SELECT * FROM institution WHERE LOWER(institution_name) = LOWER(:name) ")
  List<Institution> findInstitutionsByName(@Bind("name") String name);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = SimplifiedUser.class, prefix = "so")
  @UseRowReducer(InstitutionWithUsersReducer.class)
  @SqlQuery(
      "SELECT i.*, "
          + " u.user_id AS u_user_id, u.email AS u_email, "
          + " u.display_name AS u_display_name, u.create_date AS u_create_date, "
          + " u.email_preference AS u_email_preference, "
          + " u.era_commons_id AS u_era_commons_id, "
          + " u2.user_id AS u2_user_id, u2.email AS u2_email, "
          + " u2.display_name AS u2_display_name, u2.create_date AS u2_create_date, "
          + " u2.email_preference AS u2_email_preference, "
          + " u2.era_commons_id AS u2_era_commons_id, "
          + " so.so_user_id, so.so_email, so.so_display_name "
          + " FROM institution i "
          + " LEFT JOIN users u ON u.user_id = i.create_user "
          + " LEFT JOIN users u2 ON u2.user_id = i.update_user "
          + " LEFT JOIN "
          + "     (SELECT "
          + "         so.user_id AS so_user_id, so.email AS so_email, "
          + "         so.display_name AS so_display_name, so.institution_id AS so_institution_id "
          + "         FROM users so "
          + "         LEFT JOIN user_role ur ON ur.user_id = so.user_id"
          + "         WHERE ur.role_id = 7) so "
          + " ON i.institution_id = so.so_institution_id "
          + " ORDER BY LOWER(i.institution_name) ASC "
  )
  List<Institution> findAllInstitutions();

  @SqlQuery("SELECT institution_id FROM institution WHERE institution_id = :institutionId")
  Integer checkForExistingInstitution(@Bind("institutionId") Integer institutionId);
}
