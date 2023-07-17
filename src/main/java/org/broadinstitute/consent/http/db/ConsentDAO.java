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

  @SqlQuery("SELECT * FROM consents WHERE consent_id IN (<consentIds>)")
  Collection<Consent> findConsentsFromConsentsIDs(@BindList("consentIds") List<String> consentIds);

  @SqlQuery("SELECT consent_id FROM consents WHERE consent_id = :consentId AND active=true")
  String checkConsentById(@Bind("consentId") String consentId);

  @SqlQuery("SELECT consent_id FROM consents WHERE name = :name")
  String getIdByName(@Bind("name") String name);

  @SqlQuery("SELECT * FROM consents WHERE name = :name AND active=true")
  Consent findConsentByName(@Bind("name") String name);


  @SqlUpdate("""
      INSERT INTO consents
          (consent_id, requires_manual_review, data_use, data_use_letter, active, name, dul_name,
          create_date, sort_date, group_name)
      VALUES (:consentId, :requiresManualReview, :dataUse, :dataUseLetter, true, :name,
          :dulName, :createDate, :sortDate, :groupName)
      """)
  void insertConsent(@Bind("consentId") String consentId,
      @Bind("requiresManualReview") Boolean requiresManualReview,
      @Bind("dataUse") String dataUse,
      @Bind("dataUseLetter") String dataUseLetter,
      @Bind("name") String name,
      @Bind("dulName") String dulName,
      @Bind("createDate") Date createDate,
      @Bind("sortDate") Date sortDate,
      @Bind("groupName") String groupName);

  @SqlUpdate("DELETE FROM consents WHERE consent_id = :consentId")
  void deleteConsent(@Bind("consentId") String consentId);

  @SqlUpdate("""
      UPDATE consents
      SET requires_manual_review = :requiresManualReview,
          data_use = :dataUse,
          data_use_letter = :dataUseLetter,
          name = :name,
          dul_name = :dulName,
          last_update = :lastUpdate,
          sort_date = :sortDate,
          group_name = :groupName,
          updated = :updated
      WHERE consent_id = :consentId
      AND active = true
      """)
  void updateConsent(@Bind("consentId") String consentId,
      @Bind("requiresManualReview") Boolean requiresManualReview,
      @Bind("dataUse") String dataUse,
      @Bind("dataUseLetter") String dataUseLetter,
      @Bind("name") String name,
      @Bind("dulName") String dulName,
      @Bind("lastUpdate") Date createDate,
      @Bind("sortDate") Date sortDate,
      @Bind("groupName") String groupName,
      @Bind("updated") Boolean updateStatus);

  @SqlUpdate("""
      UPDATE consents
      SET sort_date = :sortDate
      WHERE consent_id = :consentId
      AND active = true
      """)
  void updateConsentSortDate(@Bind("consentId") String consentId, @Bind("sortDate") Date sortDate);

  // Consent Association Access Methods

  @SqlUpdate("INSERT INTO consent_associations (consent_id, association_type, dataset_id) VALUES (:consentId, :associationType, :dataSetId)")
  void insertConsentAssociation(@Bind("consentId") String consentId,
      @Bind("associationType") String associationType,
      @Bind("dataSetId") Integer dataSetId);

  @SqlQuery("SELECT association_id FROM consent_associations WHERE dataset_id = :datasetId")
  Integer findAssociationsByDataSetId(@Bind("datasetId") Integer datasetId);

  @SqlUpdate("DELETE FROM consent_associations WHERE consent_id = :consentId")
  void deleteAllAssociationsForConsent(@Bind("consentId") String consentId);

  @SqlQuery("SELECT requires_manual_review FROM consents WHERE consent_id = :consentId")
  Boolean checkManualReview(@Bind("consentId") String consentId);

}
