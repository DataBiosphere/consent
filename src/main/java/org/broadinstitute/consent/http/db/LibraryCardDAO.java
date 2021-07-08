package org.broadinstitute.consent.http.db;
import org.broadinstitute.consent.http.db.mapper.LibraryCardReducer;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

public interface LibraryCardDAO extends Transactional<LibraryCardDAO> {

  @SqlUpdate("INSERT INTO library_card " +
    " (user_id, institution_id, era_commons_id, user_name, user_email, create_user_id, create_date) " +
    " VALUES (:userId, :institutionId, :eraCommonsId, :userName, :userEmail, :createUserId, :createDate)")
  @GetGeneratedKeys
  Integer insertLibraryCard(@Bind("userId") Integer userId,
                            @Bind("institutionId") Integer institutionId,
                            @Bind("eraCommonsId") String eraCommonsId,
                            @Bind("userName") String userName,
                            @Bind("userEmail") String userEmail,
                            @Bind("createUserId") Integer createUserId,
                            @Bind("createDate") Date createDate);

  @SqlUpdate("UPDATE library_card SET " +
    " id = :libraryCardId, " +
    " user_id = :userId, " +
    " institution_id = :institutionId, " +
    " era_commons_id = :eraCommonsId, " +
    " user_name = :userName, " +
    " user_email = :userEmail, " +
    " update_user_id = :updateUserId, " +
    " update_date = :updateDate " +
    " WHERE id = :libraryCardId")
  void updateLibraryCardById(@Bind("libraryCardId") Integer libraryCardId,
                         @Bind("userId") Integer userId,
                         @Bind("institutionId") Integer institutionId,
                         @Bind("eraCommonsId") String eraCommonsId,
                         @Bind("userName") String userName,
                         @Bind("userEmail") String userEmail,
                         @Bind("updateUserId") Integer updateUserId,
                         @Bind("updateDate") Date updateDate);

  @SqlUpdate("DELETE FROM library_card WHERE id = :libraryCardId")
  void deleteLibraryCardById(@Bind("libraryCardId") Integer libraryCardId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery("SELECT * FROM library_card WHERE id = :libraryCardId")
  LibraryCard findLibraryCardById(@Bind("libraryCardId") Integer libraryCardId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery("SELECT lc.*, " +
          " institution.institution_id AS i_institution_id, " +
          " institution.institution_name AS i_name, " +
          " institution.it_director_name AS i_it_director_name, " +
          " institution.it_director_email AS i_it_director_email, " +
          " institution.create_user AS i_create_user_id, " +
          " institution.create_date AS i_create_date, " +
          " institution.update_date AS i_update_date, " +
          " institution.update_user AS i_update_user_id " +
          " FROM library_card AS lc " +
          " LEFT JOIN institution " +
          " ON lc.institution_id = institution.institution_id" +
          " WHERE lc.user_id = :userId")
  List<LibraryCard> findLibraryCardsByUserId(@Bind("userId") Integer userId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery("SELECT * FROM library_Card WHERE institution_id = :institutionId")
  List<LibraryCard> findLibraryCardsByInstitutionId(@Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery("SELECT lc.*, " +
    "institution.institution_id AS i_institution_id, " +
    "institution.institution_name AS i_name, " +
    "institution.it_director_name AS i_it_director_name, " +
    "institution.it_director_email AS i_it_director_email, " +
    "institution.create_user AS i_create_user_id, " +
    "institution.create_date AS i_create_date, " +
    "institution.update_date AS i_update_date, " +
    "institution.update_user AS i_update_user_id " +
    "FROM library_card AS lc " +
    "LEFT JOIN institution " +
    "ON lc.institution_id = institution.institution_id"
  )
  List<LibraryCard> findAllLibraryCards();

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery("SELECT * FROM library_card " +
          "WHERE user_email = :email")
  List<LibraryCard> findAllLibraryCardsByUserEmail(@Bind("email") String email);

  @SqlUpdate("UPDATE library_card SET " +
          " era_commons_id = :eraCommonsId " +
          " WHERE user_id = :userId")
  void updateEraCommonsForUser(@Bind("userId") Integer userId, @Bind("eraCommonsId") String eraCommonsId);
}
