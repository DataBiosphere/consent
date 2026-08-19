package org.broadinstitute.consent.http.service.studytemplate;

import java.util.List;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;

/**
 * A parser for one major {@code templateVersion}. Dispatch is explicit rather than inferred so a
 * future v2 parser can coexist with v1 instead of replacing it.
 */
interface StudyTemplateParser {

  /** The single {@code templateVersion} cell value this parser accepts. */
  String majorVersion();

  /**
   * Builds the intermediate template model, recording record-model errors. The caller stops before
   * {@link #validate} when this reports anything, because the record model is then untrustworthy.
   */
  ParsedStudyTemplate parse(List<TemplateRow> rows, TemplateErrors errors);

  /**
   * Converts the parsed template into a registration request and applies the ordinary registration
   * validator, recording cell conversion errors and business violations.
   */
  StudyRegistrationRequest validate(ParsedStudyTemplate template, TemplateErrors errors);
}
