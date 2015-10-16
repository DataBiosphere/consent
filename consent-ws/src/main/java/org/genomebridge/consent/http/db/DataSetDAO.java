package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Association;
import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.models.DataSetProperty;
import org.genomebridge.consent.http.models.Dictionary;
import org.genomebridge.consent.http.models.dto.DataSetDTO;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UseStringTemplate3StatementLocator
@RegisterMapper({DataSetMapper.class})
public interface DataSetDAO extends Transactional<DataSetDAO> {

    @SqlQuery("select dataSetId from dataset where dataSetId = :dataSetId")
    Integer checkDataSetbyId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select * from dataset where dataSetId = :dataSetId")
    DataSet findDataSetById(@Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("insert into dataset (name, createDate) values ( :name, :createDate)")
    @GetGeneratedKeys
    Integer insertDataSet(@Bind("name") String name, @Bind("createDate") Date createDate);

    @SqlBatch("insert into dataset (name, createDate, objectId) values (:name, :createDate, :objectId)")
    void insertAll(@BindBean List<DataSet> dataSets);

    @SqlBatch("insert into datasetproperty (dataSetId, propertyKey, propertyValue, createDate )" +
            " values (:dataSetId, :propertyKey, :propertyValue, :createDate)")
    void insertDataSetsProperties(@BindBean List<DataSetProperty> dataSetPropertiesList);

    @SqlBatch("delete from datasetproperty where dataSetId = :dataSetId")
    void deleteDataSetsProperties(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlBatch("delete from dataset where dataSetId = :dataSetId")
    void deleteDataSets(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey inner join consentassociations ca on ca.objectId = d.objectId order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSets();

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey inner join consentassociations ca on ca.objectId = d.objectId where d.objectId in (<objectIdList>) order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSets(@BindIn("objectIdList") List<String> objectIdList);
    
    @Mapper(BatchMapper.class)
    @SqlQuery("select datasetId, objectId  from dataset where objectId in (<objectIdList>)")
    List<Map<String,Integer>> searchByObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @RegisterMapper({DictionaryMapper.class})
    @SqlQuery("SELECT * FROM dictionary d where d.required = true order by displayOrder")
    List<Dictionary> getRequiredFields();

    @RegisterMapper({DictionaryMapper.class})
    @SqlQuery("SELECT * FROM dictionary d order by displayOrder")
    List<Dictionary> getMappedFields();

    @SqlQuery("SELECT COUNT(*) FROM consentassociations ca WHERE ca.objectId IN (<objectIdList>)")
    Integer consentAssociationCount(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery(" SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>)" +
              " AND NOT EXISTS (SELECT * FROM consent.consentassociations c WHERE d.objectId = c.objectId)")
    List<DataSet> missingAssociations(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery(" SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>)")
    List<DataSet> getDataSetsForObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery("SELECT * FROM dataset d WHERE d.objectId IN (Select objectId FROM consentassociations Where consentId = :consentId)")
    List<DataSet> getDataSetsForConsent(@Bind("consentId") String consentId);

    @RegisterMapper({AssociationMapper.class})
    @SqlQuery(" SELECT * FROM consentassociations ca WHERE ca.objectId IN (<objectIdList>)")
    List<Association> getAssociationsForObjectIdList(@BindIn("objectIdList") List<String> objectIdList);
    
    @SqlQuery(" SELECT objectId FROM dataset d WHERE d.objectId like concat('%',:partial,'%')")
    List<String> getObjectIdsbyPartial(@Bind("partial") String partial);

}





