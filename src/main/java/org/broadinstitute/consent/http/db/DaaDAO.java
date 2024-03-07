package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DaaMapper;
import org.broadinstitute.consent.http.db.mapper.DataAccessAgreementReducer;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DaaMapper.class)
public interface DaaDAO extends Transactional<DaaDAO> {

  String QUERY_FIELD_SEPARATOR = ", ";

  /**
   * Find all DAAs
   *
   * @return List<DataAccessAgreement>
   */
  @UseRowReducer(DataAccessAgreementReducer.class)
  @SqlQuery("""
      SELECT *
      FROM data_access_agreement
      """)
  List<DataAccessAgreement> findAll();


  /**
   * Find a DAA by id
   *
   * @param daaId The daa_id to lookup
   * @return Daa
   */
  @UseRowReducer(DataAccessAgreementReducer.class)
  @SqlQuery("SELECT * FROM data_access_agreement WHERE daa_id = :daaId")
  DataAccessAgreement findById(@Bind("daaId") Integer daaId);

  /**
   * Find a DAA by DAC
   *
   * @param dacId The initial_dac_id to lookup
   * @return Daa
   */
  @UseRowReducer(DataAccessAgreementReducer.class)
  @SqlQuery("SELECT * FROM data_access_agreement WHERE initial_dac_id = :dacId")
  DataAccessAgreement findByDacId(@Bind("dacId") Integer dacId);

  /**
   * Create a Daa given name, description, and create date
   *
   * @param createUserId The id of the user who created this DAA
   * @param createDate   The date this new DAA was created
   * @param updateUserId The id of the user who updated this DAA
   * @param updateDate   The date that this DAA was updated
   * @param initialDacId The id for the initial DAC this DAA was created for
   * @return Integer
   */
  @SqlUpdate("""
      INSERT INTO data_access_agreement (create_user_id, create_date, update_user_id, update_date, initial_dac_id)
      VALUES (:createUserId, :createDate, :updateUserId, :updateDate, :initialDacId)
      """)
  @GetGeneratedKeys
  Integer createDaa(@Bind("createUserId") Integer createUserId,
      @Bind("createDate") Instant createDate, @Bind("updateUserId") Integer updateUserId,
      @Bind("updateDate") Instant updateDate, @Bind("initialDacId") Integer initialDacId);

  @SqlUpdate("""
      INSERT INTO dac_daa (daa_id, dac_id)
      VALUES (:daaId, :dacId)
      """)
  void createDaaDacRelation(@Bind("daaId") Integer daaId, @Bind("dacId") Integer dacId);

  @SqlUpdate("""
      DELETE FROM dac_daa 
      WHERE dac_id = :dacId
      """)
  void deleteDaaDacRelation(@Bind("dacId") Integer dacId);

}
