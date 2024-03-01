package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.mapper.DaaMapper;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DacWithDatasetsReducer;
import org.broadinstitute.consent.http.db.mapper.DataAccessAgreementReducer;
import org.broadinstitute.consent.http.db.mapper.RoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserRoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesReducer;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
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
  @SqlQuery("SELECT * FROM data_access_agreement WHERE id = :daaId")
  DataAccessAgreement findById(@Bind("daaId") Integer daaId);

  /**
   * Find a DAA by DAC
   *
   * @param dacId The initial_dac_id to lookup
   * @return Daa
   */
  @RegisterBeanMapper(value = Dac.class, prefix="d")
  @RegisterBeanMapper(value = DataAccessAgreement.class)
  @UseRowReducer(DataAccessAgreementReducer.class)
  @SqlQuery("""
  SELECT daa.*,
         dac.dac_id as d_dac_id,
         dac.update_date as d_update_date,
         dac.name as d_name,
         dac.create_date as d_create_date,
         dac.description as d_description
  FROM data_access_agreement daa
  LEFT JOIN dac_daa dac_daa_join ON dac_daa_join.daa_id = daa.id AND dac_daa_join.dac_id = :dacId,
         dac dac
  WHERE dac.dac_id = :dacId
  """)
  DataAccessAgreement findByDacId(@Bind("dacId") Integer dacId);

  /**
   * Create a Daa given name, description, and create date
   *
   * @param createUserId        The id of the user who created this DAA
   * @param createDate The date this new DAA was created
   * @param updateUserId        The id of the user who updated this DAA
   * @param updateDate        The date that this DAA was updated
   * @param initialDacId      The id for the initial DAC this DAA was created for
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
