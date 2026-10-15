package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.jdbi.v3.sqlobject.transaction.TransactionalCallback;
import org.mockito.Mockito;

/** Mocks for services that lock a collection inside {@code DataAccessRequestDAO#inTransaction}. */
final class TransactionMocks {

  private TransactionMocks() {}

  /**
   * A strict DataAccessRequestDAO mock whose inTransaction runs the callback on the mock itself.
   */
  static DataAccessRequestDAO dataAccessRequestDAO() {
    DataAccessRequestDAO[] self = new DataAccessRequestDAO[1];
    self[0] =
        Mockito.mock(
            DataAccessRequestDAO.class,
            invocation -> {
              if (invocation.getMethod().getName().equals("inTransaction")) {
                TransactionalCallback<?, DataAccessRequestDAO, Exception> callback =
                    invocation.getArgument(0);
                return callback.inTransaction(self[0]);
              }
              return Mockito.RETURNS_DEFAULTS.answer(invocation);
            });
    return self[0];
  }
}
