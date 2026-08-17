package org.broadinstitute.consent.http.service.studytemplate;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * One supported template field: how to convert its cells and how to read and write it on the DTO it
 * belongs to. A {@code multiValued} field is assembled from repeated rows and written as a list.
 *
 * <p>{@code probeValue} is a value that satisfies whatever rule {@code
 * StudyRegistrationRequestValidator} applies to this field on its own, or {@code null} when the
 * field has no such rule. Substituting it lets a violation be attributed to the row that caused it
 * without restating the rule or reading its message; see {@link RegistrationViolationAttributor}.
 * For a {@code multiValued} field it is the probe for one item, substituted into the assembled list
 * one item at a time.
 */
record TemplateField<T>(
    String name,
    boolean multiValued,
    CellConverter converter,
    Function<T, Object> reader,
    BiConsumer<T, Object> writer,
    Object probeValue) {}
