package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.mapper.AssociationMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetPropertiesMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetPropertyMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetReducer;
import org.broadinstitute.consent.http.db.mapper.DictionaryMapper;
import org.broadinstitute.consent.http.db.mapper.ImmutablePairOfIntsMapper;
import org.broadinstitute.consent.http.models.Association;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.resources.Resource;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DatasetMapper.class)
public interface DatasetDAO extends Transactional<DatasetDAO> {

    String CHAIRPERSON = Resource.CHAIRPERSON;

    @SqlUpdate("INSERT INTO dataset (name, createdate, create_user_id, update_date, update_user_id, objectId, active, alias) (SELECT :name, :createDate, :createUserId, :createDate, :createUserId, :objectId, :active, MAX(alias)+1 FROM dataset)")
    @GetGeneratedKeys
    Integer insertDataset(@Bind("name") String name, @Bind("createDate") Timestamp createDate, @Bind("createUserId") Integer createUserId, @Bind("objectId") String objectId, @Bind("active") Boolean active);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid " +
        " WHERE d.datasetid = :datasetId")
    Dataset findDatasetById(@Bind("datasetId") Integer datasetId);

    @SqlQuery("SELECT datasetid FROM dataset WHERE objectid = :objectId")
    Integer findDatasetIdByObjectId(@Bind("objectId") String objectId);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid " +
        " WHERE d.datasetid IN (<datasetIdList>) " +
        " AND d.needs_approval = true ")
    List<Dataset> findNeedsApprovalDatasetByDatasetId(@BindList("datasetIdList") List<Integer> datasetIdList);

    @Deprecated
    @SqlBatch("INSERT INTO dataset (name, createdate, objectid, active, alias) VALUES (:name, :createDate, :objectId, :active, :alias)")
    void insertAll(@BindBean Collection<Dataset> dataSets);

    @SqlBatch("INSERT INTO datasetproperty (datasetid, propertykey, schema_property, propertyvalue, property_type, createdate )" +
            " VALUES (:dataSetId, :propertyKey, :schemaProperty, :getPropertyValueAsString, :getPropertyTypeAsString, :createDate)")
    void insertDatasetProperties(@BindBean @BindMethods List<DatasetProperty> dataSetPropertiesList);

    @SqlBatch("DELETE FROM datasetproperty WHERE datasetid = :dataSetId")
    void deleteDatasetsProperties(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlUpdate("DELETE FROM datasetproperty WHERE datasetid = :datasetId")
    void deleteDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("INSERT INTO dataset_audit (datasetid, changeaction, modifiedbyuser, modificationdate, objectid, name, active) VALUES (:dataSetId, :action, :user, :date, :objectId, :name, :active )")
    @GetGeneratedKeys
    Integer insertDatasetAudit(@BindBean DatasetAudit dataSets);

    @SqlUpdate("DELETE FROM dataset_user_association WHERE datasetid = :datasetId")
    void deleteUserAssociationsByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("DELETE FROM consentassociations WHERE datasetid = :datasetId")
    void deleteConsentAssociationsByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("UPDATE datasetproperty SET propertyvalue = :propertyValue WHERE datasetid = :datasetId AND propertykey = :propertyKey")
    void updateDatasetProperty(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey, @Bind("propertyValue") String propertyValue);

    @SqlUpdate("DELETE from datasetproperty WHERE datasetid = :datasetId AND propertykey = :propertyKey")
    void deleteDatasetPropertyByKey(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey);

    @SqlUpdate("DELETE FROM dataset WHERE datasetid = :datasetId")
    void deleteDatasetById(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("UPDATE dataset SET active = :active WHERE datasetid = :datasetId")
    void updateDatasetActive(@Bind("datasetId") Integer datasetId, @Bind("active") Boolean active);

    @SqlUpdate("UPDATE dataset SET needs_approval = :needsApproval WHERE datasetid = :datasetId")
    void updateDatasetNeedsApproval(@Bind("datasetId") Integer datasetId, @Bind("needsApproval") Boolean needsApproval);

    @SqlUpdate("UPDATE dataset " +
            " SET name = :datasetName," +
            " update_date = :updateDate, " +
            " update_user_id = :updateUserId, " +
            " needs_approval = :needsApproval " +
            " WHERE datasetid = :datasetId")
    void updateDataset(@Bind("datasetId") Integer datasetId, @Bind("datasetName") String datasetName, @Bind("updateDate") Timestamp updateDate, @Bind("updateUserId") Integer updateUserId, @Bind("needsApproval") Boolean needsApproval);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid " +
        " WHERE d.datasetid IN (<datasetIds>)" +
        " ORDER BY d.datasetid, k.displayorder")
    Set<Dataset> findDatasetWithDataUseByIdList(@BindList("datasetIds") List<Integer> datasetIds);

    @Deprecated
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery(
        " SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause "
            + " FROM dataset d "
            + " LEFT OUTER JOIN datasetproperty dp ON dp.datasetid = d.datasetid "
            + " LEFT OUTER JOIN dictionary k ON k.keyid = dp.propertykey "
            + " INNER JOIN consentassociations ca ON ca.datasetid = d.datasetid "
            + " INNER JOIN consents c ON c.consentid = ca.consentid "
            + " INNER JOIN user_role ur ON ur.dac_id = c.dac_id "
            + " WHERE ur.user_id = :userId "
            + " AND d.name IS NOT NULL "
            + " ORDER BY d.datasetid ")
    Set<DatasetDTO> findDatasetsByUserId(@Bind("userId") Integer userId);

    @Deprecated
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery(
        " SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause "
            + " FROM dataset d "
            + " LEFT OUTER JOIN datasetproperty dp ON dp.datasetid = d.datasetid "
            + " LEFT OUTER JOIN dictionary k ON k.keyid = dp.propertykey "
            + " LEFT OUTER JOIN consentassociations ca ON ca.datasetid = d.datasetid "
            + " LEFT OUTER JOIN consents c ON c.consentid = ca.consentid "
            + " WHERE d.name IS NOT NULL AND d.active = true "
            + " ORDER BY d.datasetid ")
    Set<DatasetDTO> findActiveDatasets();

    @Deprecated
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery(
        " SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause "
            + " FROM dataset d "
            + " LEFT OUTER JOIN datasetproperty dp ON dp.datasetid = d.datasetid "
            + " LEFT OUTER JOIN dictionary k ON k.keyid = dp.propertykey "
            + " LEFT OUTER JOIN consentassociations ca ON ca.datasetid = d.datasetid "
            + " LEFT OUTER JOIN consents c ON c.consentid = ca.consentid "
            + " ORDER BY d.datasetid ")
    Set<DatasetDTO> findAllDatasets();

    @Deprecated

    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause " +
          "FROM dataset d " +
          "LEFT OUTER JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
          "LEFT OUTER JOIN dictionary k ON k.keyid = dp.propertykey " +
          "LEFT OUTER JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
          "LEFT OUTER JOIN consents c ON c.consentid = ca.consentid " +
          "WHERE d.datasetid = :datasetId ORDER BY d.datasetid, k.displayorder")
    Set<DatasetDTO> findDatasetDTOWithPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @UseRowMapper(DatasetPropertyMapper.class)
    @SqlQuery(
        "SELECT * FROM datasetproperty WHERE datasetid = :datasetId"
    )
    Set<DatasetProperty> findDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @Deprecated
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyValue, ca.consentId, c.dac_id, c.translatedUseRestriction, c.datause " +
            "FROM dataset d INNER JOIN datasetproperty dp ON dp.datasetid = d.datasetid INNER JOIN dictionary k ON k.keyId = dp.propertyKey " +
            "INNER JOIN consentassociations ca ON ca.datasetid = d.datasetid INNER JOIN consents c ON c.consentId = ca.consentId " +
            "WHERE d.datasetid IN (<dataSetIdList>) ORDER BY d.datasetid, k.receiveOrder")
    Set<DatasetDTO> findDatasetsByReceiveOrder(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d ORDER BY receiveOrder")
    List<Dictionary> getMappedFieldsOrderByReceiveOrder();

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d WHERE d.displayOrder IS NOT NULL  ORDER BY displayOrder")
    List<Dictionary> getMappedFieldsOrderByDisplayOrder();

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid " +
        " WHERE d.objectid IN (<objectIdList>) ")
    List<Dataset> getDatasetsForObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid ")
    List<Dataset> getAllDatasets();

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid " +
        " WHERE d.name IS NOT NULL AND d.active = true ")
    List<Dataset> getActiveDatasets();

    @SqlQuery("SELECT ds.* FROM consentassociations ca INNER JOIN dataset ds ON ds.datasetid = ca.datasetid WHERE ca.consentId = :consentId")
    List<Dataset> getDatasetsForConsent(@Bind("consentId") String consentId);

    @SqlQuery("SELECT ca.consentId FROM consentassociations ca INNER JOIN dataset ds on ds.datasetid = ca.datasetid WHERE ds.datasetid = :dataSetId")
    String getAssociatedConsentIdByDatasetId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("SELECT * FROM dataset WHERE LOWER(name) = LOWER(:name)")
    Dataset getDatasetByName(@Bind("name") String name);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid " +
        " WHERE d.datasetid in (<datasetIds>) ")
    List<Dataset> findDatasetsByIdList(@BindList("datasetIds") List<Integer> datasetIds);

    @RegisterRowMapper(AssociationMapper.class)
    @SqlQuery("SELECT * FROM consentassociations ca INNER JOIN dataset ds ON ds.datasetid = ca.datasetid WHERE ds.datasetid IN (<dataSetIdList>)")
    List<Association> getAssociationsForDatasetIdList(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    /**
     * User -> UserRoles -> DACs -> Consents -> Consent Associations -> Datasets
     *
     * @param email User email
     * @return List of datasets that are visible to the user via DACs.
     */
    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " LEFT JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " LEFT JOIN consents c ON c.consentid = ca.consentid " +
        " INNER JOIN user_role ur ON ur.dac_id = c.dac_id " +
        " INNER JOIN users u ON ur.user_id = u.user_id AND u.email = :email ")
    List<Dataset> findDatasetsByAuthUserEmail(@Bind("email") String email);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     *
     * @return List of datasets that are not owned by a DAC.
     */
    @SqlQuery(" SELECT d.* from dataset d " +
            " INNER JOIN consentassociations a ON d.datasetid = a.datasetid " +
            " INNER JOIN consents c ON a.consentId = c.consentId " +
            " WHERE c.dac_id IS NULL ")
    List<Dataset> findNonDACDatasets();

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to a single DAC.
     */
    @Deprecated
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, p.propertyValue, c.consentId, c.dac_id, c.translatedUseRestriction, c.datause FROM dataset d " +
            " LEFT OUTER JOIN datasetproperty p ON p.datasetid = d.datasetid " +
            " LEFT OUTER JOIN dictionary k ON k.keyId = p.propertyKey " +
            " INNER JOIN consentassociations a ON a.datasetid = d.datasetid " +
            " INNER JOIN consents c ON c.consentId = a.consentId " +
            " WHERE c.dac_id = :dacId ")
    Set<DatasetDTO> findDatasetsByDac(@Bind("dacId") Integer dacId);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated with the provided DAC IDs
     */
    @Deprecated
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, p.propertyValue, c.consentId, c.dac_id, c.translatedUseRestriction, c.datause FROM dataset d " +
            " LEFT OUTER JOIN datasetproperty p ON p.datasetid = d.datasetid " +
            " LEFT OUTER JOIN dictionary k ON k.keyId = p.propertyKey " +
            " INNER JOIN consentassociations a ON a.datasetid = d.datasetid " +
            " INNER JOIN consents c ON c.consentId = a.consentId " +
            " WHERE c.dac_id IN (<dacIds>) ")
    Set<DatasetDTO> findDatasetsByDacIds(@BindList("dacIds") List<Integer> dacIds);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to any Dac.
     */
    @Deprecated
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, p.propertyvalue, c.consentid, c.dac_id, c.translateduserestriction, c.datause " +
            " FROM dataset d " +
            " LEFT OUTER JOIN datasetproperty p ON p.datasetid = d.datasetid " +
            " LEFT OUTER JOIN dictionary k ON k.keyid = p.propertykey " +
            " INNER JOIN consentassociations a ON a.datasetid = d.datasetid " +
            " INNER JOIN consents c ON c.consentid = a.consentid " +
            " WHERE c.dac_id IS NOT NULL ")
    Set<DatasetDTO> findDatasetsWithDacs();

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     *
     * @return List of dataset id and its associated dac id
     */
    @RegisterRowMapper(ImmutablePairOfIntsMapper.class)
    @SqlQuery("SELECT DISTINCT d.datasetid, c.dac_id FROM dataset d " +
            " INNER JOIN consentassociations a ON d.datasetid = a.datasetid " +
            " INNER JOIN consents c ON a.consentId = c.consentId " +
            " WHERE c.dac_id IS NOT NULL ")
    List<Pair<Integer, Integer>> findDatasetAndDacIds();

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(" SELECT d.*, k.key, dp.propertyvalue, dp.propertykey, dp.property_type, dp.schema_property, dp.propertyid, ca.consentid, c.dac_id, c.translateduserestriction, c.datause, dar_ds_ids.id as in_use " +
        " FROM dataset d " +
        " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.datasetid " +
        " LEFT JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
        " LEFT JOIN dictionary k ON k.keyid = dp.propertykey " +
        " INNER JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
        " INNER JOIN consents c ON c.consentid = ca.consentid " +
        " WHERE c.consentid = :consentId " +
        " AND d.active = true ")
    Set<Dataset> findDatasetsForConsentId(@Bind("consentId") String consentId);

}
