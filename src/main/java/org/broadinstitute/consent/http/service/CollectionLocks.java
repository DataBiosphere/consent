package org.broadinstitute.consent.http.service;

import java.sql.SQLException;
import java.util.Set;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;

/** Serializes collection changes with closeout submission on the collection row. */
final class CollectionLocks {

  private static final Set<String> LOCK_FAILURE_STATES = Set.of("40P01", "55P03", "40001");

  private CollectionLocks() {}

  /**
   * Locks the collection row until the caller's transaction commits, then rejects the change if the
   * collection already has a submitted closeout. Call it inside the transaction, with its DAO.
   */
  static void lockOpenCollection(DataAccessRequestDAO dao, Integer collectionId) {
    dao.lockCollection(collectionId);
    if (dao.hasSubmittedCloseout(collectionId)) {
      throw new ConsentConflictException("Cannot act on a closed out collection.");
    }
  }

  /**
   * Waiting on the collection lock can end in a deadlock or timeout when another change holds it.
   * That is a conflict the caller can retry, not a bad request.
   */
  static void throwIfLockFailure(Exception e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof SQLException sqlException
          && LOCK_FAILURE_STATES.contains(sqlException.getSQLState())) {
        throw new ConsentConflictException(
            "The collection is being changed by another request. Try again.");
      }
    }
  }
}
