package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.UserRoles;
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
  private static final String DAC_ID = "dacId";
  private static final String USER_ID = "userId";
  private static final String ACTION = "action";
  private static final String DATASET_IDS = "datasetIds";
  private static final String ACCESS_MANAGEMENT_VALUES = "accessManagementValues";
  private static final String CONTROLLED_ACCESS_MANAGEMENT = "controlled";
  private static final String UPDATE_DATASET_STATEMENT =
      """
          UPDATE dataset
          SET dac_id = null,
              dac_approval = null,
              dac_approval_date = null,
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
                       COALESCE(
                         'DUOS-' || LPAD(d.alias::TEXT, GREATEST(6, LENGTH(d.alias::TEXT)), '0'),
                         d.object_id,
                         d.dataset_id::TEXT),
                       ', '
                       ORDER BY COALESCE(
                         'DUOS-' || LPAD(d.alias::TEXT, GREATEST(6, LENGTH(d.alias::TEXT)), '0'),
                         d.object_id,
                         d.dataset_id::TEXT)
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
  private static final String AUDIT_EXTERNALIZED_DATASETS_STATEMENT =
      """
            INSERT INTO dataset_audit(dataset_id, change_action, modified_by_user, modification_date, object_id, name)
            SELECT d.dataset_id, :action, :userId, current_timestamp, d.object_id, d.name
            FROM dataset d
            WHERE d.dataset_id IN (<datasetIds>)
          """;

  @Inject
  public DacServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public void deleteDacAndRemoveDaaAssociation(User user, Dac dac) throws IllegalArgumentException {
    // fail fast
    if (dac == null) {
      throw new IllegalArgumentException("Invalid DAC");
    }
    jdbi.useTransaction(
        handle -> {
          DaaDAO daaDAO = handle.attach(DaaDAO.class);
          DacDAO dacDAO = handle.attach(DacDAO.class);
          DataAccessAgreement daa = dac.getAssociatedDaa();
          if (daa != null) {
            daaDAO.deleteDacDaaRelation(daa.getDaaId(), dac.getDacId(), user.getUserId());
          }

          // Find all dac chair/member user roles and audit each removal
          List<User> dacUsers = dacDAO.findMembersByDacId(dac.getDacId());
          dacUsers.stream()
              .map(User::getRoles)
              .flatMap(List::stream)
              .filter(
                  userRole ->
                      Objects.equals(userRole.getRoleId(), UserRoles.CHAIRPERSON.getRoleId())
                          || Objects.equals(userRole.getRoleId(), UserRoles.MEMBER.getRoleId()))
              .forEach(
                  userRole -> dacDAO.removeDacMember(userRole.getUserRoleId(), user.getUserId()));

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

          // Audit DAC removal
          dacDAO.deleteDac(dac.getDacId(), user.getUserId());
          handle.commit();
        });
  }

  public DacDatasetExternalizationResponse convertDacDatasetsToExternal(
      Integer dacId, Integer userId, DacDatasetExternalizationRequest request) {
    Instant startedAt = Instant.now();
    return jdbi.inTransaction(
        handle -> {
          List<Integer> datasetIds = findDatasetIdsForDac(handle, dacId);
          if (datasetIds.isEmpty()) {
            return buildEmptyResponse(dacId, request, startedAt);
          }

          List<Integer> convertibleDatasetIds =
              findConvertibleDatasets(handle, datasetIds, request);
          int externalDatasets = countExternalDatasets(handle, datasetIds);

          ExternalizationMetrics metrics = computeMetrics(handle, convertibleDatasetIds, request);

          if (!request.isDryRun() && !convertibleDatasetIds.isEmpty()) {
            executeExternalizationUpdates(handle, convertibleDatasetIds, userId, request);
          }

          return buildResponse(
              dacId,
              request,
              startedAt,
              datasetIds.size(),
              convertibleDatasetIds.size(),
              externalDatasets,
              metrics);
        });
  }

  private List<Integer> findDatasetIdsForDac(org.jdbi.v3.core.Handle handle, Integer dacId) {
    try (var query = handle.createQuery(FIND_DATASET_IDS_FOR_DAC_STATEMENT)) {
      return query.bind(DAC_ID, dacId).mapTo(Integer.class).list();
    }
  }

  private DacDatasetExternalizationResponse buildEmptyResponse(
      Integer dacId, DacDatasetExternalizationRequest request, Instant startedAt) {
    return new DacDatasetExternalizationResponse(
        dacId, request.isDryRun(), request.reason(), startedAt, Instant.now(), 0, 0, 0, 0, 0, 0);
  }

  private List<Integer> findConvertibleDatasets(
      org.jdbi.v3.core.Handle handle,
      List<Integer> datasetIds,
      DacDatasetExternalizationRequest request) {
    List<String> accessManagementValues = getAccessManagementValues(request);
    try (var query = handle.createQuery(FIND_CONVERTIBLE_DATASET_IDS_STATEMENT)) {
      return query
          .bindList(DATASET_IDS, datasetIds)
          .bindList(ACCESS_MANAGEMENT_VALUES, accessManagementValues)
          .mapTo(Integer.class)
          .list();
    }
  }

  private List<String> getAccessManagementValues(DacDatasetExternalizationRequest request) {
    return request.shouldConvertOpenAccessDatasets()
        ? List.of(CONTROLLED_ACCESS_MANAGEMENT, "open")
        : List.of(CONTROLLED_ACCESS_MANAGEMENT);
  }

  private int countExternalDatasets(org.jdbi.v3.core.Handle handle, List<Integer> datasetIds) {
    try (var query = handle.createQuery(COUNT_EXTERNAL_DATASETS_STATEMENT)) {
      return query.bindList(DATASET_IDS, datasetIds).mapTo(Integer.class).one();
    }
  }

  private ExternalizationMetrics computeMetrics(
      org.jdbi.v3.core.Handle handle,
      List<Integer> convertibleDatasetIds,
      DacDatasetExternalizationRequest request) {
    int darDatasetApprovalsRevoked = 0;
    int usersWithAccessRemoved = 0;
    if (request.shouldRevokeApprovedAccess() && !convertibleDatasetIds.isEmpty()) {
      darDatasetApprovalsRevoked = countDarDatasetRelations(handle, convertibleDatasetIds);
      usersWithAccessRemoved = countDistinctUsers(handle, convertibleDatasetIds);
    }

    int openElectionsCanceled = 0;
    if (request.shouldCancelOpenElections() && !convertibleDatasetIds.isEmpty()) {
      openElectionsCanceled = countOpenElections(handle, convertibleDatasetIds);
    }

    return new ExternalizationMetrics(
        darDatasetApprovalsRevoked, usersWithAccessRemoved, openElectionsCanceled);
  }

  private int countDarDatasetRelations(
      org.jdbi.v3.core.Handle handle, List<Integer> convertibleDatasetIds) {
    try (var query = handle.createQuery(COUNT_DAR_DATASET_RELATIONS_FOR_DATASETS_STATEMENT)) {
      return query.bindList(DATASET_IDS, convertibleDatasetIds).mapTo(Integer.class).one();
    }
  }

  private int countDistinctUsers(
      org.jdbi.v3.core.Handle handle, List<Integer> convertibleDatasetIds) {
    try (var query = handle.createQuery(COUNT_DISTINCT_USERS_FOR_DATASETS_STATEMENT)) {
      return query.bindList(DATASET_IDS, convertibleDatasetIds).mapTo(Integer.class).one();
    }
  }

  private int countOpenElections(
      org.jdbi.v3.core.Handle handle, List<Integer> convertibleDatasetIds) {
    try (var query = handle.createQuery(COUNT_OPEN_ELECTIONS_FOR_DATASETS_STATEMENT)) {
      return query.bindList(DATASET_IDS, convertibleDatasetIds).mapTo(Integer.class).one();
    }
  }

  private void executeExternalizationUpdates(
      org.jdbi.v3.core.Handle handle,
      List<Integer> convertibleDatasetIds,
      Integer userId,
      DacDatasetExternalizationRequest request) {
    List<String> accessManagementValues = getAccessManagementValues(request);

    try (var clearDac = handle.createUpdate(CLEAR_DAC_FIELDS_FOR_DATASETS_STATEMENT)) {
      clearDac.bindList(DATASET_IDS, convertibleDatasetIds).bind(USER_ID, userId).execute();
    }

    try (var updateToExternal =
        handle.createUpdate(UPDATE_CONTROLLED_DATASETS_TO_EXTERNAL_STATEMENT)) {
      updateToExternal
          .bindList(DATASET_IDS, convertibleDatasetIds)
          .bindList(ACCESS_MANAGEMENT_VALUES, accessManagementValues)
          .execute();
    }

    auditExternalizedDatasets(handle, convertibleDatasetIds, userId);

    if (request.shouldRevokeApprovedAccess()) {
      revokeApprovedAccess(handle, convertibleDatasetIds);
    }

    if (request.shouldCancelOpenElections()) {
      cancelOpenElections(handle, convertibleDatasetIds);
    }
  }

  private void revokeApprovedAccess(
      org.jdbi.v3.core.Handle handle, List<Integer> convertibleDatasetIds) {
    try (var appendNotes = handle.createUpdate(APPEND_ADMIN_DAR_NOTES_FOR_DATASETS_STATEMENT)) {
      appendNotes.bindList(DATASET_IDS, convertibleDatasetIds).execute();
    }
    try (var deleteRelations =
        handle.createUpdate(DELETE_DAR_DATASET_RELATIONS_FOR_DATASETS_STATEMENT)) {
      deleteRelations.bindList(DATASET_IDS, convertibleDatasetIds).execute();
    }
  }

  private void cancelOpenElections(
      org.jdbi.v3.core.Handle handle, List<Integer> convertibleDatasetIds) {
    try (var cancel = handle.createUpdate(CANCEL_OPEN_ELECTIONS_FOR_DATASETS_STATEMENT)) {
      cancel.bindList(DATASET_IDS, convertibleDatasetIds).execute();
    }
  }

  private void auditExternalizedDatasets(
      org.jdbi.v3.core.Handle handle, List<Integer> convertibleDatasetIds, Integer userId) {
    try (var audit = handle.createUpdate(AUDIT_EXTERNALIZED_DATASETS_STATEMENT)) {
      audit
          .bindList(DATASET_IDS, convertibleDatasetIds)
          .bind(USER_ID, userId)
          .bind(ACTION, AuditActions.UPDATE.getValue())
          .execute();
    }
  }

  private DacDatasetExternalizationResponse buildResponse(
      Integer dacId,
      DacDatasetExternalizationRequest request,
      Instant startedAt,
      int datasetsTotalInDac,
      int convertedToExternal,
      int externalDatasets,
      ExternalizationMetrics metrics) {
    return new DacDatasetExternalizationResponse(
        dacId,
        request.isDryRun(),
        request.reason(),
        startedAt,
        Instant.now(),
        datasetsTotalInDac,
        convertedToExternal,
        externalDatasets,
        metrics.darDatasetApprovalsRevoked(),
        metrics.openElectionsCanceled(),
        metrics.usersWithAccessRemoved());
  }

  private record ExternalizationMetrics(
      int darDatasetApprovalsRevoked, int usersWithAccessRemoved, int openElectionsCanceled) {}

  public List<Integer> findConvertibleDatasetIds(
      Integer dacId, DacDatasetExternalizationRequest request) {
    return jdbi.withHandle(
        handle -> {
          List<Integer> datasetIds;
          try (var dacQuery = handle.createQuery(FIND_DATASET_IDS_FOR_DAC_STATEMENT)) {
            datasetIds = dacQuery.bind(DAC_ID, dacId).mapTo(Integer.class).list();
          }
          if (datasetIds.isEmpty()) {
            return List.of();
          }
          List<String> accessManagementValues = getAccessManagementValues(request);
          try (var convertibleQuery = handle.createQuery(FIND_CONVERTIBLE_DATASET_IDS_STATEMENT)) {
            return convertibleQuery
                .bindList(DATASET_IDS, datasetIds)
                .bindList(ACCESS_MANAGEMENT_VALUES, accessManagementValues)
                .mapTo(Integer.class)
                .list();
          }
        });
  }
}
