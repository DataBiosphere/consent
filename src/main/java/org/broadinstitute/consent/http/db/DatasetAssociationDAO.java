package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DatasetAssociationMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;


@RegisterRowMapper(DatasetAssociationMapper.class)
public interface DatasetAssociationDAO extends Transactional<DatasetAssociationDAO> {

  @SqlUpdate("DELETE FROM dataset_user_association WHERE dacuserId = :userId")
  void deleteAllDatasetUserAssociationsByUser(@Bind("userId") Integer userId);

}
