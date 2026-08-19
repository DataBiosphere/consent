package org.broadinstitute.consent.http.service.studytemplate;

/**
 * A single-field substitution on an already-mapped registration request. Any violation that
 * disappears while the field holds a value satisfying its own rule was caused by that field.
 *
 * <p>What the field held before the substitution decides how far that goes, which is what {@link
 * Kind} records.
 */
record ViolationProbe(int row, Kind kind, Runnable apply, Runnable restore) {

  enum Kind {
    /**
     * The cell failed conversion. Its violation is a consequence of the field being omitted, so it
     * is dropped instead of reported a second time.
     */
    SUPPRESSED,
    /** The cell is empty, so a violation it resolves is that cell's own missing value. */
    UNSET,
    /** The cell holds a value, so a violation it resolves is about that value. */
    SET,
    /**
     * The field has no cell in the file, so there is nothing to report a row against. Such a probe
     * only shows that a violation belongs to the absent field rather than to whichever cell made
     * the field required; its row is unused.
     */
    ABSENT
  }
}
