package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DatasetAssociationMapper;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Collection;
import java.util.List;


@RegisterRowMapper(DatasetAssociationMapper.class)
public interface DatasetAssociationDAO extends Transactional<DatasetAssociationDAO> {

    @SqlBatch("insert into dataset_user_association (datasetid, dacuserid, createdate )" +
            " values (:datasetId, :dacuserId, :createDate)")
    void insertDatasetUserAssociation(@BindBean Collection<DatasetAssociation> associationList) throws UnableToExecuteStatementException;

    @SqlQuery("select * from dataset_user_association where datasetid = :datasetId")
    List<DatasetAssociation> getDatasetAssociation(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select * from dataset_user_association where datasetid in (<datasetIdList>)")
    List<DatasetAssociation> getDatasetAssociations(@BindList("datasetIdList") List<Integer> datasetIdList);

    @SqlUpdate("delete from dataset_user_association where datasetid = :datasetId")
    void delete(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select exists (select * from dataset_user_association where datasetid = :datasetId )")
    Boolean exist(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select ds.dacuserId from dataset_user_association ds where ds.datasetid = :datasetId")
    List<Integer> getDataOwnersOfDataset(@Bind("datasetId") Integer datasetId);

}