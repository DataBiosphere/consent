package org.broadinstitute.consent.http.enumeration;

/**
 * Represents the type of operation being performed on a FileStorageObject. Used by
 * FileStorageObjectService.checkAccess(...) to determine which authorization rules apply.
 */
public enum OperationType {
  /** Read-only operations: listing documents, fetching metadata, downloading files. */
  READ,

  /** Create / update / delete operations. */
  WRITE
}
