package org.broadinstitute.consent.http.service.studytemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.service.studytemplate.CellConverter.CellValue;
import org.broadinstitute.consent.http.service.studytemplate.ViolationProbe.Kind;

/** Parses and maps the v1 study template described in {@code docs/study-template-v1.md}. */
final class StudyTemplateV1Parser implements StudyTemplateParser {

  /** The v1 {@code recordType} values. */
  static final String STUDY = "study";

  static final String CONSENT_GROUP = "consentGroup";
  static final String FILE_TYPE = "fileType";

  /** The v1 contract allows exactly one study record and fixes its {@code recordId}. */
  private static final String STUDY_RECORD_ID = "study";

  /** The row of a probe that has no cell to report; see {@link ViolationProbe.Kind#ABSENT}. */
  private static final int NO_ROW = 0;

  private final RegistrationViolationAttributor attributor = new RegistrationViolationAttributor();

  @Override
  public String majorVersion() {
    return "1";
  }

  @Override
  public ParsedStudyTemplate parse(List<TemplateRow> rows, TemplateErrors errors) {
    Map<RecordKey, RecordBuilder> builders = new LinkedHashMap<>();
    for (TemplateRow row : rows) {
      fieldsFor(row, errors)
          .ifPresent(
              fields -> {
                RecordBuilder builder =
                    builders.computeIfAbsent(
                        new RecordKey(row.recordType(), row.recordId()),
                        key -> new RecordBuilder(key.recordType(), key.recordId(), row));
                assign(builder, fields, row, errors);
              });
    }

    RecordBuilder study = builders.get(new RecordKey(STUDY, STUDY_RECORD_ID));
    if (study == null) {
      errors.message("Template must contain a study record");
    }

    List<RecordBuilder> consentGroups = buildersOfType(builders, CONSENT_GROUP);
    Set<String> consentGroupIds = new LinkedHashSet<>();
    consentGroups.forEach(builder -> consentGroupIds.add(builder.recordId));

    Map<String, List<TemplateRecord>> fileTypes = new LinkedHashMap<>();
    for (RecordBuilder builder : buildersOfType(builders, FILE_TYPE)) {
      if (consentGroupIds.contains(builder.parentRecordId)) {
        fileTypes
            .computeIfAbsent(builder.parentRecordId, key -> new ArrayList<>())
            .add(builder.build());
      } else {
        errors.at(
            builder.firstRow,
            TemplateColumns.PARENT_RECORD_ID,
            "Unknown consent group '%s' for fileType record '%s'"
                .formatted(builder.parentRecordId, builder.recordId));
      }
    }

    return new ParsedStudyTemplate(
        study == null ? null : study.build(),
        consentGroups.stream().map(RecordBuilder::build).toList(),
        fileTypes);
  }

  @Override
  public StudyRegistrationRequest validate(ParsedStudyTemplate template, TemplateErrors errors) {
    List<ViolationProbe> studyProbes = new ArrayList<>();
    StudyRegistrationRequest request = new StudyRegistrationRequest();
    if (template.study() != null) {
      apply(StudyTemplateV1Fields.STUDY, template.study(), request, errors, studyProbes);
    }

    List<ConsentGroupRequest> consentGroups = new ArrayList<>();
    for (TemplateRecord consentGroupRecord : template.consentGroups()) {
      // A fresh probe list per group, so each group's violations attribute against its own scope.
      List<ViolationProbe> probes = new ArrayList<>();
      ConsentGroupRequest consentGroup =
          consentGroup(
              consentGroupRecord,
              template.fileTypes().get(consentGroupRecord.recordId()),
              errors,
              probes);
      consentGroups.add(consentGroup);
      attributor.collectConsentGroup(consentGroup, consentGroupRecord.firstRow(), probes, errors);
    }
    if (!consentGroups.isEmpty()) {
      request.setConsentGroups(consentGroups);
    }

    // After the consent groups are attached, so the study scope sees whether it has a dataset.
    attributor.collectStudy(request, studyProbes, errors);
    return request;
  }

