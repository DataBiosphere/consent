package org.broadinstitute.consent.http.service.studytemplate;

import java.util.List;

/** The study-template header, which every template version shares. */
final class TemplateColumns {

  static final String TEMPLATE_VERSION = "templateVersion";
  static final String RECORD_TYPE = "recordType";
  static final String RECORD_ID = "recordId";
  static final String PARENT_RECORD_ID = "parentRecordId";
  static final String FIELD = "field";
  static final String VALUE = "value";

  static final List<String> HEADERS =
      List.of(TEMPLATE_VERSION, RECORD_TYPE, RECORD_ID, PARENT_RECORD_ID, FIELD, VALUE);

  private TemplateColumns() {}
}
