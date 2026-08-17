package org.broadinstitute.consent.http.service.studytemplate;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;

/**
 * Runs the ordinary registration validator over a mapped template and gives its violations the row
 * that caused them, so template and manual submissions accept exactly the same business data.
 *
 * <p>Attribution is by substitution rather than by reading the validator's messages: a field is
 * temporarily set to a value that satisfies its own rule, and whichever violations disappear belong
 * to that field's row. Violations no field accounts for stay message-only.
 */
final class RegistrationViolationAttributor {

  private final StudyRegistrationRequestValidator validator =
      new StudyRegistrationRequestValidator();

  void collect(
      StudyRegistrationRequest request, List<ViolationProbe> probes, TemplateErrors errors) {
    List<String> baseline = validator.collectViolations(request);
    if (baseline.isEmpty()) {
      return;
    }
    Set<String> unattributed = new LinkedHashSet<>(baseline);

    probes.stream()
        .filter(ViolationProbe::suppress)
        .forEach(probe -> unattributed.removeAll(violationsResolvedBy(request, baseline, probe)));

    probes.stream()
        .filter(probe -> !probe.suppress())
        .sorted(Comparator.comparingInt(ViolationProbe::row))
        .forEach(
            probe ->
                violationsResolvedBy(request, baseline, probe)
                    .forEach(
                        violation -> {
                          if (unattributed.remove(violation)) {
                            errors.at(probe.row(), TemplateColumns.VALUE, violation);
                          }
                        }));

    unattributed.forEach(errors::message);
  }

  private List<String> violationsResolvedBy(
      StudyRegistrationRequest request, List<String> baseline, ViolationProbe probe) {
    probe.apply().run();
    try {
      Set<String> remaining = new HashSet<>(validator.collectViolations(request));
      return baseline.stream().filter(violation -> !remaining.contains(violation)).toList();
    } finally {
      probe.restore().run();
    }
  }
}
