package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Association;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UseStringTemplate3StatementLocator
@RegisterMapper({DataSetMapper.class})
public interface DataSetDAO extends Transactional<DataSetDAO> {

    String CHAIRPERSON = "CHAIRPERSON";

    @SqlQuery("select * from dataset where dataSetId = :dataSetId")
    DataSet findDataSetById(@Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select * from dataset where objectId = :objectId")
    DataSet findDataSetByObjectId(@Bind("objectId") String objectId);

    @SqlQuery("select * from dataset where objectId in (<objectIdList>) and needs_approval = true")
    List<DataSet> findNeedsApprovalDataSetByObjectId(@BindIn("objectIdList") List<String> objectIdList);

    @SqlBatch("insert into dataset (name, createDate, objectId, active) values (:name, :createDate, :objectId, :active)")
    void insertAll(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("update dataset set name = :name where dataSetId = :dataSetId")
    void updateAll(@BindBean Collection<DataSet> dataSets);

    @SqlBatch("insert into datasetproperty (dataSetId, propertyKey, propertyValue, createDate )" +
            " values (:dataSetId, :propertyKey, :propertyValue, :createDate)")
    void insertDataSetsProperties(@BindBean List<DataSetProperty> dataSetPropertiesList);

    @SqlBatch("delete from datasetproperty where dataSetId = :dataSetId")
    void deleteDataSetsProperties(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlBatch("delete from dataset where dataSetId = :dataSetId")
    void deleteDataSets(@Bind("dataSetId") Collection<Integer> dataSetsIds);

    @SqlUpdate("update dataset set active = :active where dataSetId = :dataSetId")
    void updateDataSetActive(@Bind("dataSetId") Integer dataSetId, @Bind("active") Boolean active);

    @SqlUpdate("update dataset set needs_approval = :needs_approval where objectId = :objectId")
    void updateDataSetNeedsApproval(@Bind("objectId") String objectId, @Bind("needs_approval") Boolean needs_approval);

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey inner join consentassociations ca on ca.objectId = d.objectId inner join consents c on c.consentId = ca.consentId order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSets();

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey inner join consentassociations ca on ca.objectId = d.objectId inner join consents c on c.consentId = ca.consentId where d.objectId = :objectId order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSetWithPropertiesByOBjectId(@Bind("objectId") String objectId);

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery(" select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction from dataset  d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey inner join consentassociations ca on ca.objectId = d.objectId inner join consents c on c.consentId = ca.consentId inner join election e on e.referenceId = ca.consentId " +
            " inner join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON  + "' inner join (SELECT referenceId,MAX(createDate) maxDate FROM election where status ='Closed' group by referenceId) ev on ev.maxDate = e.createDate " +
            " and ev.referenceId = e.referenceId and v.vote = true and d.active = true order by d.dataSetId, k.displayOrder")
    Set<DataSetDTO> findDataSetsForResearcher();

    @Mapper(DataSetPropertiesMapper.class)
    @SqlQuery("select d.*, k.key, dp.propertyValue, ca.consentId , c.translatedUseRestriction from dataset d inner join datasetproperty dp on dp.dataSetId = d.dataSetId inner join dictionary k on k.keyId = dp.propertyKey inner join consentassociations ca on ca.objectId = d.objectId inner join consents c on c.consentId = ca.consentId where d.objectId in (<objectIdList>) order by d.dataSetId, k.receiveOrder")
    Set<DataSetDTO> findDataSetsByReceiveOrder(@BindIn("objectIdList") List<String> objectIdList);

    @Mapper(BatchMapper.class)
    @SqlQuery("select datasetId, objectId  from dataset where objectId in (<objectIdList>)")
    List<Map<String,Integer>> searchByObjectIdList(@BindIn("objectIdList") List<String> objectIdList);


    @SqlQuery("select *  from dataset where objectId in (<objectIdList>) ")
    List<DataSet> searchDataSetsByObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery("select ds.dataSetId  from dataset ds where ds.objectId in (<objectIdList>) ")
    List<Integer> searchDataSetsIdsByObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @RegisterMapper({DictionaryMapper.class})
    @SqlQuery("SELECT * FROM dictionary d order by receiveOrder")
    List<Dictionary> getMappedFieldsOrderByReceiveOrder();

    @RegisterMapper({DictionaryMapper.class})
    @SqlQuery("SELECT * FROM dictionary d order by displayOrder")
    List<Dictionary> getMappedFieldsOrderByDisplayOrder();

    @SqlQuery("SELECT COUNT(*) FROM consentassociations ca WHERE ca.objectId IN (<objectIdList>)")
    Integer consentAssociationCount(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery(" SELECT * FROM dataset d WHERE d.objectId IN (<objectIdList>)")
    List<DataSet> getDataSetsForObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @SqlQuery("SELECT * FROM dataset d WHERE d.objectId IN (Select objectId FROM consentassociations Where consentId = :consentId)")
    List<DataSet> getDataSetsForConsent(@Bind("consentId") String consentId);

    @SqlQuery("SELECT * FROM dataset d WHERE d.objectId = :objectId ")
    DataSet getDataSetsByObjectId(@Bind("objectId") String objectId);

    @RegisterMapper({AssociationMapper.class})
    @SqlQuery("SELECT * FROM consentassociations ca WHERE ca.objectId IN (<objectIdList>)")
    List<Association> getAssociationsForObjectIdList(@BindIn("objectIdList") List<String> objectIdList);

    @RegisterMapper({AutocompleteMapper.class})
    @SqlQuery("SELECT DISTINCT d.objectId as id, CONCAT_WS(' | ', d.objectId, d.name, dsp.propertyValue) as concatenation FROM dataset d inner join consentassociations ca on ca.objectId = d.objectId and d.active = true" +
            " inner join consents c on c.consentId = ca.consentId inner join election e on e.referenceId = ca.consentId " +
            " inner join datasetproperty dsp on dsp.dataSetId = d.dataSetId and dsp.propertyKey IN (9) " +
            " inner join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON  +
            "' inner join (SELECT referenceId,MAX(createDate) maxDate FROM" +
            " election where status ='Closed' group by referenceId) ev on ev.maxDate = e.createDate and ev.referenceId = e.referenceId " +
            " and v.vote = true  and d.objectId like concat('%',:partial,'%') or d.name like concat('%',:partial,'%') or dsp.propertyValue like concat('%',:partial,'%')" +
            " order by d.dataSetId")
    List< Map<String, String>> getObjectIdsbyPartial(@Bind("partial") String partial);

}