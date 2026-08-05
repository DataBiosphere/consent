package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DatasetIdentityMigrationTest extends DAOTestHelper {

  private static final String DATASET_IDENTITY_CHANGELOG =
      "changesets/changelog-consent-2026-08-05-dataset-identity.xml";

  @Test
  void testRollbackRestoresLegacyIdentityAndMigrationReapplies() throws Exception {
    User user = createUser();
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer datasetId =
        datasetDAO.insertDataset(
            "Rollback Dataset " + UUID.randomUUID(),
            FIXED_TIMESTAMP,
            user.getUserId(),
            "Rollback Object " + UUID.randomUUID(),
            dataUse.toString(),
            null);
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE dataset SET alias = 1000000 WHERE dataset_id = :datasetId")
                .bind("datasetId", datasetId)
                .execute());
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    String purposeId = UUID.randomUUID().toString();
    matchDAO.insertMatch(
        datasetId, purposeId, true, false, FIXED_DATE, MatchAlgorithm.V4.getVersion(), false);

    jdbi.useHandle(
        handle -> {
          Connection connection = handle.getConnection();
          Database database =
              DatabaseFactory.getInstance()
                  .findCorrectDatabaseImplementation(new JdbcConnection(connection));
          Liquibase liquibase =
              new Liquibase(
                  DATASET_IDENTITY_CHANGELOG, new ClassLoaderResourceAccessor(), database);
          Contexts contexts = new Contexts();
          LabelExpression labels = new LabelExpression();

          try {
            // DAOTestHelper clears data between tests, including Liquibase's tracking rows. Restore
            // the tracking metadata without rerunning the already-present schema changes.
            liquibase.changeLogSync(contexts, labels);
            liquibase.rollback(5, contexts, labels);

            assertTrue(columnExists(handle, "match_entity", "consent"));
            assertFalse(columnExists(handle, "match_entity", "dataset_id"));
            assertFalse(sequenceExists(handle, "dataset_alias_seq"));
            assertEquals(
                dataset.getDatasetIdentifier(),
                handle
                    .createQuery("SELECT consent FROM match_entity WHERE purpose = :purpose")
                    .bind("purpose", purposeId)
                    .mapTo(String.class)
                    .one());
            assertEquals(
                0L,
                handle
                    .createQuery(
                        """
                        INSERT INTO dataset
                            (name, create_date, create_user_id, update_date,
                             update_user_id, object_id, data_use)
                        VALUES (:name, :createDate, :userId, :createDate,
                                :userId, :objectId, :dataUse)
                        RETURNING alias
                        """)
                    .bind("name", "Legacy Default Dataset " + UUID.randomUUID())
                    .bind("createDate", FIXED_TIMESTAMP)
                    .bind("userId", user.getUserId())
                    .bind("objectId", "Legacy Default Object " + UUID.randomUUID())
                    .bind("dataUse", dataUse.toString())
                    .mapTo(Long.class)
                    .one());
          } finally {
            liquibase.update(contexts, labels);
          }

          assertTrue(columnExists(handle, "match_entity", "consent"));
          assertTrue(columnExists(handle, "match_entity", "dataset_id"));
          assertTrue(sequenceExists(handle, "dataset_alias_seq"));
          assertEquals(
              datasetId,
              handle
                  .createQuery("SELECT dataset_id FROM match_entity WHERE purpose = :purpose")
                  .bind("purpose", purposeId)
                  .mapTo(Integer.class)
                  .one());
          assertEquals(
              "DUOS-1000000",
              handle
                  .createQuery("SELECT consent FROM match_entity WHERE purpose = :purpose")
                  .bind("purpose", purposeId)
                  .mapTo(String.class)
                  .one());
          assertEquals(
              datasetId,
              handle
                  .createQuery(
                      """
                      INSERT INTO match_entity (consent, purpose, match_entity, failed)
                      VALUES (:consent, :purpose, true, false)
                      RETURNING dataset_id
                      """)
                  .bind("consent", dataset.getDatasetIdentifier())
                  .bind("purpose", UUID.randomUUID().toString())
                  .mapTo(Integer.class)
                  .one());
          assertTrue(
              handle
                      .createQuery(
                          """
                          INSERT INTO dataset
                              (name, create_date, create_user_id, update_date,
                               update_user_id, object_id, data_use, alias)
                          VALUES (:name, :createDate, :userId, :createDate,
                                  :userId, :objectId, :dataUse, 5000000)
                          RETURNING alias
                          """)
                      .bind("name", "Legacy Sequence Dataset " + UUID.randomUUID())
                      .bind("createDate", FIXED_TIMESTAMP)
                      .bind("userId", user.getUserId())
                      .bind("objectId", "Legacy Sequence Object " + UUID.randomUUID())
                      .bind("dataUse", dataUse.toString())
                      .mapTo(Long.class)
                      .one()
                  < 5000000L);
          handle.commit();
        });
  }

  private boolean columnExists(
      org.jdbi.v3.core.Handle handle, String tableName, String columnName) {
    return handle
            .createQuery(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = :tableName
                  AND column_name = :columnName
                """)
            .bind("tableName", tableName)
            .bind("columnName", columnName)
            .mapTo(Integer.class)
            .one()
        == 1;
  }

  private boolean sequenceExists(org.jdbi.v3.core.Handle handle, String sequenceName) {
    return handle
            .createQuery(
                """
                SELECT COUNT(*)
                FROM information_schema.sequences
                WHERE sequence_schema = current_schema()
                  AND sequence_name = :sequenceName
                """)
            .bind("sequenceName", sequenceName)
            .mapTo(Integer.class)
            .one()
        == 1;
  }
}
