package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.models.Dac;
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
  private static final String DELETE_ROLES_STATEMENT =
      "DELETE FROM user_role WHERE dac_id = :dacId";
  private static final String UPDATE_DATASET_STATEMENT =
      "UPDATE dataset SET dac_id = null, dac_approval = null WHERE dac_id = :dacId";
  private static final String DELETE_DAC_RULES_STATEMENT =
      "DELETE FROM dac_rule_settings WHERE dac_id = :dacId ";
  private static final String AUDIT_DAC_RULE_DELETION_STATEMENT =
      """
            INSERT INTO dac_rule_audit(action, dac_id, rule_id, user_id, action_date)
            SELECT 'REMOVE', s.dac_id, s.rule_id, :userId, current_timestamp
            FROM dac_rule_settings s
            WHERE dac_id = :dacId
          """;

  /**
   * Audit the removal of every member/chair belonging to the DAC being deleted. One row is inserted
   * per user_role entry so the audit trail records which users lost access.
   */
  private static final String AUDIT_DAC_MEMBER_DELETION_STATEMENT =
      """
            INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
            SELECT dac_id, :userId, ur.user_id, ur.role_id, 'REMOVE', current_timestamp
            FROM user_role ur
            WHERE dac_id = :dacId
          """;

  /** Audit the DAC-level DELETE event itself. */
  private static final String AUDIT_DAC_DELETION_STATEMENT =
      """
            INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
            VALUES (:dacId, :userId, NULL, NULL, 'DELETE', current_timestamp)
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

          // Audit each member/chair removal before the rows are deleted.
          Update dacMemberAudit = handle.createUpdate(AUDIT_DAC_MEMBER_DELETION_STATEMENT);
          dacMemberAudit.bind(DAC_ID, dac.getDacId());
          dacMemberAudit.bind(USER_ID, user.getUserId());
          dacMemberAudit.execute();

          // Audit the DAC deletion itself before the dac row is removed.
          Update dacDeletionAudit = handle.createUpdate(AUDIT_DAC_DELETION_STATEMENT);
          dacDeletionAudit.bind(DAC_ID, dac.getDacId());
          dacDeletionAudit.bind(USER_ID, user.getUserId());
          dacDeletionAudit.execute();

          Update memberDeletion = handle.createUpdate(DELETE_ROLES_STATEMENT);
          memberDeletion.bind(DAC_ID, dac.getDacId());
          memberDeletion.execute();

          Update datasetUpdate = handle.createUpdate(UPDATE_DATASET_STATEMENT);
          datasetUpdate.bind(DAC_ID, dac.getDacId());
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
}
