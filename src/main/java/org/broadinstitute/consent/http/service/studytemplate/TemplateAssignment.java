package org.broadinstitute.consent.http.service.studytemplate;

/** One {@code field} assignment on a template record, with the row that carried it. */
record TemplateAssignment(String field, String value, int row) {}
