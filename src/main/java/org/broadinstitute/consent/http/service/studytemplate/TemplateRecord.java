package org.broadinstitute.consent.http.service.studytemplate;

import java.util.List;
import java.util.Map;

/**
 * One logical template record: its identity, the row that first declared it, and its field
 * assignments keyed by field in file order. A scalar array field holds one assignment per item.
 */
record TemplateRecord(
    String recordType,
    String recordId,
    int firstRow,
    Map<String, List<TemplateAssignment>> assignments) {}
