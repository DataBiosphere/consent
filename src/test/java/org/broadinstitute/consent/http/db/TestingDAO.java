package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@SuppressWarnings("SqlWithoutWhere")
public interface TestingDAO extends Transactional<TestingDAO> {

  @SqlUpdate("DELETE FROM vote")
  void deleteAllVotes();

  @SqlUpdate("DELETE FROM approval_expiration_time")
  void deleteAllApprovalTimes();

  @SqlUpdate("DELETE FROM consent_audit")
  void deleteAllConsentAudits();

  @SqlUpdate("DELETE FROM match_rationale")
  void deleteAllMatchEntityRationales();

  @SqlUpdate("DELETE FROM match_entity")
  void deleteAllMatchEntities();

  @SqlUpdate("DELETE FROM consent_associations")
  void deleteAllConsentAssociations();

  @SqlUpdate("DELETE FROM consents")
  void deleteAllConsents();

  @SqlUpdate("DELETE FROM access_rp")
  void deleteAllAccessRps();

  @SqlUpdate("DELETE FROM election")
  void deleteAllElections();

  @SqlUpdate("DELETE FROM dataset_property")
  void deleteAllDatasetProperties();

  @SqlUpdate("DELETE FROM dataset_audit")
  void deleteAllDatasetAudits();

  /**
   * This only deletes new keys created through tests Keys 1-11 are existing keys required for many
   * legacy tests.
   */
  @SqlUpdate("DELETE FROM dictionary WHERE key_id > 11")
  void deleteAllDictionaryTerms();

  @SqlUpdate("DELETE FROM dataset")
  void deleteAllDatasets();

  @SqlUpdate("DELETE FROM study")
  void deleteAllStudies();

  @SqlUpdate("DELETE FROM study_property")
  void deleteAllStudyProperties();


  @SqlUpdate("DELETE FROM user_role where dac_id is not null")
  void deleteAllDacUserRoles();

  @SqlUpdate("DELETE FROM dac")
  void deleteAllDacs();

  @SqlUpdate("DELETE FROM library_card")
  void deleteAllLibraryCards();

  @SqlUpdate("DELETE FROM institution")
  void deleteAllInstitutions();

  @SqlUpdate("DELETE FROM user_property")
  void deleteAllUserProperties();

  @SqlUpdate("DELETE FROM user_role")
  void deleteAllUserRoles();

  @SqlUpdate("DELETE FROM users")
  void deleteAllUsers();

  @SqlUpdate("DELETE FROM data_access_request")
  void deleteAllDARs();

  @SqlUpdate("DELETE FROM dar_collection")
  void deleteAllDARCollections();

  @SqlUpdate("DELETE FROM dar_dataset")
  void deleteAllDARDataset();

  @SqlUpdate("DELETE FROM counter")
  void deleteAllCounters();

  @SqlUpdate("DELETE FROM email_entity")
  void deleteAllEmailEntities();

  @SqlUpdate("DELETE FROM file_storage_object")
  void deleteAllFileStorageObjects();

  @SqlUpdate("DELETE FROM acknowledgement")
  void deleteAllAcknowledgements();

  @SqlUpdate("DELETE FROM data_access_agreement")
  void deleteAllDataAccessAgreements();

  @SqlUpdate("DELETE FROM dac_daa")
  void deleteAllDACDAAs();

  @SqlUpdate("DELETE FROM lc_daa")
  void deleteAllLibraryCardDAAs();
}
