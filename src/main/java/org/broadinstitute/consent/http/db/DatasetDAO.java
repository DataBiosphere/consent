package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.mapper.AssociationMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetDTOWithPropertiesMapper;
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

    @SqlUpdate(
            "INSERT INTO dataset "
            + "(name, create_date, create_user_id, update_date, "
                + "update_user_id, object_id, active, dac_id, alias, data_use) "
            + "(SELECT :name, :createDate, :createUserId, :createDate, "
                + ":createUserId, :objectId, :active, :dacId, COALESCE(MAX(alias),0)+1, :dataUse FROM dataset)")
    @GetGeneratedKeys
    Integer insertDataset(
        @Bind("name") String name,
        @Bind("createDate") Timestamp createDate,
        @Bind("createUserId") Integer createUserId,
        @Bind("objectId") String objectId,
        @Bind("active") Boolean active,
        @Bind("dataUse") String dataUse,
        @Bind("dacId") Integer dacId);

    @SqlUpdate(
            "INSERT INTO dataset "
            + "(name, create_date, create_user_id, update_date, "
                + "update_user_id, object_id, active, dac_id, alias, data_use, sharing_plan_document, "
                + " sharing_plan_document_name) "
            + "(SELECT :name, :createDate, :createUserId, :createDate, "
                + ":createUserId, :objectId, :active, :dacId, COALESCE(MAX(alias),0)+1, :dataUse, "
                + ":sharingPlanDocument, :sharingPlanDocumentName "
                + " FROM dataset)")
    @GetGeneratedKeys
    Integer insertDataset(
        @Bind("name") String name,
        @Bind("createDate") Timestamp createDate,
        @Bind("createUserId") Integer createUserId,
        @Bind("objectId") String objectId,
        @Bind("active") Boolean active,
        @Bind("dataUse") String dataUse,
        @Bind("dacId") Integer dacId,
        @Bind("sharingPlanDocument") String sharingPlanDocument,
        @Bind("sharingPlanDocumentName") String sharingPlanDocumentName);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.dataset_id = :datasetId")
    Dataset findDatasetById(@Bind("datasetId") Integer datasetId);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.alias = :alias")
    Dataset findDatasetByAlias(@Bind("alias") Integer alias);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
            "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
                    " FROM dataset d " +
                    " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
                    " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
                    " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
                    " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
                    " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
                    " WHERE d.alias IN (<aliases>)")
    List<Dataset> findDatasetsByAlias(@BindList("aliases") List<Integer> aliases);

    @SqlQuery("SELECT dataset_id FROM dataset WHERE object_id = :objectId")
    Integer findDatasetIdByObjectId(@Bind("objectId") String objectId);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.dataset_id IN (<datasetIdList>) " +
                " AND d.needs_approval = true ")
    List<Dataset> findNeedsApprovalDatasetByDatasetId(@BindList("datasetIdList") List<Integer> datasetIdList);

    @Deprecated
    @SqlBatch("INSERT INTO dataset (name, create_date, object_id, active, alias, data_use) VALUES (:name, :createDate, :objectId, :active, :alias, :dataUse)")
    void insertAll(@BindBean Collection<Dataset> datasets);

    @SqlUpdate("UPDATE dataset SET dac_id = :dacId WHERE dataset_id = :datasetId")
    void updateDatasetDacId(@Bind("datasetId") Integer datasetId, @Bind("dacId") Integer dacId);

    @SqlBatch(
        "INSERT INTO dataset_property (dataset_id, property_key, schema_property, property_value, property_type, create_date )" +
            " VALUES (:dataSetId, :propertyKey, :schemaProperty, :getPropertyValueAsString, :getPropertyTypeAsString, :createDate)")
    void insertDatasetProperties(@BindBean @BindMethods List<DatasetProperty> dataSetPropertiesList);

    @SqlBatch("DELETE FROM dataset_property WHERE dataset_id = :dataSetId")
    void deleteDatasetsProperties(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlUpdate("DELETE FROM dataset_property WHERE dataset_id = :datasetId")
    void deleteDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate(
        "INSERT INTO dataset_audit "
            + "(dataset_id, change_action, modified_by_user, modification_date, object_id, name, active) "
            + "VALUES (:dataSetId, :action, :user, :date, :objectId, :name, :active )")
    @GetGeneratedKeys
    Integer insertDatasetAudit(@BindBean DatasetAudit dataSets);

    @SqlUpdate("DELETE FROM dataset_user_association WHERE datasetid = :datasetId")
    void deleteUserAssociationsByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("DELETE FROM consent_associations WHERE datasetid = :datasetId")
    void deleteconsent_associationsByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate(
        "UPDATE dataset_property "
            + "SET property_value = :propertyValue "
            + "WHERE dataset_id = :datasetId "
                + "AND property_key = :propertyKey")
    void updateDatasetProperty(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey, @Bind("propertyValue") String propertyValue);

    @SqlUpdate(
        "DELETE from dataset_property "
            + "WHERE dataset_id = :datasetId "
                + "AND property_key = :propertyKey")
    void deleteDatasetPropertyByKey(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey);

    @SqlUpdate("DELETE FROM dataset WHERE dataset_id = :datasetId")
    void deleteDatasetById(@Bind("datasetId") Integer datasetId);

    @SqlUpdate(
        "UPDATE dataset "
            + "SET active = :active "
            + "WHERE dataset_id = :datasetId")
    void updateDatasetActive(@Bind("datasetId") Integer datasetId, @Bind("active") Boolean active);

    @SqlUpdate(
        "UPDATE dataset "
            + "SET needs_approval = :needsApproval "
            + "WHERE dataset_id = :datasetId")
    void updateDatasetNeedsApproval(@Bind("datasetId") Integer datasetId, @Bind("needsApproval") Boolean needsApproval);

    @SqlUpdate(
            "UPDATE dataset " +
                " SET name = :datasetName," +
                    " update_date = :updateDate, " +
                    " update_user_id = :updateUserId, " +
                    " needs_approval = :needsApproval " +
                " WHERE dataset_id = :datasetId")
    void updateDataset(@Bind("datasetId") Integer datasetId, @Bind("datasetName") String datasetName, @Bind("updateDate") Timestamp updateDate, @Bind("updateUserId") Integer updateUserId, @Bind("needsApproval") Boolean needsApproval);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.dataset_id IN (<datasetIds>)" +
            " ORDER BY d.dataset_id, k.display_order")
    Set<Dataset> findDatasetWithDataUseByIdList(@BindList("datasetIds") List<Integer> datasetIds);

    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, ca.consent_id, d.dac_id, c.translated_use_restriction "
            + " FROM dataset d "
            + " LEFT OUTER JOIN dataset_property dp ON dp.dataset_id = d.dataset_id "
            + " LEFT OUTER JOIN dictionary k ON k.key_id = dp.property_key "
            + " INNER JOIN consent_associations ca ON ca.dataset_id = d.dataset_id "
            + " INNER JOIN consents c ON c.consent_id = ca.consent_id "
            + " INNER JOIN user_role ur ON ur.dac_id = d.dac_id "
            + " WHERE ur.user_id = :userId "
                + " AND d.name IS NOT NULL "
            + " ORDER BY d.dataset_id ")
    Set<DatasetDTO> findDatasetsByUserId(@Bind("userId") Integer userId);

    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, ca.consent_id, d.dac_id, c.translated_use_restriction "
            + " FROM dataset d "
            + " LEFT OUTER JOIN dataset_property dp ON dp.dataset_id = d.dataset_id "
            + " LEFT OUTER JOIN dictionary k ON k.key_id = dp.property_key "
            + " LEFT OUTER JOIN consent_associations ca ON ca.dataset_id = d.dataset_id "
            + " LEFT OUTER JOIN consents c ON c.consent_id = ca.consent_id "
            + " WHERE d.name IS NOT NULL AND d.active = true "
            + " ORDER BY d.dataset_id ")
    Set<DatasetDTO> findActiveDatasets();

    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, ca.consent_id, d.dac_id, c.translated_use_restriction "
            + " FROM dataset d "
            + " LEFT OUTER JOIN dataset_property dp ON dp.dataset_id = d.dataset_id "
            + " LEFT OUTER JOIN dictionary k ON k.key_id = dp.property_key "
            + " LEFT OUTER JOIN consent_associations ca ON ca.dataset_id = d.dataset_id "
            + " LEFT OUTER JOIN consents c ON c.consent_id = ca.consent_id "
            + " ORDER BY d.dataset_id ")
    Set<DatasetDTO> findAllDatasets();

    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, ca.consent_id, d.dac_id, c.translated_use_restriction " +
          "FROM dataset d " +
          "LEFT OUTER JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
          "LEFT OUTER JOIN dictionary k ON k.key_id = dp.property_key " +
          "LEFT OUTER JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
          "LEFT OUTER JOIN consents c ON c.consent_id = ca.consent_id " +
          "WHERE d.dataset_id = :datasetId ORDER BY d.dataset_id, k.display_order")
    Set<DatasetDTO> findDatasetDTOWithPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @UseRowMapper(DatasetPropertyMapper.class)
    @SqlQuery("SELECT * FROM dataset_property WHERE dataset_id = :datasetId")
    Set<DatasetProperty> findDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, ca.consent_id, d.dac_id, c.translated_use_restriction " +
            " FROM dataset d INNER JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " INNER JOIN dictionary k ON k.key_id = dp.property_key " +
            " INNER JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " INNER JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.dataset_id IN (<dataSetIdList>) ORDER BY d.dataset_id, k.receive_order ")
    Set<DatasetDTO> findDatasetsByReceiveOrder(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d ORDER BY receiveOrder")
    List<Dictionary> getMappedFieldsOrderByReceiveOrder();

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d WHERE d.displayOrder IS NOT NULL  ORDER BY displayOrder")
    List<Dictionary> getMappedFieldsOrderByDisplayOrder();

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.object_id IN (<objectIdList>) ")
    List<Dataset> getDatasetsForObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id ")
    List<Dataset> getAllDatasets();

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.name IS NOT NULL AND d.active = true ")
    List<Dataset> getActiveDatasets();

    @SqlQuery(
        "SELECT ds.* FROM consent_associations ca "
            + "INNER JOIN dataset ds ON ds.dataset_id = ca.dataset_id "
            + "WHERE ca.consent_id = :consentId")
    List<Dataset> getDatasetsForConsent(@Bind("consentId") String consentId);

    @SqlQuery(
        "SELECT ca.consent_id FROM consent_associations ca "
            + "INNER JOIN dataset ds on ds.dataset_id = ca.dataset_id "
            + "WHERE ds.dataset_id = :dataSetId")
    String getAssociatedConsentIdByDatasetId(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("SELECT * FROM dataset WHERE LOWER(name) = LOWER(:name)")
    Dataset getDatasetByName(@Bind("name") String name);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.dataset_id in (<datasetIds>) ")
    List<Dataset> findDatasetsByIdList(@BindList("datasetIds") List<Integer> datasetIds);

    @RegisterRowMapper(AssociationMapper.class)
    @SqlQuery(
        "SELECT * FROM consent_associations ca "
            + "INNER JOIN dataset ds ON ds.dataset_id = ca.dataset_id "
            + "WHERE ds.dataset_id IN (<dataSetIdList>)")
    List<Association> getAssociationsForDatasetIdList(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    /**
     * User -> UserRoles -> DACs -> Consents -> Consent Associations -> Datasets
     *
     * @param email User email
     * @return List of datasets that are visible to the user via DACs.
     */
    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " INNER JOIN user_role ur ON ur.dac_id = d.dac_id " +
            " INNER JOIN users u ON ur.user_id = u.user_id AND u.email = :email ")
    List<Dataset> findDatasetsByAuthUserEmail(@Bind("email") String email);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     *
     * @return List of datasets that are not owned by a DAC.
     */
    @SqlQuery(
        "SELECT d.* from dataset d " +
            " WHERE d.dac_id IS NULL ")
    List<Dataset> findNonDACDatasets();

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to a single DAC.
     */
    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, p.property_value, c.consent_id, d.dac_id, c.translated_use_restriction " +
            " FROM dataset d " +
            " LEFT OUTER JOIN dataset_property p ON p.dataset_id = d.dataset_id " +
            " LEFT OUTER JOIN dictionary k ON k.key_id = p.property_key " +
            " INNER JOIN consent_associations a ON a.dataset_id = d.dataset_id " +
            " INNER JOIN consents c ON c.consent_id = a.consent_id " +
            " WHERE d.dac_id = :dacId ")
    Set<DatasetDTO> findDatasetsByDac(@Bind("dacId") Integer dacId);

    /**
     * Finds all datasets which are assigned to this DAC and which
     * have been requested for this DAC.
     *
     * @param dacId id
     * @return all datasets associated with DAC
     */
    @UseRowReducer(DatasetReducer.class)
    @SqlQuery("SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " LEFT JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " LEFT JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE d.dac_id = :dacId " +
            " OR (dp.schema_property = 'dataAccessCommitteeId' AND dp.property_value = :dacId::text)")
    List<Dataset> findDatasetsAssociatedWithDac(@Bind("dacId") Integer dacId);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated with the provided DAC IDs
     */
    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, p.property_value, c.consent_id, d.dac_id, c.translated_use_restriction " +
            " FROM dataset d " +
            " LEFT OUTER JOIN dataset_property p ON p.dataset_id = d.dataset_id " +
            " LEFT OUTER JOIN dictionary k ON k.key_id = p.property_key " +
            " INNER JOIN consent_associations a ON a.dataset_id = d.dataset_id " +
            " INNER JOIN consents c ON c.consent_id = a.consent_id " +
            " WHERE d.dac_id IN (<dacIds>) ")
    Set<DatasetDTO> findDatasetsByDacIds(@BindList("dacIds") List<Integer> dacIds);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to any Dac.
     */
    @Deprecated
    @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
    @SqlQuery(
        "SELECT d.*, k.key, p.property_value, c.consent_id, d.dac_id, c.translated_use_restriction " +
            " FROM dataset d " +
            " LEFT OUTER JOIN dataset_property p ON p.dataset_id = d.dataset_id " +
            " LEFT OUTER JOIN dictionary k ON k.key_id = p.property_key " +
            " INNER JOIN consent_associations a ON a.dataset_id = d.dataset_id " +
            " INNER JOIN consents c ON c.consent_id = a.consent_id " +
            " WHERE d.dac_id IS NOT NULL ")
    Set<DatasetDTO> findDatasetsWithDacs();

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     *
     * @return List of dataset id and its associated dac id
     */
    @RegisterRowMapper(ImmutablePairOfIntsMapper.class)
    @SqlQuery(
        "SELECT DISTINCT d.dataset_id, d.dac_id FROM dataset d " +
            " WHERE d.dac_id IS NOT NULL ")
    List<Pair<Integer, Integer>> findDatasetAndDacIds();

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery(
        "SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, ca.consent_id, d.dac_id, c.translated_use_restriction, dar_ds_ids.id as in_use " +
            " FROM dataset d " +
            " LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id " +
            " LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id " +
            " LEFT JOIN dictionary k ON k.key_id = dp.property_key " +
            " INNER JOIN consent_associations ca ON ca.dataset_id = d.dataset_id " +
            " INNER JOIN consents c ON c.consent_id = ca.consent_id " +
            " WHERE c.consent_id = :consentId " +
            " AND d.active = true ")
    Set<Dataset> findDatasetsForConsentId(@Bind("consentId") String consentId);

    @SqlUpdate(
        "UPDATE dataset " +
        "SET dac_approval = :dacApproval, " +
            "update_date = :updateDate, " +
            "update_user_id = :updateUserId " +
        "WHERE dataset_id = :datasetId"
    )
    void updateDatasetApproval(
        @Bind("dacApproval") Boolean dacApproved,
        @Bind("updateDate") Instant updateDate,
        @Bind("updateUserId") Integer updateUserId,
        @Bind("datasetId") Integer datasetId
    );
    @SqlUpdate("DELETE FROM consent_associations WHERE dataset_id = :datasetId")
    void deleteConsentAssociationsByDatasetId(@Bind("datasetId") Integer datasetId);
}
