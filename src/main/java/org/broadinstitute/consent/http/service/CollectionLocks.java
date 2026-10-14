package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;

/** Serializes collection changes with closeout submission on the collection row. */
final class CollectionLocks {

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
}
