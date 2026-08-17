package org.broadinstitute.consent.http.service.studytemplate;

/**
 * A single-field substitution on an already-mapped registration request. Any violation that
 * disappears while the field holds a value satisfying its own rule was caused by this row.
 *
 * <p>{@code suppress} marks a field whose cell already failed conversion. Its violation is a
 * consequence of the field being omitted, so it is dropped instead of reported a second time.
 */
record ViolationProbe(int row, boolean suppress, Runnable apply, Runnable restore) {}
