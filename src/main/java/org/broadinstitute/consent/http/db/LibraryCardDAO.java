package org.broadinstitute.consent.http.db;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.db.mapper.LibraryCardMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(LibraryCardMapper.class)
public interface LibraryCardDAO extends Transactional<LibraryCardDAO> {

  @SqlUpdate("INSERT INTO library_card " +
    " (user_id, institution_id, era_commons_id, user_name, user_email, create_user, create_date) " +
    " VALUES (:userId, :institutionId, :eraCommonsId, :userName, :userEmail, :createUser, :createDate)")
  @GetGeneratedKeys
  Integer insertLibraryCard(@Bind("userId") Integer userId,
                            @Bind("institutionId") Integer institutionId,
                            @Bind("eraCommonsId") String eraCommonsId, 
                            @Bind("userName") String userName,
                            @Bind("userEmail") String userEmail,
                            @Bind("createUser") Integer createUser,
                            @Bind("createDate") Date createDate);

  @SqlUpdate("UPDATE library_card SET " +
    " id = :libraryCardId, " +
    " user_id = :userId, " +
    " institution_id = :institutionId, " +
    " era_commons_id = :eraCommonsId, " +
    " user_name = :userName, " +
    " user_email = :userEmail, " +
    " update_user = :updateUser, " +
    " update_date = :updateDate " +
    " WHERE id = :libraryCardId")
  void updateLibraryCardById(@Bind("libraryCardId") Integer libraryCardId,
                         @Bind("userId") Integer userId, 
                         @Bind("institutionId") Integer institutionId,
                         @Bind("eraCommonsId") String eraCommonsId,
                         @Bind("userName") String userName,
                         @Bind("userEmail") String userEmail,
                         @Bind("updateUser") Integer updateUser,
                         @Bind("updateDate") Date updateDate);

  @SqlUpdate("DELETE FROM library_card WHERE id = :libraryCardId")
  void deleteLibraryCardById(@Bind("libraryCardId") Integer libraryCardId);

  @SqlQuery("SELECT * FROM library_card WHERE id = :libraryCardId")
  LibraryCard findLibraryCardById(@Bind("libraryCardId") Integer libraryCardId);

  @SqlQuery("SELECT * FROM library_card WHERE user_id = :userId")
  List<LibraryCard> findLibraryCardsByUserId(@Bind("userId") Integer userId);

  @SqlQuery("SELECT * FROM library_card")
  List<LibraryCard> findAllLibraryCards();

}
