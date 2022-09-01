package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.ConsentManageMapper;
import org.broadinstitute.consent.http.db.mapper.ConsentMapper;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(ConsentMapper.class)
public interface ConsentDAO extends Transactional<ConsentDAO> {

    @SqlQuery("select * from consents where consentId = :consentId and active=true")
    Consent findConsentById(@Bind("consentId") String consentId);

    @SqlQuery(
        " SELECT c.* "
            + " FROM consents c "
            + " INNER JOIN consentassociations cs ON c.consentid = cs.consentid "
            + " WHERE cs.datasetid = :datasetId")
    Consent findConsentFromDatasetID(@Bind("datasetId") Integer datasetId);

    @SqlQuery("SELECT c.name " +
            "FROM consents c INNER JOIN consentassociations cs ON c.consentId = cs.consentId "+
            "WHERE cs.dataSetId = :datasetId")
    String findConsentNameFromDatasetID(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select * from consents  where consentId in (<consentIds>)")
    Collection<Consent> findConsentsFromConsentsIDs(@BindList("consentIds") List<String> consentIds);

    @SqlQuery("select * from consents  where name in (<names>)")
    List<Consent> findConsentsFromConsentNames(@BindList("names") List<String> names);

    @SqlQuery("select consentId from consents where consentId = :consentId and active=true")
    String checkConsentById(@Bind("consentId") String consentId);

    @SqlQuery("select consentId from consents where name = :name")
    String getIdByName(@Bind("name") String name);

    @SqlQuery("select * from consents where name = :name and active=true")
    Consent findConsentByName(@Bind("name") String name);

    @SqlUpdate("insert into consents " +
            "(consentId, requiresManualReview, useRestriction, dataUse, dataUseLetter, active, name, dulName, createDate, sortDate, translatedUseRestriction, groupName, dac_id) values " +
            "(:consentId, :requiresManualReview, :useRestriction, :dataUse, :dataUseLetter, true, :name , :dulName, :createDate, :sortDate , :translatedUseRestriction, :groupName, :dacId)")
    void insertConsent(@Bind("consentId") String consentId,
                       @Bind("requiresManualReview") Boolean requiresManualReview,
                       @Bind("useRestriction") String useRestriction,
                       @Bind("dataUse") String dataUse,
                       @Bind("dataUseLetter") String dataUseLetter,
                       @Bind("name") String name,
                       @Bind("dulName") String dulName,
                       @Bind("createDate") Date createDate,
                       @Bind("sortDate") Date sortDate,
                       @Bind("translatedUseRestriction") String translatedUseRestriction,
                       @Bind("groupName") String groupName,
                       @Bind("dacId") Integer dacId);

    @SqlUpdate("delete from consents where consentId = :consentId")
    void deleteConsent(@Bind("consentId") String consentId);

    @SqlUpdate(" update consents set " +
            " requiresManualReview = :requiresManualReview, " +
            " useRestriction = :useRestriction, " +
            " dataUse = :dataUse, " +
            " dataUseLetter = :dataUseLetter, " +
            " name = :name, " +
            " dulName = :dulName, " +
            " lastUpdate = :lastUpdate, " +
            " sortDate = :sortDate, " +
            " translatedUseRestriction = :translatedUseRestriction, " +
            " groupName = :groupName, " +
            " updated = :updated, " +
            " dac_id = :dacId " +
            " where consentId = :consentId " +
            " and active = true ")
    void updateConsent(@Bind("consentId") String consentId,
                       @Bind("requiresManualReview") Boolean requiresManualReview,
                       @Bind("useRestriction") String useRestriction,
                       @Bind("dataUse") String dataUse,
                       @Bind("dataUseLetter") String dataUseLetter,
                       @Bind("name") String name,
                       @Bind("dulName") String dulName,
                       @Bind("lastUpdate") Date createDate,
                       @Bind("sortDate") Date sortDate,
                       @Bind("translatedUseRestriction") String translatedUseRestriction,
                       @Bind("groupName") String groupName,
                       @Bind("updated") Boolean updateStatus,
                       @Bind("dacId") Integer dacId);

    @SqlUpdate(" UPDATE consents " +
            " SET translateduserestriction = :translatedUseRestriction " +
            " WHERE consentid = :consentId ")
    void updateConsentTranslatedUseRestriction(
            @Bind("consentId") String consentId,
            @Bind("translatedUseRestriction") String translatedUseRestriction);

    @SqlUpdate("update consents set sortDate = :sortDate " +
            "where consentId = :consentId and active = true")
    void updateConsentSortDate(@Bind("consentId") String consentId, @Bind("sortDate") Date sortDate);

    // Consent Association Access Methods

    @SqlUpdate("insert into consentassociations (consentId, associationType, dataSetId) values (:consentId, :associationType, :dataSetId)")
    void insertConsentAssociation(@Bind("consentId") String consentId,
                       @Bind("associationType") String associationType,
                       @Bind("dataSetId") Integer dataSetId);


    @SqlQuery("select ds.object_id from consentassociations ca inner join dataset ds on ds.dataset_id = ca.dataSetId where ca.consentId = :consentId and ca.associationType = :associationType and ds.object_id is not null")
    List<String> findAssociationsByType(@Bind("consentId") String consentId,
                                        @Bind("associationType") String associationType);

    @SqlQuery("select associationId from consentassociations where dataSetId = :datasetId")
    Integer findAssociationsByDataSetId(@Bind("datasetId") Integer datasetId);

    @SqlQuery("select  ds.object_id from consentassociations ca inner join dataset ds on ds.dataset_id = ca.dataSetId where ca.consentId = :consentId and ca.associationType = :associationType and ds.object_id = :objectId")
    String findAssociationByTypeAndId(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType,
                                      @Bind("objectId") String objectId);

    @SqlUpdate("delete from consentassociations where consentId = :consentId and associationType = :associationType and dataSetId = :dataSetId")
    void deleteOneAssociation(@Bind("consentId") String consentId,
                              @Bind("associationType") String associationType,
                              @Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("delete from consentassociations where consentId = :consentId and associationType = :associationType")
    void deleteAllAssociationsForType(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType);

    @SqlUpdate("delete from consentassociations where consentId = :consentId")
    void deleteAllAssociationsForConsent(@Bind("consentId") String consentId);

    @SqlQuery("select distinct(associationType) from consentassociations where consentId = :consentId")
    List<String> findAssociationTypesForConsent(@Bind("consentId") String consentId);

    @SqlQuery("SELECT * FROM consents WHERE consentId NOT IN (SELECT c.consentId FROM consents c INNER JOIN election e on e.reference_id = c.consentId )")
    List<Consent> findUnreviewedConsents();

    @SqlQuery("select requiresManualReview from consents where consentId = :consentId")
    Boolean checkManualReview(@Bind("consentId") String consentId);

    @SqlQuery(
        "SELECT c.consentId, c.dac_id, c.name, c.createDate, c.sortDate, c.groupName, c.updated, e.election_id, e.status, e.version, e.archived  " +
            "FROM consents c " +
            "INNER JOIN election e ON e.reference_id = c.consentId " +
            "INNER JOIN ( "+
                "SELECT reference_id, MAX(create_date) max_date " +
                "FROM election e " +
                "GROUP BY reference_id) election_view "+
                    "ON election_view.max_date = e.create_date " +
                    "AND election_view.reference_id = e.reference_id " +
                    "AND e.status = :status")
    @UseRowMapper(ConsentManageMapper.class)
    List<ConsentManage> findConsentManageByStatus(@Bind("status") String status);

    @SqlQuery("select ca.consentId from consentassociations ca  where ca.dataSetId IN (<dataSetIdList>) ")
    List<String> getAssociationConsentIdsFromDatasetIds(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    @SqlUpdate("update consents set updated = :updated where consentId = :referenceId")
    void updateConsentUpdateStatus(@Bind("referenceId") String referenceId,
                                   @Bind("updated") Boolean updated);

    @SqlUpdate("update consents set dac_id = :dacId where consentId = :consentId")
    void updateConsentDac(@Bind("consentId") String consentId,
                                @Bind("dacId") Integer dacId);

}