  private ConsentGroupRequest consentGroup(
      TemplateRecord templateRecord,
      List<TemplateRecord> fileTypeRecords,
      TemplateErrors errors,
      List<ViolationProbe> probes) {
    ConsentGroupRequest consentGroup = new ConsentGroupRequest();
    Set<String> unconverted =
        apply(StudyTemplateV1Fields.CONSENT_GROUP, templateRecord, consentGroup, errors, probes);

    // The template contract requires access management even though the validator does not: the wire
    // default of "missing means controlled" is not a choice a producer should make by omission.
    if (consentGroup.getAccessManagement() == null
        && !unconverted.contains(StudyTemplateV1Fields.ACCESS_MANAGEMENT)) {
      errors.at(
          rowOf(templateRecord, StudyTemplateV1Fields.ACCESS_MANAGEMENT),
          TemplateColumns.FIELD,
          "accessManagement is required for consentGroup record '%s'"
              .formatted(templateRecord.recordId()));
    }

    if (fileTypeRecords != null) {
      consentGroup.setFileTypes(fileTypes(fileTypeRecords, errors, probes));
    }
    return consentGroup;
  }

  private List<FileTypeObject> fileTypes(
      List<TemplateRecord> records, TemplateErrors errors, List<ViolationProbe> probes) {
    List<FileTypeObject> fileTypes = new ArrayList<>();
    for (TemplateRecord fileTypeRecord : records) {
      FileTypeObject fileType = new FileTypeObject();
      Set<String> unconverted =
          apply(StudyTemplateV1Fields.FILE_TYPES, fileTypeRecord, fileType, errors, probes);
      if (fileType.getFileType() == null
          && !unconverted.contains(StudyTemplateV1Fields.FILE_TYPE_FIELD)) {
        errors.at(
            rowOf(fileTypeRecord, StudyTemplateV1Fields.FILE_TYPE_FIELD),
            TemplateColumns.FIELD,
            "fileType is required for fileType record '%s'".formatted(fileTypeRecord.recordId()));
      }
      fileTypes.add(fileType);
    }
    return fileTypes;
  }

  /**
   * Writes every assignment on {@code templateRecord} onto {@code target}, returning the fields
   * whose cells could not be converted. Those fields are absent from the DTO, so their derivative
   * registration violations are suppressed rather than reported alongside the conversion error.
   */
  private <T> Set<String> apply(
      Map<String, TemplateField<T>> catalog,
      TemplateRecord templateRecord,
      T target,
      TemplateErrors errors,
      List<ViolationProbe> probes) {
    Set<String> unconverted = new LinkedHashSet<>();
    templateRecord
        .assignments()
        .forEach(
            (name, assignments) -> {
              TemplateField<T> field = catalog.get(name);
              boolean converted =
                  field.multiValued()
                      ? writeItems(field, assignments, target, errors, probes)
                      : writeValue(field, assignments.getFirst(), target, errors, probes);
              if (!converted) {
                unconverted.add(name);
                // Every multi-valued field carrying a probe value converts as plain text, so only a
                // single-valued cell can both fail conversion and need its violation suppressed.
                if (!field.multiValued() && field.probeValue() != null) {
                  addProbe(
                      field,
                      assignments.getFirst().row(),
                      target,
                      Kind.SUPPRESSED,
                      field.probeValue(),
                      probes);
                }
              }
            });
    addAbsentProbes(catalog, templateRecord, target, probes);
    return unconverted;
  }

  /**
   * Probes for the fields this record never mentions. They have no row to report, and exist only so
   * that a violation asking for a field the file left out is not attributed to the cell that made
   * the field required; see {@link ViolationProbe.Kind#ABSENT}.
   */
  private <T> void addAbsentProbes(
      Map<String, TemplateField<T>> catalog,
      TemplateRecord templateRecord,
      T target,
      List<ViolationProbe> probes) {
    catalog.forEach(
        (name, field) -> {
          if (field.probeValue() != null && !templateRecord.assignments().containsKey(name)) {
            Object substitute =
                field.multiValued() ? List.of(field.probeValue()) : field.probeValue();
            addProbe(field, NO_ROW, target, Kind.ABSENT, substitute, probes);
          }
        });
  }

  private <T> boolean writeItems(
      TemplateField<T> field,
      List<TemplateAssignment> assignments,
      T target,
      TemplateErrors errors,
      List<ViolationProbe> probes) {
    List<Object> items = new ArrayList<>();
    List<Integer> itemRows = new ArrayList<>();
    for (TemplateAssignment assignment : assignments) {
      CellValue cell = field.converter().convert(field.name(), assignment.value());
      if (cell.rejected()) {
        errors.at(assignment.row(), TemplateColumns.VALUE, cell.errorMessage());
      } else {
        items.add(cell.value());
        itemRows.add(assignment.row());
      }
    }
    if (items.isEmpty()) {
      return false;
    }
    field.writer().accept(target, items);
    // One probe per item, each substituting only that item, so a per-item rule such as the data
    // custodian email check attributes to the row holding the offending item.
    if (field.probeValue() != null) {
      for (int index = 0; index < items.size(); index++) {
        List<Object> substituted = new ArrayList<>(items);
        substituted.set(index, field.probeValue());
        addProbe(field, itemRows.get(index), target, Kind.SET, substituted, probes);
      }
    }
    return true;
  }

