package org.broadinstitute.consent.http.enumeration;

/** Why a DAR document's file was deleted from the bucket. */
public enum DarDocumentDeletionReason {
  /** An upload replaced it. */
  REPLACED,
  /** A draft save or submit removed it. */
  REMOVED,
  /** A save failed, so the file it uploaded wasn't kept. */
  FAILED_SAVE_CLEANUP,
  /** A progress report failed, so the files it uploaded weren't kept. */
  FAILED_REPORT_CLEANUP
}
