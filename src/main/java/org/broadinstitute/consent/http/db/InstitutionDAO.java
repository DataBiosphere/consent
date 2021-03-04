package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Institution;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

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

  @SqlQuery("SELECT * FROM institution")
  List<Institution> findAllInstitutions();
}
