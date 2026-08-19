package org.broadinstitute.consent.http.service.studytemplate;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;
import org.broadinstitute.consent.http.service.studytemplate.ViolationProbe.Kind;

/**
 * Runs the ordinary registration validator over a mapped template and gives its violations the row
 * that caused them, so template and manual submissions accept exactly the same business data.
 *
 * <p>Attribution is by substitution rather than by reading the validator's messages: a field is
 * temporarily set to a value that satisfies its own rule, and whichever violations disappear belong
 * to that field. A row is reported only when the field has a cell to point at, and an empty cell
 * outranks a filled one: a conditional requirement is resolved both by filling the field it asks
 * for and by changing the choice that asked, and only the first of those is a cell the producer
 * needs to edit.
 *
 * <p>Each scope is attributed against only its own violations. The validator dedupes messages, so
 * asking the whole request would let one dataset's error mask an identical error in a sibling — and
 * would leave both unlocated, since no single substitution can make the shared message disappear.
 */
final class RegistrationViolationAttributor {

  private final StudyRegistrationRequestValidator validator =
      new StudyRegistrationRequestValidator();

  /** Study-scoped violations. One that no cell accounts for stays message-only. */
  void collectStudy(
      StudyRegistrationRequest request, List<ViolationProbe> probes, TemplateErrors errors) {
    attribute(() -> validator.collectStudyViolations(request), probes, errors, errors::message);
  }

  /**
   * The violations of one consent group. One that no cell accounts for is still located, on the
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

    ofKind(probes, Kind.SUPPRESSED)
        .forEach(
            probe -> unattributed.removeAll(violationsResolvedBy(violations, baseline, probe)));

    // Filling a field the file never mentions can only resolve that field's own requirement, so a
    // cell that merely triggered the requirement must not be blamed for it.
    Set<String> ofAnAbsentField = new HashSet<>();
    ofKind(probes, Kind.ABSENT)
        .forEach(
            probe -> ofAnAbsentField.addAll(violationsResolvedBy(violations, baseline, probe)));

    locate(violations, baseline, ofKind(probes, Kind.UNSET), unattributed, errors, Set.of());
    locate(violations, baseline, ofKind(probes, Kind.SET), unattributed, errors, ofAnAbsentField);

    unattributed.forEach(unattributedSink);
  }

  private void locate(
      Supplier<List<String>> violations,
      List<String> baseline,
      Stream<ViolationProbe> probes,
      Set<String> unattributed,
      TemplateErrors errors,
      Set<String> notAttributable) {
    probes
        .sorted(Comparator.comparingInt(ViolationProbe::row))
        .forEach(
            probe ->
                violationsResolvedBy(violations, baseline, probe).stream()
                    .filter(violation -> !notAttributable.contains(violation))
                    .forEach(
                        violation -> {
                          if (unattributed.remove(violation)) {
                            errors.at(probe.row(), TemplateColumns.VALUE, violation);
                          }
                        }));
  }

  private static Stream<ViolationProbe> ofKind(List<ViolationProbe> probes, Kind kind) {
    return probes.stream().filter(probe -> probe.kind() == kind);
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
