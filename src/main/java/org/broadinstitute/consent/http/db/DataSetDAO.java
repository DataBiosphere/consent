package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.mapper.AssociationMapper;
import org.broadinstitute.consent.http.db.mapper.BatchMapper;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DataSetMapper;
import org.broadinstitute.consent.http.db.mapper.DataSetPropertiesMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetPropertyMapper;
import org.broadinstitute.consent.http.db.mapper.DictionaryMapper;
import org.broadinstitute.consent.http.db.mapper.ImmutablePairOfIntsMapper;
import org.broadinstitute.consent.http.models.Association;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.resources.Resource;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DataSetMapper.class)
public interface DataSetDAO extends Transactional<DataSetDAO> {

    String CHAIRPERSON = Resource.CHAIRPERSON;

    @SqlUpdate("insert into dataset (name, createDate, objectId, active, alias) values (:name, :createDate, :objectId, :active, :alias)")
    @GetGeneratedKeys
    Integer insertDataset(@Bind("name") String name, @Bind("createDate") Date createDate, @Bind("objectId") String objectId, @Bind("active") Boolean active, @Bind("alias") Integer alias);

    @SqlUpdate("INSERT INTO dataset (name, createdate, create_user_id, update_date, update_user_id, objectId, active, alias) VALUES (:name, :createDate, :createUserId, :createDate, :createUserId, :objectId, :active, :alias)")
    @GetGeneratedKeys
    Integer insertDatasetV2(@Bind("name") String name, @Bind("createDate") Timestamp createDate, @Bind("createUserId") Integer createUserId, @Bind("objectId") String objectId, @Bind("active") Boolean active, @Bind("alias") Integer alias);

