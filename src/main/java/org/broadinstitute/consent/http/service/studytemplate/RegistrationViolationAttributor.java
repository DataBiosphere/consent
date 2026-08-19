package org.broadinstitute.consent.http.service.studytemplate;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;

/**
 * Runs the ordinary registration validator over a mapped template and gives its violations the row
 * that caused them, so template and manual submissions accept exactly the same business data.
 *
 * <p>Attribution is by substitution rather than by reading the validator's messages: a field is
 * temporarily set to a value that satisfies its own rule, and whichever violations disappear belong
 * to that field's row.
 *
 * <p>Each scope is attributed against only its own violations. The validator dedupes messages, so
 * asking the whole request would let one dataset's error mask an identical error in a sibling — and
 * would leave both unlocated, since no single substitution can make the shared message disappear.
 */
final class RegistrationViolationAttributor {

  private final StudyRegistrationRequestValidator validator =
      new StudyRegistrationRequestValidator();

  /** Study-scoped violations. One that no field accounts for stays message-only. */
  void collectStudy(
      StudyRegistrationRequest request, List<ViolationProbe> probes, TemplateErrors errors) {
    attribute(() -> validator.collectStudyViolations(request), probes, errors, errors::message);
  }

  /**
   * The violations of one consent group. One that no field accounts for is still located, on the
   * record's first row: rules such as the primary data use check span several fields, so no single
   * substitution resolves them, and the producer still needs to know which dataset to fix.
   */
  void collectConsentGroup(
      ConsentGroupRequest consentGroup,
      int recordRow,
      List<ViolationProbe> probes,
      TemplateErrors errors) {
    attribute(
        () -> validator.collectConsentGroupViolations(consentGroup),
        probes,
        errors,
        violation -> errors.at(recordRow, violation));
  }

  private void attribute(
      Supplier<List<String>> violations,
      List<ViolationProbe> probes,
      TemplateErrors errors,
      Consumer<String> unattributedSink) {
    List<String> baseline = violations.get();
    if (baseline.isEmpty()) {
      return;
    }
    Set<String> unattributed = new LinkedHashSet<>(baseline);

    probes.stream()
        .filter(ViolationProbe::suppress)
        .forEach(
            probe -> unattributed.removeAll(violationsResolvedBy(violations, baseline, probe)));

    probes.stream()
        .filter(probe -> !probe.suppress())
        .sorted(Comparator.comparingInt(ViolationProbe::row))
        .forEach(
            probe ->
                violationsResolvedBy(violations, baseline, probe)
                    .forEach(
                        violation -> {
                          if (unattributed.remove(violation)) {
                            errors.at(probe.row(), TemplateColumns.VALUE, violation);
                          }
                        }));

    unattributed.forEach(unattributedSink);
  }

  private List<String> violationsResolvedBy(
      Supplier<List<String>> violations, List<String> baseline, ViolationProbe probe) {
    probe.apply().run();
    try {
      Set<String> remaining = new HashSet<>(violations.get());
      return baseline.stream().filter(violation -> !remaining.contains(violation)).toList();
    } finally {
      probe.restore().run();
    }
  }
}
