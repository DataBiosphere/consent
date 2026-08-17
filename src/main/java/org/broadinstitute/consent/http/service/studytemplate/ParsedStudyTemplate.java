package org.broadinstitute.consent.http.service.studytemplate;

import java.util.List;
import java.util.Map;

/**
 * The intermediate template model produced before any registration DTO exists: the single study
 * record, its consent groups in file order, and each consent group's fileType records in file order
 * keyed by consent-group {@code recordId}.
 */
record ParsedStudyTemplate(
    TemplateRecord study,
    List<TemplateRecord> consentGroups,
    Map<String, List<TemplateRecord>> fileTypes) {}