  private <T> boolean writeValue(
      TemplateField<T> field,
      TemplateAssignment assignment,
      T target,
      TemplateErrors errors,
      List<ViolationProbe> probes) {
    // An empty cell means absent, so the field keeps its own required rule rather than gaining a
    // conversion error.
    if (!assignment.value().isEmpty()) {
      CellValue cell = field.converter().convert(field.name(), assignment.value());
      if (cell.rejected()) {
        errors.at(assignment.row(), TemplateColumns.VALUE, cell.errorMessage());
        return false;
      }
      field.writer().accept(target, cell.value());
    }
    addProbe(field, assignment.row(), target, kindOf(field, target), field.probeValue(), probes);
    return true;
  }

  private <T> void addProbe(
      TemplateField<T> field,
      int row,
      T target,
      Kind kind,
      Object substitute,
      List<ViolationProbe> probes) {
    if (field.probeValue() == null) {
      return;
    }
    Object assigned = field.reader().apply(target);
    probes.add(
        new ViolationProbe(
            row,
            kind,
            () -> field.writer().accept(target, substitute),
            () -> field.writer().accept(target, assigned)));
  }

  /** An empty cell leaves its field unset, which is what makes its own violation locatable. */
  private static <T> Kind kindOf(TemplateField<T> field, T target) {
    return field.reader().apply(target) == null ? Kind.UNSET : Kind.SET;
  }

  /**
   * Resolves a field name against the catalogue for this row's record type, empty when the row
   * cannot be used. This is the only place a record type is dispatched on, so an unknown one is
   * reported exactly once.
   */
  private Optional<Function<String, TemplateField<?>>> fieldsFor(
      TemplateRow row, TemplateErrors errors) {
    return switch (row.recordType()) {
      case STUDY -> fieldsIf(isUsableStudyRow(row, errors), StudyTemplateV1Fields.STUDY::get);
      case CONSENT_GROUP ->
          fieldsIf(isUsableConsentGroupRow(row, errors), StudyTemplateV1Fields.CONSENT_GROUP::get);
      case FILE_TYPE ->
          fieldsIf(isUsableFileTypeRow(row, errors), StudyTemplateV1Fields.FILE_TYPES::get);
      default -> {
        errors.at(
            row.row(), TemplateColumns.RECORD_TYPE, "Unknown record type: " + row.recordType());
        yield Optional.empty();
      }
    };
  }

  private static Optional<Function<String, TemplateField<?>>> fieldsIf(
      boolean usable, Function<String, TemplateField<?>> fields) {
    return usable ? Optional.of(fields) : Optional.empty();
  }

  private boolean isUsableStudyRow(TemplateRow row, TemplateErrors errors) {
    if (!STUDY_RECORD_ID.equals(row.recordId())) {
      errors.at(
          row.row(),
          TemplateColumns.RECORD_ID,
          "Study records must use recordId '%s'".formatted(STUDY_RECORD_ID));
      return false;
    }
    if (!row.parentRecordId().isEmpty()) {
      errors.at(
          row.row(), TemplateColumns.PARENT_RECORD_ID, "Study records must not set parentRecordId");
      return false;
    }
    return true;
  }

  private boolean isUsableConsentGroupRow(TemplateRow row, TemplateErrors errors) {
    if (row.recordId().isEmpty()) {
      errors.at(row.row(), TemplateColumns.RECORD_ID, "consentGroup records require a recordId");
      return false;
    }
    if (!STUDY_RECORD_ID.equals(row.parentRecordId())) {
      errors.at(
          row.row(),
          TemplateColumns.PARENT_RECORD_ID,
          "consentGroup record '%s' must set parentRecordId to '%s'"
              .formatted(row.recordId(), STUDY_RECORD_ID));
      return false;
    }
    return true;
  }

