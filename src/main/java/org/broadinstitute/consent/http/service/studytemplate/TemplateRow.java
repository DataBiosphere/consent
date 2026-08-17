package org.broadinstitute.consent.http.service.studytemplate;

/** One data row of a study template, with the one-based CSV row it came from. */
record TemplateRow(
    int row,
    String templateVersion,
    String recordType,
    String recordId,
    String parentRecordId,
    String field,
    String value) {}
