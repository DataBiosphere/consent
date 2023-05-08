package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DatasetAssociationMapper;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Collection;
import java.util.List;


@RegisterRowMapper(DatasetAssociationMapper.class)
public interface DatasetAssociationDAO extends Transactional<DatasetAssociationDAO> {

    @SqlBatch("insert into dataset_user_association (datasetId, dacuserId, createDate )" +
            " values (:datasetId, :dacuserId, :createDate)")
    void insertDatasetUserAssociation(@BindBean Collection<DatasetAssociation> associationList) throws UnableToExecuteStatementException;

    @SqlQuery("select * from dataset_user_association where datasetId = :datasetId")
    List<DatasetAssociation> getDatasetAssociation(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("delete from dataset_user_association where datasetId = :datasetId")
    void delete(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("DELETE FROM dataset_user_association WHERE dacuserId = :userId")
    void deleteAllDatasetUserAssociationsByUser(@Bind("userId") Integer userId);

    @SqlQuery("select exists (select * from dataset_user_association where datasetId = :datasetId )")
    Boolean exist(@Bind("datasetId") Integer datasetId);

    @SqlQuery("""
        SELECT DISTINCT dacuserid
        FROM dataset_user_association
        WHERE datasetid = :datasetId
        """)
    List<Integer> getDataOwnersOfDataSet(@Bind("datasetId") Integer datasetId);

}
