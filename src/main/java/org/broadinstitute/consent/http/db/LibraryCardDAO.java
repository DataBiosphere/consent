package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.LibraryCardDaaAuditMapper;
import org.broadinstitute.consent.http.db.mapper.LibraryCardReducer;
import org.broadinstitute.consent.http.db.mapper.LibraryCardWithDaaReducer;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.LibraryCardDaaAudit;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface LibraryCardDAO extends Transactional<LibraryCardDAO> {

  @SqlUpdate(
      """
      INSERT INTO library_card (user_id, user_name, user_email, create_user_id, create_date)
      VALUES (:userId, :userName, :userEmail, :createUserId, :createDate)
      """)
  @GetGeneratedKeys
  Integer insertLibraryCard(
      @Bind("userId") Integer userId,
      @Bind("userName") String userName,
      @Bind("userEmail") String userEmail,
      @Bind("createUserId") Integer createUserId,
      @Bind("createDate") Date createDate);

  @SqlUpdate(
      """
      UPDATE library_card SET
            id = :libraryCardId,
            user_id = :userId,
            user_name = :userName,
            user_email = :userEmail,
            update_user_id = :updateUserId,
            update_date = :updateDate
            WHERE id = :libraryCardId
      """)
  void updateLibraryCardById(
      @Bind("libraryCardId") Integer libraryCardId,
      @Bind("userId") Integer userId,
      @Bind("userName") String userName,
      @Bind("userEmail") String userEmail,
      @Bind("updateUserId") Integer updateUserId,
      @Bind("updateDate") Date updateDate);

  @SqlUpdate(
      """
      WITH daa_deletes AS (DELETE FROM lc_daa lc_daa WHERE lc_daa.lc_id = :libraryCardId)
      DELETE FROM library_card lc WHERE lc.id = :libraryCardId
      """)
  void deleteLibraryCardById(@Bind("libraryCardId") Integer libraryCardId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery(
      """
      SELECT lc.*,
      ld.daa_id
      FROM library_card AS lc
      LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
      WHERE lc.id = :libraryCardId
      """)
  LibraryCard findLibraryCardById(@Bind("libraryCardId") Integer libraryCardId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @RegisterBeanMapper(value = DataAccessAgreement.class, prefix = "daa")
  @UseRowReducer(LibraryCardWithDaaReducer.class)
  @SqlQuery(
      """
      SELECT lc.*,
      ld.daa_id,
      daa.daa_id as daa_daa_id,
      daa.create_user_id as daa_create_user_id,
      daa.create_date as daa_create_date,
      daa.update_user_id as daa_update_user_id,
      daa.update_date as daa_update_date,
      daa.initial_dac_id as daa_initial_dac_id
      FROM library_card lc
      LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
      LEFT JOIN data_access_agreement daa ON ld.daa_id = daa.daa_id
      WHERE lc.id = :libraryCardId
      """)
  LibraryCard findLibraryCardDaaById(@Bind("libraryCardId") Integer libraryCardId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery(
      """
      SELECT lc.*,
      ld.daa_id
      FROM library_card AS lc
      LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
      WHERE lc.user_id = :userId
      """)
  LibraryCard findLibraryCardByUserId(@Bind("userId") Integer userId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery(
      """
      SELECT library_card.*, ld.daa_id
      FROM library_card
      LEFT JOIN lc_daa ld ON library_card.id = ld.lc_id
      INNER JOIN users u ON library_card.user_id = u.user_id AND u.institution_id = :institutionId
      """)
  List<LibraryCard> findLibraryCardsByInstitutionId(@Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery(
      """
      SELECT lc.*,
      ld.daa_id
      FROM library_card AS lc
      LEFT JOIN lc_daa ld ON lc.id = ld.lc_id
      """)
  List<LibraryCard> findAllLibraryCards();

  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery("SELECT * FROM library_card " + "WHERE user_email = :email")
  LibraryCard findLibraryCardByUserEmail(@Bind("email") String email);

  @SqlUpdate(
      "DELETE FROM library_card WHERE user_id = :userId OR create_user_id = :userId OR update_user_id = :userId")
  void deleteAllLibraryCardsByUser(@Bind("userId") Integer userId);

  @SqlUpdate(
      """
      WITH audit AS (INSERT INTO lc_daa_audit (daa_id, lc_id, user_id, action, action_date) VALUES (:daaId, :lcId, :userId, 'ADD', NOW()))
      INSERT INTO lc_daa (lc_id, daa_id)
      VALUES (:lcId, :daaId)
      ON CONFLICT DO NOTHING
      """)
  void createLibraryCardDaaRelation(
      @Bind("userId") Integer userId, @Bind("lcId") Integer lcId, @Bind("daaId") Integer daaId);

  @SqlUpdate(
      """
      WITH audit AS (INSERT INTO lc_daa_audit (daa_id, lc_id, user_id, action, action_date) VALUES (:daaId, :lcId, :userId, 'REMOVE', NOW()))
      DELETE FROM lc_daa
      WHERE lc_id = :lcId
      AND daa_id = :daaId
      """)
  void deleteLibraryCardDaaRelation(
      @Bind("userId") Integer userId, @Bind("lcId") Integer lcId, @Bind("daaId") Integer daaId);

  @RegisterRowMapper(LibraryCardDaaAuditMapper.class)
  @SqlQuery(
      """
    SELECT a.*
    FROM lc_daa_audit a
    INNER JOIN library_card lc ON a.lc_id = lc.id AND lc.user_id = :lcUserId
    ORDER BY a.action_date DESC
    """)
  List<LibraryCardDaaAudit> findAuditsByLcUserId(@Bind("lcUserId") Integer lcUserId);

  /**
   * Finds library cards by user emails.
   *
   * @param emails A list of email addresses
   * @return List of LibraryCard objects associated with the provided emails.
   */
  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery(
      """
      SELECT *
      FROM library_card
      WHERE LOWER(user_email) = ANY(ARRAY(SELECT LOWER(UNNEST(ARRAY[<emails>]))))
      """)
  List<LibraryCard> findByUserEmails(
      @BindList(value = "emails", onEmpty = EmptyHandling.NULL_STRING) List<String> emails);

  /**
   * Finds library cards by user ids.
   *
   * @param userIds A list of user IDs
   * @return List of LibraryCard objects associated with the provided ids.
   */
  @RegisterBeanMapper(value = LibraryCard.class)
  @UseRowReducer(LibraryCardReducer.class)
  @SqlQuery(
      """
      SELECT *
      FROM library_card
      WHERE user_id in (<userIds>)
      """)
  List<LibraryCard> findLibraryCardsByUserIds(
      @BindList(value = "userIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> userIds);
}
