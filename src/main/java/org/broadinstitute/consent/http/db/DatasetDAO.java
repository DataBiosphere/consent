package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
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

    @Deprecated
    @SqlUpdate("insert into dataset (name, createDate, objectId, active, alias) values (:name, :createDate, :objectId, :active, :alias)")
    @GetGeneratedKeys
    Integer insertDataset(@Bind("name") String name, @Bind("createDate") Date createDate, @Bind("objectId") String objectId, @Bind("active") Boolean active, @Bind("alias") Integer alias);

    @SqlUpdate("INSERT INTO dataset (name, createdate, create_user_id, update_date, update_user_id, objectId, active, alias) (SELECT :name, :createDate, :createUserId, :createDate, :createUserId, :objectId, :active, MAX(alias)+1 FROM dataset)")
    @GetGeneratedKeys
    Integer insertDatasetV2(@Bind("name") String name, @Bind("createDate") Timestamp createDate, @Bind("createUserId") Integer createUserId, @Bind("objectId") String objectId, @Bind("active") Boolean active);

    @SqlQuery("select * from dataset where datasetid = :datasetId")
    Dataset findDatasetById(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select * from dataset where objectId = :objectId")
    Integer findDatasetIdByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("SELECT * FROM dataset WHERE datasetid IN (<datasetIdList>) AND needs_approval = true")
    List<Dataset> findNeedsApprovalDatasetByDatasetId(@BindList("datasetIdList") List<Integer> datasetIdList);

    @Deprecated
    @SqlBatch("insert into dataset (name, createDate, objectId, active, alias) values (:name, :createDate, :objectId, :active, :alias)")
    void insertAll(@BindBean Collection<Dataset> datasets);

    @SqlBatch("insert into datasetproperty (datasetid, propertykey, propertyvalue, createdate )" +
            " values (:datasetId, :propertyKey, :propertyValue, :createDate)")
    void insertDatasetProperties(@BindBean List<DatasetProperty> datasetPropertiesList);

    @SqlBatch("delete from datasetproperty where datasetid = :datasetId")
    void deleteDatasetPropertiesByDatasetIdList(@Bind("datasetId") Collection<Integer> datasetsIds);

    @SqlUpdate("DELETE FROM datasetproperty WHERE datasetid = :datasetId")
    void deleteDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("INSERT INTO dataset_audit (datasetid, changeaction, modifiedbyuser, modificationdate, objectid, name, active) VALUES (:datasetId, :action, :user, :date, :objectId, :name, :active )")
    @GetGeneratedKeys
    Integer insertDatasetAudit(@BindBean DatasetAudit datasets);

    @SqlUpdate("DELETE FROM dataset_user_association WHERE datasetid = :datasetId")
    void deleteUserAssociationsByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("DELETE FROM consentassociations WHERE datasetid = :datasetId")
    void deleteConsentAssociationsByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("UPDATE datasetproperty SET propertyvalue = :propertyValue WHERE datasetid = :datasetId AND propertykey = :propertyKey")
    void updateDatasetProperty(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey, @Bind("propertyValue") String propertyValue);

    @SqlUpdate("DELETE from datasetproperty WHERE datasetid = :datasetId AND propertykey = :propertyKey")
    void deleteDatasetPropertyByKey(@Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey);

    @SqlBatch("delete from dataset where datasetid = :datasetId")
    void deleteDatasets(@Bind("datasetId") Collection<Integer> datasetsIds);

    @SqlUpdate("DELETE FROM dataset WHERE datasetid = :datasetId")
    void deleteDatasetById(@Bind("datasetId") Integer datasetId);

    @SqlUpdate("update dataset set active = :active where datasetid = :datasetId")
    void updateDatasetActive(@Bind("datasetId") Integer datasetId, @Bind("active") Boolean active);

    @SqlUpdate("update dataset set needs_approval = :needs_approval where datasetId = :datasetId")
    void updateDatasetNeedsApproval(@Bind("datasetId") Integer datasetId, @Bind("needs_approval") Boolean needs_approval);

    @SqlUpdate("UPDATE dataset SET update_date = :updateDate, update_user_id = :updateUserId WHERE datasetid = :datasetId")
    void updateDatasetUpdateUserAndDate(@Bind("datasetId") Integer datasetId, @Bind("updateDate") Timestamp updateDate, @Bind("updateUserId") Integer updateUserId);

    @UseRowReducer(DatasetReducer.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause " +
          "FROM dataset d " +
          "LEFT OUTER JOIN datasetproperty dp ON dp.datasetid = d.datasetid " +
          "LEFT OUTER JOIN dictionary k ON k.keyid = dp.propertykey " +
          "LEFT OUTER JOIN consentassociations ca ON ca.datasetid = d.datasetid " +
          "LEFT OUTER JOIN consents c ON c.consentid = ca.consentid " +
          "WHERE d.datasetid IN (<datasetIds>)" +
          "ORDER BY d.datasetid, k.displayorder")
    Set<Dataset> findDatasetWithDataUseByIdList(@BindList("datasetIds") List<Integer> datasetIds);

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

    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("SELECT d.*, k.key, dp.propertyvalue, ca.consentid, c.dac_id, c.translateduserestriction, c.datause " +
          "FROM dataset d " +
          "LEFT OUTER JOIN datasetproperty dp on dp.datasetid = d.datasetid " +
          "LEFT OUTER JOIN dictionary k on k.keyid = dp.propertykey " +
          "LEFT OUTER JOIN consentassociations ca on ca.datasetid = d.datasetid " +
          "LEFT OUTER JOIN consents c on c.consentid = ca.consentid " +
          "WHERE d.datasetid = :datasetId ORDER BY d.datasetid, k.displayorder")
    Set<DatasetDTO> findDatasetDTOWithPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @UseRowMapper(DatasetPropertyMapper.class)
    @SqlQuery(
        "SELECT * FROM datasetproperty WHERE datasetid = :datasetId"
    )
    Set<DatasetProperty> findDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId, c.dac_id, c.translatedUseRestriction, c.datause " +
            "from dataset d inner join datasetproperty dp on dp.datasetId = d.datasetId inner join dictionary k on k.keyId = dp.propertyKey " +
            "inner join consentassociations ca on ca.datasetId = d.datasetId inner join consents c on c.consentId = ca.consentId " +
            "where d.datasetId in (<datasetIdList>) order by d.datasetId, k.receiveOrder")
    Set<DatasetDTO> findDatasetsByReceiveOrder(@BindList("datasetIdList") List<Integer> datasetIdList);

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d order by receiveOrder")
    List<Dictionary> getMappedFieldsOrderByReceiveOrder();

    @RegisterRowMapper(DictionaryMapper.class)
    @SqlQuery("SELECT * FROM dictionary d WHERE d.displayOrder is not null  order by displayOrder")
    List<Dictionary> getMappedFieldsOrderByDisplayOrder();

    @SqlQuery("SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>)")
    List<Dataset> getDatasetsForObjectIdList(@BindList("objectIdList") List<String> objectIdList);

    @SqlQuery("SELECT ds.* FROM consentassociations ca inner join dataset ds on ds.datasetId = ca.datasetId WHERE ca.consentId = :consentId")
    List<Dataset> getDatasetsForConsent(@Bind("consentId") String consentId);

    @SqlQuery("SELECT ca.consentId FROM consentassociations ca INNER JOIN dataset ds on ds.datasetId = ca.datasetId WHERE ds.datasetId = :datasetId")
    String getAssociatedConsentIdByDatasetId(@Bind("datasetId") Integer datasetId);

    @SqlQuery("SELECT * FROM dataset WHERE LOWER(name) = LOWER(:name)")
    Dataset getDatasetByName(@Bind("name") String name);

    @SqlQuery("SELECT * FROM dataset WHERE datasetid in (<datasetIds>) ")
    List<Dataset> findDatasetsByIdList(@BindList("datasetIds") List<Integer> datasetIds);

    @RegisterRowMapper(AssociationMapper.class)
    @SqlQuery("SELECT * FROM consentassociations ca inner join dataset ds on ds.datasetId = ca.datasetId WHERE ds.datasetId IN (<datasetIdList>)")
    List<Association> getAssociationsForDatasetIdList(@BindList("datasetIdList") List<Integer> datasetIdList);

    /**
     * User -> UserRoles -> DACs -> Consents -> Consent Associations -> Datasets
     *
     * @param email User email
     * @return List of datasets that are visible to the user via DACs.
     */
    @SqlQuery(" SELECT d.* " +
            " FROM dataset d " +
            " LEFT OUTER JOIN consentassociations a ON d.datasetId = a.datasetId " +
            " LEFT OUTER JOIN consents c ON a.consentId = c.consentId " +
            " INNER JOIN user_role ur ON ur.dac_id = c.dac_id " +
            " INNER JOIN dacuser u ON ur.user_id = u.dacUserId and u.email = :email ")
    List<Dataset> findDatasetsByAuthUserEmail(@Bind("email") String email);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     *
     * @return List of datasets that are not owned by a DAC.
     */
    @SqlQuery(" select d.* from dataset d " +
            " inner join consentassociations a on d.datasetid = a.datasetid " +
            " inner join consents c on a.consentid = c.consentid " +
            " where c.dac_id is null ")
    List<Dataset> findNonDACDatasets();

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to a single DAC.
     */
    @UseRowMapper(DatasetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, p.propertyvalue, c.consentid, c.dac_id, c.translateduserestriction, c.datause from dataset d " +
            " left outer join datasetproperty p on p.datasetid = d.datasetid " +
            " left outer join dictionary k on k.keyid = p.propertykey " +
            " inner join consentassociations a on a.datasetid = d.datasetid " +
            " inner join consents c on c.consentid = a.consentid " +
            " where c.dac_id = :dacId ")
    Set<DatasetDTO> findDatasetsByDac(@Bind("dacId") Integer dacId);

    /**
     * DACs -> Consents -> Consent Associations -> Datasets
     * Datasets -> DatasetProperties -> Dictionary
     *
     * @return Set of datasets, with properties, that are associated to any Dac.
     */
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
    @SqlQuery("select distinct d.datasetid, c.dac_id from dataset d " +
            " inner join consentassociations a on d.datasetid = a.datasetid " +
            " inner join consents c on a.consentid = c.consentid " +
            " where c.dac_id is not null ")
    List<Pair<Integer, Integer>> findDatasetAndDacIds();

    @UseRowMapper(DatasetMapper.class)
    @SqlQuery("select d.* from dataset d " +
            " inner join consentassociations a on a.datasetid = d.datasetid and a.consentid = :consentId " +
            " where d.active = true ")
    Set<Dataset> findDatasetsForConsentId(@Bind("consentId") String consentId);

}