    @SqlQuery("select * from dataset where dataSetId = :dataSetId")
    DataSet findDataSetById(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select * from dataset where dataSetId in (<dataSetIdList>)")
    List<DataSet> findDataSetsByIdList(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    @SqlQuery("select * from dataset where objectId = :objectId")
    DataSet findDataSetByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("select * from dataset where objectId = :objectId")
    Integer findDataSetIdByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("SELECT * FROM dataset WHERE dataSetId IN (<dataSetIdList>) AND needs_approval = true")
    List<DataSet> findNeedsApprovalDataSetByDataSetId(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    @SqlBatch("insert into dataset (name, createDate, objectId, active, alias) values (:name, :createDate, :objectId, :active, :alias)")
    void insertAll(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("update dataset set name = :name, createDate = :createDate, active = :active, alias = :alias where dataSetId = :dataSetId")
    void updateAll(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("update dataset set name = :name, active = :active, createDate = :createDate, alias = :alias where objectId = :objectId")
    void updateAllByObjectId(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("insert into datasetproperty (dataSetId, propertyKey, propertyValue, createDate )" +
            " values (:dataSetId, :propertyKey, :propertyValue, :createDate)")
    void insertDataSetsProperties(@BindBean List<DataSetProperty> dataSetPropertiesList);

    @SqlBatch("delete from datasetproperty where dataSetId = :dataSetId")
    void deleteDataSetsProperties(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlUpdate("UPDATE datasetproperty SET propertyvalue = :propertyValue WHERE datasetid = :datasetId AND propertykey = :propertyKey")
    void updateDatasetProperty(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey, @Bind("propertyValue") String propertyValue);

    @SqlUpdate("DELETE from datasetproperty WHERE datasetid = :datasetId AND propertykey = :propertyKey")
    void deleteDatasetPropertyByKey(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey);

    @SqlBatch("delete from dataset where dataSetId = :dataSetId")
    void deleteDataSets(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlUpdate("UPDATE dataset SET active = null, name = null, createdate = null, needs_approval = 0 WHERE datasetid = :dataSetId")
    void logicalDatasetDelete(@Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("update dataset set active = :active where dataSetId = :dataSetId")
    void updateDataSetActive(@Bind("dataSetId") Integer dataSetId, @Bind("active") Boolean active);

    @SqlUpdate("update dataset set needs_approval = :needs_approval where dataSetId = :dataSetId")
    void updateDataSetNeedsApproval(@Bind("dataSetId") Integer dataSetId, @Bind("needs_approval") Boolean needs_approval);

    @SqlUpdate("UPDATE dataset SET update_date = :updateDate, update_user_id = :updateUserId WHERE datasetid = :datasetId")
    void updateDatasetUpdateUserAndDate(@Bind("datasetId") Integer datasetId, @Bind("updateDate") Timestamp updateDate, @Bind("updateUserId") Integer updateUserId);

    @UseRowMapper(DataSetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause " +
          "FROM dataset d " +
          "INNER JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
          "INNER JOIN dictionary k ON k.keyid = dp.propertykey " +
          "INNER JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
          "INNER JOIN consents c ON c.consentid = ca.consentid " +
          "INNER JOIN user_role ur ON ur.dac_id = c.dac_id " +
          "INNER JOIN dacuser u ON ur.user_id = u.dacuserid " +
          "WHERE u.dacuserid = :dacUserId AND d.name IS NOT NULL " +
          "ORDER BY d.datasetid, k.displayorder")
    Set<DataSetDTO> findDatasetsByUser(@Bind("dacUserId") Integer dacUserId);

    @UseRowMapper(DataSetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause " +
          "FROM dataset d " +
          "INNER JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
          "INNER JOIN dictionary k ON k.keyid = dp.propertykey " +
          "INNER JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
          "INNER JOIN consents c ON c.consentid = ca.consentid " +
          "WHERE d.name IS NOT NULL AND d.active = true " +
          "ORDER BY d.datasetid, k.displayorder")
    Set<DataSetDTO> findActiveDatasets();

    @UseRowMapper(DataSetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause " +
          "FROM dataset d " +
          "INNER JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
          "INNER JOIN dictionary k ON k.keyid = dp.propertykey " +
          "INNER JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
          "INNER JOIN consents c ON c.consentid = ca.consentid " +
          "ORDER BY d.datasetid, k.displayorder")
    Set<DataSetDTO> findAllDatasets();

    @UseRowMapper(DataSetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause " +
          "FROM dataset d " +
          "LEFT OUTER JOIN datasetproperty dp on dp.datasetid = d.datasetid " +
          "LEFT OUTER JOIN dictionary k on k.keyid = dp.propertykey " +
          "LEFT OUTER JOIN consentassociations ca on ca.datasetid = d.datasetid " +
          "LEFT OUTER JOIN consents c on c.consentid = ca.consentid " +
          "WHERE d.datasetid = :datasetId ORDER BY d.datasetid, k.displayorder")
    Set<DataSetDTO> findDatasetDTOWithPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @UseRowMapper(DatasetPropertyMapper.class)
    @SqlQuery(
        "SELECT * FROM datasetproperty WHERE datasetid = :datasetId"
    )
    Set<DataSetProperty> findDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @UseRowMapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId, c.dac_id, c.translatedUseRestriction, c.datause " +
            "from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey " +
            "inner join consentassociations ca on ca.dataSetId = d.dataSetId inner join consents c on c.consentId = ca.consentId " +
            "where d.dataSetId in (<dataSetIdList>) order by d.dataSetId, k.receiveOrder")
    Set<DataSetDTO> findDataSetsByReceiveOrder(@BindList("dataSetIdList") List<Integer> dataSetIdList);


    @SqlQuery("select *  from dataset where objectId in (<objectIdList>) ")
    List<DataSet> searchDataSetsByObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @SqlQuery("select ds.dataSetId  from dataset ds where ds.objectId in (<objectIdList>) ")
    List<Integer> searchDataSetsIdsByObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d order by receiveOrder")
    List<Dictionary> getMappedFieldsOrderByReceiveOrder();

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d WHERE d.displayOrder is not null  order by displayOrder")
    List<Dictionary> getMappedFieldsOrderByDisplayOrder();

    @SqlQuery("SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>)")
    List<DataSet> getDataSetsForObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @SqlQuery("SELECT * FROM dataset d WHERE d.name IN (<names>)")
    List<DataSet> getDataSetsForNameList(@BindList("names") List<String> names);

    @SqlQuery("SELECT ds.* FROM consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId WHERE ca.consentId = :consentId")
    List<DataSet> getDataSetsForConsent(@Bind("consentId") String consentId);

    @RegisterRowMapper(AssociationMapper.class)
    @SqlQuery("SELECT * FROM consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.objectId IN (<objectIdList>)")
    List<Association> getAssociationsForObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @RegisterRowMapper(AssociationMapper.class)
    @SqlQuery("SELECT * FROM consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.dataSetId IN (<dataSetIdList>)")
    List<Association> getAssociationsForDataSetIdList(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    @SqlQuery("SELECT ca.associationId FROM consentassociations ca INNER JOIN dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.objectId = :objectId")
    Integer getConsentAssociationByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("SELECT ca.consentId FROM consentassociations ca INNER JOIN dataset ds on ds.dataSetId = ca.dataSetId WHERE ds.dataSetId = :dataSetId")
    String getAssociatedConsentIdByDataSetId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("SELECT dataSetId FROM dataset WHERE name = :name")
    Integer getDatasetIdByName(@Bind("name") String name);

    @SqlQuery("SELECT * FROM dataset WHERE LOWER(name) LIKE LOWER(:name)")
    DataSet getDatasetByName(@Bind("name") String name);

    @SqlQuery("select *  from dataset where name in (<names>) ")
    List<DataSet> searchDataSetsByNameList(@BindList("names") List<String> names);

    @UseRowMapper(BatchMapper.class)
    @SqlQuery("select dataSetId, name  from dataset where name in (<nameList>)")
    List<Map<String,Integer>> searchByNameIdList(@BindList("nameList") List<String> nameList);

    @SqlQuery(" SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>) AND d.name is not null")
    List<DataSet> getDataSetsWithValidNameForObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @SqlQuery("SELECT * FROM dataset WHERE datasetid in (<dataSetIds>) ")
    List<DataSet> findDatasetsByIdList(@BindList("dataSetIds") List<Integer> dataSetIds);

    @SqlQuery("select MAX(alias) from dataset")
    Integer findLastAlias();

    /**
     * User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
     *
     * @param email User email
     * @return List of datasets that are visible to the user via DACs.
     */
    @SqlQuery(" select d.* from dataset d " +
            " inner join consentassociations a on d.dataSetId = a.dataSetId " +
            " inner join consents c on a.consentId = c.consentId " +
            " inner join user_role ur on ur.dac_id = c.dac_id " +
            " inner join dacuser u on ur.user_id = u.dacUserId and u.email = :email ")
    List<DataSet> findDataSetsByAuthUserEmail(@Bind("email") String email);

    /**
     * DACs -> Consents -> Consent Associations -> DataSets
     *
     * @return List of datasets that are not owned by a DAC.
     */
    @SqlQuery(" select d.* from dataset d " +
            " inner join consentassociations a on d.dataSetId = a.dataSetId " +
            " inner join consents c on a.consentId = c.consentId " +
            " where c.dac_id is null ")
    List<DataSet> findNonDACDataSets();

    /**
     * DACs -> Consents -> Consent Associations -> DataSets
     * DataSets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to a single DAC.
     */
    @UseRowMapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, p.propertyValue, c.consentId, c.dac_id, c.translatedUseRestriction, c.datause from dataset d " +
            " left outer join datasetproperty p on p.dataSetId = d.dataSetId " +
            " left outer join dictionary k on k.keyId = p.propertyKey " +
            " inner join consentassociations a on a.dataSetId = d.dataSetId " +
            " inner join consents c on c.consentId = a.consentId " +
            " where c.dac_id = :dacId ")
    Set<DataSetDTO> findDatasetsByDac(@Bind("dacId") Integer dacId);

    /**
     * DACs -> Consents -> Consent Associations -> DataSets
     * DataSets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to any Dac.
     */
    @UseRowMapper(DataSetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, p.propertyvalue, c.consentid, c.dac_id, c.translateduserestriction, c.datause " +
            " FROM dataset d " +
            " LEFT OUTER JOIN datasetproperty p ON p.datasetid = d.datasetid " +
            " LEFT OUTER JOIN dictionary k ON k.keyid = p.propertykey " +
            " INNER JOIN consentassociations a ON a.datasetid = d.datasetid " +
            " INNER JOIN consents c ON c.consentid = a.consentid " +
            " WHERE c.dac_id IS NOT NULL ")
    Set<DataSetDTO> findDatasetsWithDacs();

    /**
     * DACs -> Consents -> Consent Associations -> DataSets
     *
     * @return List of dataset id and its associated dac id
     */
    @RegisterRowMapper(ImmutablePairOfIntsMapper.class)
    @SqlQuery("select distinct d.dataSetId, c.dac_id from dataset d " +
            " inner join consentassociations a on d.dataSetId = a.dataSetId " +
            " inner join consents c on a.consentId = c.consentId " +
            " where c.dac_id is not null ")
    List<Pair<Integer, Integer>> findDatasetAndDacIds();

    /**
     * Find the Dac for this dataset.
     *
     * DACs -> Consents -> Consent Associations -> DataSets
     *
     * @param datasetId The dataset Id
     * @return The DAC that corresponds to this dataset
     */
    @RegisterRowMapper(DacMapper.class)
    @SqlQuery("select d.* from dac d " +
            " inner join consents c on d.dac_id = c.dac_id " +
            " inner join consentassociations a on a.consentId = c.consentId " +
            " where a.dataSetId = :datasetId " +
            " limit 1 ")
    Dac findDacForDataset(@Bind("datasetId") Integer datasetId);

    @UseRowMapper(DataSetMapper.class)
    @SqlQuery("select d.* from dataset d " +
            " inner join consentassociations a on a.dataSetId = d.dataSetId and a.consentId = :consentId " +
            " where d.active = true ")
    Set<DataSet> findDatasetsForConsentId(@Bind("consentId") String consentId);

}
