package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacDatasetExternalizationRequest;
import org.broadinstitute.consent.http.models.DacDatasetExternalizationResponse;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DacServiceDAO implements ConsentLogger {

  private final Jdbi jdbi;
  private final DaaDAO daaDAO;
  private static final String DAC_ID = "dacId";
  private static final String USER_ID = "userId";
  private static final String DATASET_IDS = "datasetIds";
  private static final String ACCESS_MANAGEMENT_VALUES = "accessManagementValues";
  private static final String CONTROLLED_ACCESS_MANAGEMENT = "controlled";
  private static final String DELETE_ROLES_STATEMENT =
      "DELETE FROM user_role WHERE dac_id = :dacId";
  private static final String UPDATE_DATASET_STATEMENT =
      """
          UPDATE dataset
          SET dac_id = null,
              dac_approval = null,
              update_user_id = :userId,
              update_date = NOW()
          WHERE dac_id = :dacId
      """;
  private static final String DELETE_DAC_RULES_STATEMENT =
      "DELETE FROM dac_rule_settings WHERE dac_id = :dacId ";
  private static final String FIND_DATASET_IDS_FOR_DAC_STATEMENT =
      "SELECT dataset_id FROM dataset WHERE dac_id = :dacId";
  private static final String FIND_CONVERTIBLE_DATASET_IDS_STATEMENT =
      """
          SELECT DISTINCT dp.dataset_id
          FROM dataset_property dp
          WHERE dp.dataset_id IN (<datasetIds>)
            AND dp.schema_property = 'accessManagement'
            AND LOWER(dp.property_value) IN (<accessManagementValues>)
      """;
  private static final String COUNT_EXTERNAL_DATASETS_STATEMENT =
      """
          SELECT COUNT(DISTINCT dp.dataset_id)
          FROM dataset_property dp
          WHERE dp.dataset_id IN (<datasetIds>)
            AND dp.schema_property = 'accessManagement'
            AND LOWER(dp.property_value) = 'external'
      """;
  private static final String UPDATE_CONTROLLED_DATASETS_TO_EXTERNAL_STATEMENT =
      """
          UPDATE dataset_property
          SET property_value = 'external'
          WHERE dataset_id IN (<datasetIds>)
            AND schema_property = 'accessManagement'
            AND LOWER(property_value) IN (<accessManagementValues>)
      """;
  private static final String CLEAR_DAC_FIELDS_FOR_DATASETS_STATEMENT =
      """
          UPDATE dataset
          SET dac_id = null,
              dac_approval = null,
              dac_approval_date = null,
              update_user_id = :userId,
              update_date = NOW()
          WHERE dataset_id IN (<datasetIds>)
      """;
  private static final String APPEND_ADMIN_DAR_NOTES_FOR_DATASETS_STATEMENT =
      """
           UPDATE data_access_request dar
           SET admin_dar_notes =
                   CONCAT_WS(
                       ' ',
                       NULLIF(dar.admin_dar_notes, ''),
                       CONCAT(
                           'On ',
                           TO_CHAR(current_timestamp AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
                           ' the following datasets were removed administratively from this request because the responsible Data Access Committee no longer manages access using DUOS.',
                           ' ',
                           affected.removed_dataset_identifiers)),
               update_date = NOW()
            FROM (
              SELECT dd.reference_id,
                     STRING_AGG(
                       COALESCE('DUOS-' || LPAD(d.alias::TEXT, 6, '0'), d.object_id, d.dataset_id::TEXT),
                       ', '
                       ORDER BY COALESCE('DUOS-' || LPAD(d.alias::TEXT, 6, '0'), d.object_id, d.dataset_id::TEXT)
                     ) AS removed_dataset_identifiers
              FROM dar_dataset dd
              INNER JOIN dataset d ON d.dataset_id = dd.dataset_id
              WHERE dd.dataset_id IN (<datasetIds>)
              GROUP BY dd.reference_id
            ) affected
            WHERE dar.reference_id = affected.reference_id
        """;
  private static final String COUNT_DAR_DATASET_RELATIONS_FOR_DATASETS_STATEMENT =
      """
          SELECT COUNT(*)
          FROM dar_dataset dd
          WHERE dd.dataset_id IN (<datasetIds>)
      """;
  private static final String COUNT_DISTINCT_USERS_FOR_DATASETS_STATEMENT =
      """
          SELECT COUNT(DISTINCT dar.user_id)
          FROM data_access_request dar
          INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
          WHERE dd.dataset_id IN (<datasetIds>)
      """;
  private static final String DELETE_DAR_DATASET_RELATIONS_FOR_DATASETS_STATEMENT =
      """
          DELETE FROM dar_dataset
          WHERE dataset_id IN (<datasetIds>)
      """;
  private static final String COUNT_OPEN_ELECTIONS_FOR_DATASETS_STATEMENT =
      """
          SELECT COUNT(*)
          FROM election e
          WHERE e.dataset_id IN (<datasetIds>)
            AND LOWER(e.status) = 'open'
            AND LOWER(e.election_type) = 'dataaccess'
      """;
  private static final String CANCEL_OPEN_ELECTIONS_FOR_DATASETS_STATEMENT =
      """
          UPDATE election
          SET status = 'Canceled',
              archived = true,
              last_update = NOW()
          WHERE dataset_id IN (<datasetIds>)
            AND LOWER(status) = 'open'
            AND LOWER(election_type) = 'dataaccess'
      """;
  private static final String AUDIT_DAC_RULE_DELETION_STATEMENT =
      """
            INSERT INTO dac_rule_audit(action, dac_id, rule_id, user_id, action_date)
            SELECT 'REMOVE', s.dac_id, s.rule_id, :userId, current_timestamp
            FROM dac_rule_settings s
            WHERE dac_id = :dacId
          """;
  private static final String DELETE_DAC_STATEMENT = "DELETE FROM dac where dac_id = :dacId";

  @Inject
  public DacServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
    daaDAO = jdbi.onDemand(DaaDAO.class);
  }

  public void deleteDacAndRemoveDaaAssociation(User user, Dac dac) throws IllegalArgumentException {
    // fail fast
    if (dac == null) {
      throw new IllegalArgumentException("Invalid DAC");
    }
    jdbi.useTransaction(
        handle -> {
          DataAccessAgreement daa = dac.getAssociatedDaa();
          if (daa != null) {
            daaDAO.deleteDacDaaRelation(daa.getDaaId(), dac.getDacId(), user.getUserId());
          }

          Update memberDeletion = handle.createUpdate(DELETE_ROLES_STATEMENT);
          memberDeletion.bind(DAC_ID, dac.getDacId());
          memberDeletion.execute();

          Update datasetUpdate = handle.createUpdate(UPDATE_DATASET_STATEMENT);
          datasetUpdate.bind(DAC_ID, dac.getDacId());
          datasetUpdate.bind(USER_ID, user.getUserId());
          datasetUpdate.execute();

          Update dacAutomationRulesDeletionAudit =
              handle.createUpdate(AUDIT_DAC_RULE_DELETION_STATEMENT);
          dacAutomationRulesDeletionAudit.bind(DAC_ID, dac.getDacId());
          dacAutomationRulesDeletionAudit.bind(USER_ID, user.getUserId());
          dacAutomationRulesDeletionAudit.execute();

          Update dacAutomationRulesDeletion = handle.createUpdate(DELETE_DAC_RULES_STATEMENT);
          dacAutomationRulesDeletion.bind(DAC_ID, dac.getDacId());
          dacAutomationRulesDeletion.execute();

          Update dacDeletion = handle.createUpdate(DELETE_DAC_STATEMENT);
          dacDeletion.bind(DAC_ID, dac.getDacId());
          dacDeletion.execute();
          handle.commit();
        });
  }

  public DacDatasetExternalizationResponse convertDacDatasetsToExternal(
      Integer dacId, Integer userId, DacDatasetExternalizationRequest request) {
    Instant startedAt = Instant.now();
    return jdbi.inTransaction(
        handle -> {
          List<Integer> datasetIds =
              handle
                  .createQuery(FIND_DATASET_IDS_FOR_DAC_STATEMENT)
                  .bind(DAC_ID, dacId)
                  .mapTo(Integer.class)
                  .list();
          if (datasetIds.isEmpty()) {
            return new DacDatasetExternalizationResponse(
                dacId,
                request.isDryRun(),
                request.reason(),
                startedAt,
                Instant.now(),
                0,
                0,
                0,
                0,
                0,
                0);
          }

          List<String> accessManagementValues =
              request.shouldConvertOpenAccessDatasets()
                  ? List.of(CONTROLLED_ACCESS_MANAGEMENT, "open")
                  : List.of(CONTROLLED_ACCESS_MANAGEMENT);
          List<Integer> convertibleDatasetIds =
              handle
                  .createQuery(FIND_CONVERTIBLE_DATASET_IDS_STATEMENT)
                  .bindList(DATASET_IDS, datasetIds)
                  .bindList(ACCESS_MANAGEMENT_VALUES, accessManagementValues)
                  .mapTo(Integer.class)
                  .list();
          int externalDatasets =
              handle
                  .createQuery(COUNT_EXTERNAL_DATASETS_STATEMENT)
                  .bindList(DATASET_IDS, datasetIds)
                  .mapTo(Integer.class)
                  .one();
          int convertedToExternal = convertibleDatasetIds.size();

          int darDatasetApprovalsRevoked = 0;
          int usersWithAccessRemoved = 0;
          if (request.shouldRevokeApprovedAccess() && !convertibleDatasetIds.isEmpty()) {
            darDatasetApprovalsRevoked =
                handle
                    .createQuery(COUNT_DAR_DATASET_RELATIONS_FOR_DATASETS_STATEMENT)
                    .bindList(DATASET_IDS, convertibleDatasetIds)
                    .mapTo(Integer.class)
                    .one();
            usersWithAccessRemoved =
                handle
                    .createQuery(COUNT_DISTINCT_USERS_FOR_DATASETS_STATEMENT)
                    .bindList(DATASET_IDS, convertibleDatasetIds)
                    .mapTo(Integer.class)
                    .one();
          }

          int openElectionsCanceled = 0;
          if (request.shouldCancelOpenElections() && !convertibleDatasetIds.isEmpty()) {
            openElectionsCanceled =
                handle
                    .createQuery(COUNT_OPEN_ELECTIONS_FOR_DATASETS_STATEMENT)
                    .bindList(DATASET_IDS, convertibleDatasetIds)
                    .mapTo(Integer.class)
                    .one();
          }

          if (!request.isDryRun() && !convertibleDatasetIds.isEmpty()) {
            handle
                .createUpdate(CLEAR_DAC_FIELDS_FOR_DATASETS_STATEMENT)
                .bindList(DATASET_IDS, convertibleDatasetIds)
                .bind(USER_ID, userId)
                .execute();

            handle
                .createUpdate(UPDATE_CONTROLLED_DATASETS_TO_EXTERNAL_STATEMENT)
                .bindList(DATASET_IDS, convertibleDatasetIds)
                .bindList(ACCESS_MANAGEMENT_VALUES, accessManagementValues)
                .execute();

            if (request.shouldRevokeApprovedAccess()) {
              handle
                  .createUpdate(APPEND_ADMIN_DAR_NOTES_FOR_DATASETS_STATEMENT)
                  .bindList(DATASET_IDS, convertibleDatasetIds)
                  .execute();

              handle
                  .createUpdate(DELETE_DAR_DATASET_RELATIONS_FOR_DATASETS_STATEMENT)
                  .bindList(DATASET_IDS, convertibleDatasetIds)
                  .execute();
            }

            if (request.shouldCancelOpenElections()) {
              handle
                  .createUpdate(CANCEL_OPEN_ELECTIONS_FOR_DATASETS_STATEMENT)
                  .bindList(DATASET_IDS, convertibleDatasetIds)
                  .execute();
            }
          }

          return new DacDatasetExternalizationResponse(
              dacId,
              request.isDryRun(),
              request.reason(),
              startedAt,
              Instant.now(),
              datasetIds.size(),
              convertedToExternal,
              externalDatasets,
              darDatasetApprovalsRevoked,
              openElectionsCanceled,
              usersWithAccessRemoved);
        });
  }

  public List<Integer> findConvertibleDatasetIds(
      Integer dacId, DacDatasetExternalizationRequest request) {
    return jdbi.withHandle(
        handle -> {
          List<Integer> datasetIds =
              handle
                  .createQuery(FIND_DATASET_IDS_FOR_DAC_STATEMENT)
                  .bind(DAC_ID, dacId)
                  .mapTo(Integer.class)
                  .list();
          if (datasetIds.isEmpty()) {
            return List.of();
          }
          List<String> accessManagementValues =
              request.shouldConvertOpenAccessDatasets()
                  ? List.of(CONTROLLED_ACCESS_MANAGEMENT, "open")
                  : List.of(CONTROLLED_ACCESS_MANAGEMENT);
          return handle
              .createQuery(FIND_CONVERTIBLE_DATASET_IDS_STATEMENT)
              .bindList(DATASET_IDS, datasetIds)
              .bindList(ACCESS_MANAGEMENT_VALUES, accessManagementValues)
              .mapTo(Integer.class)
              .list();
        });
  }
}
