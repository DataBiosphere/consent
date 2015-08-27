package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.DataSet;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.*;

@UseStringTemplate3StatementLocator
@RegisterMapper({DataSetMapper.class})
public interface DataSetDAO extends Transactional<DataSetDAO> {

    @SqlQuery("select dataSetId from dataset where dataSetId = :dataSetId")
    Integer checkDataSetbyId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select * from dataset where dataSetId = :dataSetId")
    DataSet findDataSetById(@Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("insert into dataset (name, createDate) values ( :name, :createDate)")
    @GetGeneratedKeys
    Integer insertDataSet(@Bind("name") String name,
                          @Bind("createDate") Date createDate);


    @SqlBatch("insert into dataset (name, createDate) values (:name, :createDate)")
    void insertUserRoles(@Bind("roleId") List<Integer> roleIds,
                         @Bind("dacUserId") Integer userId);

    @SqlBatch("insert into dataset (name, createDate, objectId) values (:name, :createDate, :objectId)")
    void insertAll(@BindBean ArrayList<DataSet> dataSets);

    @Mapper(BatchMapper.class)
    @SqlQuery("select datasetId, objectId  from dataset where objectId in (<objectIdList>)")
    List<Map<String,Integer>> searchByObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

}





