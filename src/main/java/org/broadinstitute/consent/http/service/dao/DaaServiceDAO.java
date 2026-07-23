package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.ToIntFunction;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.models.DaaBulkRelationResult;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class DaaServiceDAO implements ConsentLogger {

  private final Jdbi jdbi;
  private final DaaDAO daaDAO;
  private final FileStorageObjectDAO fsoDAO;

  @Inject
  public DaaServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
    this.daaDAO = jdbi.onDemand(DaaDAO.class);
    this.fsoDAO = jdbi.onDemand(FileStorageObjectDAO.class);
  }

  public Integer createDaaWithFso(Integer userId, Integer dacId, FileStorageObject fso)
      throws SQLException {
    List<Integer> createdDaaIds = new ArrayList<>();
    jdbi.useHandle(
        handle -> {
          handle.getConnection().setAutoCommit(false);
          Instant now = Instant.now();
          try {
            Integer daaId = daaDAO.createDaa(userId, now, userId, now, dacId);
            createdDaaIds.add(daaId);
            daaDAO.createDacDaaRelation(dacId, daaId, userId);
            if (fso != null) {
              if (fso.getFileName() == null) {
                throw new IllegalArgumentException("FileStorageObject must have a File Name");
              }
              if (fso.getCategory() == null) {
                throw new IllegalArgumentException("FileStorageObject must have a FileCategory");
              }
              if (fso.getBlobId() == null) {
                throw new IllegalArgumentException("FileStorageObject must have a BlobId");
              }
              fsoDAO.insertNewFile(
                  fso.getFileName(),
                  fso.getCategory().getValue(),
                  fso.getBlobId().toGsUtilUri(),
                  fso.getMediaType(),
                  daaId.toString(),
                  userId,
                  now);
            }
          } catch (Exception e) {
            handle.rollback();
            logException(e);
            throw e;
          }
          handle.commit();
        });
    return createdDaaIds.isEmpty() ? null : createdDaaIds.getFirst();
  }

  // ── Atomic bulk pre-authorization operations (DT-3325) ──────────────────────────
  //
  // Each of the four methods below performs the entire batch inside a single
  // jdbi.useTransaction(...) so that a mid-batch failure rolls the whole batch back —
  // the SO never ends up with a half-applied set of relationships. The lc_daa audit
  // rows are written by the same SQL statement as the relation itself, so audit history
  // is preserved atomically. Library-card auto-create happens inside the transaction;
  // the "new card issued" email is deliberately left to the caller to send AFTER commit.

  /**
   * Atomically pre-authorize every user in {@code users} for a single DAA. Any user lacking a
   * library card has one created inside the transaction.
   */
  public DaaBulkRelationResult bulkAddUsersToDaa(
      Integer daaId, List<User> users, User signingOfficial) {
    return inBulkTransaction(
        users.size(),
        lcDAO -> {
          int applied = 0;
          for (User user : users) {
            Integer lcId = findOrCreateLibraryCardId(lcDAO, user, signingOfficial);
            applied +=
                lcDAO.createLibraryCardDaaRelation(
                    user.getUserId(), signingOfficial.getUserId(), lcId, daaId);
          }
          return applied;
        });
  }

  /** Atomically remove pre-authorization for a single DAA from every user in {@code users}. */
  public DaaBulkRelationResult bulkRemoveUsersFromDaa(
      Integer daaId, List<User> users, User signingOfficial) {
    return inBulkTransaction(
        users.size(),
        lcDAO -> {
          int applied = 0;
          for (User user : users) {
            LibraryCard lc = lcDAO.findLibraryCardByUserId(user.getUserId());
            if (lc != null) {
              applied +=
                  lcDAO.deleteLibraryCardDaaRelation(
                      user.getUserId(), signingOfficial.getUserId(), lc.getId(), daaId);
            }
          }
          return applied;
        });
  }

  /**
   * Atomically pre-authorize a single user for every DAA in {@code daaIds}. If the user lacks a
   * library card one is created inside the transaction.
   */
  public DaaBulkRelationResult bulkAddDaasToUser(
      User user, List<Integer> daaIds, User signingOfficial) {
    return inBulkTransaction(
        daaIds.size(),
        lcDAO -> {
          Integer lcId = findOrCreateLibraryCardId(lcDAO, user, signingOfficial);
          int applied = 0;
          for (Integer daaId : daaIds) {
            applied +=
                lcDAO.createLibraryCardDaaRelation(
                    user.getUserId(), signingOfficial.getUserId(), lcId, daaId);
          }
          return applied;
        });
  }

  /** Atomically remove pre-authorization for every DAA in {@code daaIds} from a single user. */
  public DaaBulkRelationResult bulkRemoveDaasFromUser(
      User user, List<Integer> daaIds, User signingOfficial) {
    return inBulkTransaction(
        daaIds.size(),
        lcDAO -> {
          LibraryCard lc = lcDAO.findLibraryCardByUserId(user.getUserId());
          if (lc == null) {
            return 0;
          }
          int applied = 0;
          for (Integer daaId : daaIds) {
            applied +=
                lcDAO.deleteLibraryCardDaaRelation(
                    user.getUserId(), signingOfficial.getUserId(), lc.getId(), daaId);
          }
          return applied;
        });
  }

  /**
   * Returns the id of the user's existing library card, creating a bare card (via the supplied
   * transaction-scoped DAO) if none exists. Must be called from within a transaction so the card
   * creation participates in the same all-or-nothing batch.
   */
  private Integer findOrCreateLibraryCardId(LibraryCardDAO lcDAO, User user, User signingOfficial) {
    LibraryCard existing = lcDAO.findLibraryCardByUserId(user.getUserId());
    if (existing != null) {
      return existing.getId();
    }
    return lcDAO.insertLibraryCard(
        user.getUserId(),
        user.getDisplayName(),
        user.getEmail(),
        signingOfficial.getUserId(),
        new Date());
  }

  /**
   * Runs {@code work} inside a single transaction against a transaction-scoped {@link
   * LibraryCardDAO} and wraps the count it returns (the number of relationships actually changed)
   * in a {@link DaaBulkRelationResult}. Centralizing the transaction boundary keeps each bulk
   * method to just its per-item loop, and guarantees the all-or-nothing rollback semantics are
   * applied identically across all four operations.
   */
  private DaaBulkRelationResult inBulkTransaction(
      int requested, ToIntFunction<LibraryCardDAO> work) {
    int[] applied = {0};
    jdbi.useTransaction(
        handle -> applied[0] = work.applyAsInt(handle.attach(LibraryCardDAO.class)));
    return summarize(requested, applied[0]);
  }

  /**
   * Builds a result from the requested count and the number of relationships actually changed.
   * {@code applied} counts only rows that were genuinely inserted/deleted (the audit-writing
   * statement runs {@code WHERE EXISTS}, so it returns 1 only on a real change), so re-adding an
   * existing relation or removing one that was never there is reported under {@code skipped} rather
   * than inflating {@code applied}. Because the batch is atomic, any true failure throws and rolls
   * back rather than surfacing here, so {@code errors} is always empty.
   */
  private DaaBulkRelationResult summarize(int requested, int applied) {
    return new DaaBulkRelationResult(requested, applied, requested - applied, List.of());
  }
}
