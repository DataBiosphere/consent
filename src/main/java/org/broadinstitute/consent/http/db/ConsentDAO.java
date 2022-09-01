package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.ConsentMapper;
import org.broadinstitute.consent.http.models.Consent;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(ConsentMapper.class)
public interface ConsentDAO extends Transactional<ConsentDAO> {

    /**
     * Find consent by id
     *
     * @return Consent
     */
    @SqlQuery("SELECT * FROM consents WHERE consent_id = :consentId AND active=true")
    Consent findConsentById(@Bind("consentId") String consentId);

    @SqlQuery(
            " SELECT c.* "
                    + " FROM consents c "
                    + " INNER JOIN consent_associations ca ON c.consent_id = ca.consent_id "
                    + " WHERE ca.dataset_id = :datasetId")
    Consent findConsentFromDatasetID(@Bind("datasetId") Integer datasetId);

    @SqlQuery("SELECT * FROM consents WHERE consent_id IN (<consentIds>)")
    Collection<Consent> findConsentsFromConsentsIDs(@BindList("consentIds") List<String> consentIds);

    @SqlQuery("SELECT consent_id FROM consents WHERE consent_id = :consentId AND active=true")
    String checkConsentById(@Bind("consentId") String consentId);

    @SqlQuery("SELECT consent_id FROM consents WHERE name = :name")
    String getIdByName(@Bind("name") String name);

    @SqlQuery("SELECT * FROM consents WHERE name = :name AND active=true")
    Consent findConsentByName(@Bind("name") String name);


    @SqlUpdate("INSERT INTO consents " +
            "(consent_id, requires_manual_review, use_restriction, data_use, data_use_letter, active, name, dul_name," +
            " create_date, sort_date, translated_use_restriction, group_name)" +
            " VALUES (:consentId, :requiresManualReview, :useRestriction, :dataUse, :dataUseLetter, true, :name," +
            " :dulName, :createDate, :sortDate , :translatedUseRestriction, :groupName)")
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
                       @Bind("groupName") String groupName);

    @SqlUpdate("DELETE FROM consents WHERE consent_id = :consentId")
    void deleteConsent(@Bind("consentId") String consentId);

    @SqlUpdate("UPDATE consents SET " +
            " requires_manual_review = :requiresManualReview, " +
            " use_restriction = :useRestriction, " +
            " data_use = :dataUse, " +
            " data_use_letter = :dataUseLetter, " +
            " name = :name, " +
            " dul_name = :dulName, " +
            " last_update = :lastUpdate, " +
            " sort_date = :sortDate, " +
            " translated_use_restriction = :translatedUseRestriction, " +
            " group_name = :groupName, " +
            " updated = :updated, " +
            " WHERE consent_id = :consentId " +
            " AND active = true ")
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
                       @Bind("updated") Boolean updateStatus);

    @SqlUpdate("UPDATE consents " +
            " SET translated_use_restriction = :translatedUseRestriction " +
            " WHERE consent_id = :consentId ")
    void updateConsentTranslatedUseRestriction(
            @Bind("consentId") String consentId,
            @Bind("translatedUseRestriction") String translatedUseRestriction);

    @SqlUpdate("UPDATE consents SET sort_date = :sortDate " +
            "WHERE consent_id = :consentId AND active = true")
    void updateConsentSortDate(@Bind("consentId") String consentId, @Bind("sortDate") Date sortDate);

    // Consent Association Access Methods

    @SqlUpdate("INSERT INTO consent_associations (consent_id, association_type, dataset_id) VALUES (:consentId, :associationType, :dataSetId)")
    void insertConsentAssociation(@Bind("consentId") String consentId,
                                  @Bind("associationType") String associationType,
                                  @Bind("dataSetId") Integer dataSetId);


    @SqlQuery("SELECT ds.object_id FROM consent_associations ca INNER JOIN dataset ds ON ds.dataset_id = ca.dataset_id " +
            " WHERE ca.consent_id = :consentId AND ca.association_type = :associationType AND ds.object_id IS NOT NULL")
    List<String> findAssociationsByType(@Bind("consentId") String consentId,
                                        @Bind("associationType") String associationType);

    @SqlQuery("SELECT association_id FROM consent_associations WHERE dataset_id = :datasetId")
    Integer findAssociationsByDataSetId(@Bind("datasetId") Integer datasetId);

    @SqlQuery("SELECT ds.object_id FROM consent_associations ca INNER JOIN dataset ds ON ds.dataset_id = ca.dataset_id " +
            " WHERE ca.consent_id = :consentId AND ca.association_type = :associationType AND ds.object_id = :objectId")
    String findAssociationByTypeAndId(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType,
                                      @Bind("objectId") String objectId);

    @SqlUpdate("DELETE FROM consent_associations WHERE consent_id = :consentId AND association_type = :associationType AND dataset_id = :dataSetId")
    void deleteOneAssociation(@Bind("consentId") String consentId,
                              @Bind("associationType") String associationType,
                              @Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("DELETE FROM consent_associations WHERE consent_id = :consentId AND association_type = :associationType")
    void deleteAllAssociationsForType(@Bind("consentId") String consentId,
                                      @Bind("associationType") String associationType);

    @SqlUpdate("DELETE FROM consent_associations WHERE consent_id = :consentId")
    void deleteAllAssociationsForConsent(@Bind("consentId") String consentId);

    @SqlQuery("SELECT DISTINCT (association_type) FROM consent_associations WHERE consent_id = :consentId")
    List<String> findAssociationTypesForConsent(@Bind("consentId") String consentId);


    @SqlQuery("SELECT requires_manual_review FROM consents WHERE consent_id = :consentId")
    Boolean checkManualReview(@Bind("consentId") String consentId);

    @SqlQuery("SELECT ca.consent_id FROM consent_associations ca WHERE ca.dataset_id IN (<dataSetIdList>)")
    List<String> getAssociationConsentIdsFromDatasetIds(@BindList("dataSetIdList") List<Integer> dataSetIdList);

    @SqlUpdate("UPDATE consents SET updated = :updated WHERE consent_id = :referenceId")
    void updateConsentUpdateStatus(@Bind("referenceId") String referenceId,
                                   @Bind("updated") Boolean updated);
}
