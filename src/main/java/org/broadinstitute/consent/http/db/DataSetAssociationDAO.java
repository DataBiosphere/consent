package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mongo.DatasetAssociationMapper;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.List;


@UseStringTemplate3StatementLocator
@RegisterMapper({DatasetAssociationMapper.class})
public interface DataSetAssociationDAO extends Transactional<DataSetAssociationDAO> {

    @SqlBatch("insert into dataset_user_association (datasetId, dacuserId, createDate )" +
            " values (:datasetId, :dacuserId, :createDate)")
    void insertDatasetUserAssociation(@BindBean Collection<DatasetAssociation> associationList) throws UnableToExecuteStatementException;

    @SqlQuery("select * from dataset_user_association where datasetId = :datasetId")
    List<DatasetAssociation> getDatasetAssociation(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select * from dataset_user_association where datasetId in (<dataSetIdList>)")
    List<DatasetAssociation> getDatasetAssociations(@BindIn("dataSetIdList") List<Integer> dataSetIdList);

    @SqlUpdate("delete from dataset_user_association where datasetId = :datasetId")
    void delete(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select exists (select * from dataset_user_association where datasetId = :datasetId )")
    Boolean exist(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select ds.dacuserId from dataset_user_association ds where ds.datasetId = :datasetId")
    List<Integer> getDataOwnersOfDataSet(@Bind("datasetId") Integer datasetId);

    @SqlQuery("SELECT dua.dataSetId FROM consent.dataset_user_association dua inner join dataset ds on  ds.datasetId = dua.datasetId and " +
              " dua.dacUserId = :dacuserId and ds.needs_approval = true")
    List<Integer> getDataSetsIdOfDataOwnerNeedsApproval(@Bind("dacuserId") Integer dacuserId);

    @SqlQuery("select count(*) from dataset_user_association where dataSetId in (<dataSetIdList>)")
    List<Integer> getCountOfDataOwnersPerDataSet(@BindIn("dataSetIdList") List<Integer> dataSetIdList);

    @SqlQuery("select * from dataset_user_association ds where ds.dacuserId = :dacUserId")
    List<DatasetAssociation> findAllDatasetAssociationsByOwnerId(@Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("delete from dataset_user_association where dacuserId = :ownerId")
    void deleteDatasetRelationshipsForUser(@Bind("ownerId") Integer ownerId);

}