  private boolean isUsableFileTypeRow(TemplateRow row, TemplateErrors errors) {
    if (row.recordId().isEmpty()) {
      errors.at(row.row(), TemplateColumns.RECORD_ID, "fileType records require a recordId");
      return false;
    }
    if (row.parentRecordId().isEmpty()) {
      errors.at(
          row.row(),
          TemplateColumns.PARENT_RECORD_ID,
          "fileType record '%s' must name its consentGroup in parentRecordId"
              .formatted(row.recordId()));
      return false;
    }
    return true;
  }

  private void assign(
      RecordBuilder builder,
      Function<String, TemplateField<?>> fields,
      TemplateRow row,
      TemplateErrors errors) {
    if (!builder.parentRecordId.equals(row.parentRecordId())) {
      errors.at(
          row.row(),
          TemplateColumns.RECORD_ID,
          ("Duplicate %s recordId '%s': each recordId identifies one record and must name one"
                  + " parentRecordId")
              .formatted(builder.recordType, builder.recordId));
      return;
    }
    TemplateField<?> field = fields.apply(row.field());
    if (field == null) {
      errors.at(row.row(), TemplateColumns.FIELD, unusableFieldMessage(builder.recordType, row));
      return;
    }
    List<TemplateAssignment> assignments =
        builder.assignments.computeIfAbsent(row.field(), key -> new ArrayList<>());
    if (field.multiValued()
        ? isUsableItem(builder, row, assignments, errors)
        : isFirst(builder, row, assignments, errors)) {
      assignments.add(new TemplateAssignment(row.field(), row.value(), row.row()));
    }
  }

  private boolean isUsableItem(
      RecordBuilder builder,
      TemplateRow row,
      List<TemplateAssignment> assignments,
      TemplateErrors errors) {
    if (row.value().isEmpty()) {
      errors.at(
          row.row(),
          TemplateColumns.VALUE,
          "Empty item for %s field '%s'".formatted(builder.recordType, row.field()));
      return false;
    }
    if (assignments.stream().anyMatch(assignment -> assignment.value().equals(row.value()))) {
      errors.at(
          row.row(),
          TemplateColumns.VALUE,
          "Duplicate item for %s field '%s'".formatted(builder.recordType, row.field()));
      return false;
    }
    return true;
  }

  private boolean isFirst(
      RecordBuilder builder,
      TemplateRow row,
      List<TemplateAssignment> assignments,
      TemplateErrors errors) {
    if (assignments.isEmpty()) {
      return true;
    }
    errors.at(
        row.row(),
        TemplateColumns.FIELD,
        "Duplicate field '%s' for %s record '%s'"
            .formatted(row.field(), builder.recordType, builder.recordId));
    return false;
  }

  private static String unusableFieldMessage(String recordType, TemplateRow row) {
    return StudyTemplateV1Fields.isExcludedFromV1(recordType, row.field())
        ? "Unsupported %s field: %s. It must be set on the draft form after import."
            .formatted(recordType, row.field())
        : "Unknown %s field: %s".formatted(recordType, row.field());
  }

  private static int rowOf(TemplateRecord templateRecord, String field) {
    List<TemplateAssignment> assignments = templateRecord.assignments().get(field);
    return assignments == null ? templateRecord.firstRow() : assignments.getFirst().row();
  }

  private static List<RecordBuilder> buildersOfType(
      Map<RecordKey, RecordBuilder> builders, String recordType) {
    return builders.values().stream()
        .filter(builder -> builder.recordType.equals(recordType))
        .toList();
  }

  /** Identifies one logical record while rows are still being grouped. */
  private record RecordKey(String recordType, String recordId) {}

  /** Collects a record's assignments while rows are still being read. */
  private static final class RecordBuilder {

    private final String recordType;
    private final String recordId;
    private final String parentRecordId;
    private final int firstRow;
    private final Map<String, List<TemplateAssignment>> assignments = new LinkedHashMap<>();

    private RecordBuilder(String recordType, String recordId, TemplateRow declaringRow) {
      this.recordType = recordType;
      this.recordId = recordId;
      this.parentRecordId = declaringRow.parentRecordId();
      this.firstRow = declaringRow.row();
    }

    private TemplateRecord build() {
      Map<String, List<TemplateAssignment>> byField = new LinkedHashMap<>();
      assignments.forEach((field, items) -> byField.put(field, List.copyOf(items)));
      return new TemplateRecord(
          recordType, recordId, firstRow, Collections.unmodifiableMap(byField));
    }
  }
}
