package org.broadinstitute.consent.http.service.studytemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyTemplateValidationResult;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationError;

/**
 * Validates an uploaded study template and maps it to a {@link StudyRegistrationRequest}. Nothing
 * here reads or writes the database, and no uploaded content is logged.
 *
 * <p>Validation runs in three stages, each of which stops the next when it reports anything. File
 * and header problems come first, because rows cannot be trusted without them; record-model
 * problems come second, because a DTO built from an untrustworthy record model would produce
 * misleading business violations; cell conversion and the ordinary registration validator run last
 * and report together.
 */
public class StudyTemplateValidationService {

  static final int MAX_TEMPLATE_BYTES = 5 * 1024 * 1024;
  static final int MAX_ERRORS = 100;

  private static final char BOM = '\uFEFF';
  private static final CSVFormat CSV_FORMAT =
      CSVFormat.RFC4180.builder().setIgnoreEmptyLines(true).get();

  /** What a spreadsheet writes under a non-US locale, reported by name rather than guessed at. */
  private static final List<Character> FOREIGN_DELIMITERS = List.of(';', '\t');

  private final Map<String, StudyTemplateParser> parsersByVersion;

  public StudyTemplateValidationService() {
    this(List.of(new StudyTemplateV1Parser()));
  }

  StudyTemplateValidationService(List<StudyTemplateParser> parsers) {
    this.parsersByVersion =
        parsers.stream()
            .collect(
                Collectors.toMap(
                    StudyTemplateParser::majorVersion,
                    Function.identity(),
                    (first, _) -> first,
                    LinkedHashMap::new));
  }

  public StudyTemplateValidationResult validate(InputStream content) {
    TemplateErrors errors = new TemplateErrors();

    String csv = readTemplate(content, errors);
    List<CSVRecord> records = csv == null ? List.of() : parseCsv(csv, errors);
    if (records.isEmpty()) {
      // Whichever step produced no records already recorded why.
      return failed(errors);
    }
    validateHeader(records.getFirst(), errors);
    if (!errors.isEmpty()) {
      return failed(errors);
    }

    List<TemplateRow> rows = dataRows(records, errors);
    StudyTemplateParser parser = parserFor(rows, errors);
    if (!errors.isEmpty()) {
      return failed(errors);
    }

    ParsedStudyTemplate template = parser.parse(rows, errors);
    if (!errors.isEmpty()) {
      return failed(errors);
    }

    StudyRegistrationRequest registration = parser.validate(template, errors);
    return errors.isEmpty() ? StudyTemplateValidationResult.valid(registration) : failed(errors);
  }

  /** Returns the template text with any leading BOM removed, or {@code null} when unusable. */
  private static String readTemplate(InputStream content, TemplateErrors errors) {
    byte[] bytes;
    try {
      bytes = content.readNBytes(MAX_TEMPLATE_BYTES + 1);
    } catch (IOException _) {
      errors.message("Template file could not be read");
      return null;
    }
    if (bytes.length > MAX_TEMPLATE_BYTES) {
      errors.message("Template file must be no larger than 5 MiB");
      return null;
    }
    String csv;
    try {
      csv =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(bytes))
              .toString();
    } catch (CharacterCodingException _) {
      errors.message("Template file must be UTF-8 encoded");
      return null;
    }
    if (!csv.isEmpty() && csv.charAt(0) == BOM) {
      csv = csv.substring(1);
    }
    if (csv.isBlank()) {
      errors.message("Template file is empty");
      return null;
    }
    return csv;
  }

  private static List<CSVRecord> parseCsv(String csv, TemplateErrors errors) {
    try (CSVParser parser = CSVParser.parse(csv, CSV_FORMAT)) {
      return parser.getRecords();
    } catch (IOException | UncheckedIOException | IllegalStateException _) {
      // The parser's own message quotes the offending content, which must not be echoed back.
      errors.message("Template file is not valid CSV");
      return List.of();
    }
  }

  private static void validateHeader(CSVRecord header, TemplateErrors errors) {
    List<String> columns = header.toList();
    if (columns.size() == 1) {
      Character delimiter =
          FOREIGN_DELIMITERS.stream()
              .filter(candidate -> columns.getFirst().indexOf(candidate) >= 0)
              .findFirst()
              .orElse(null);
      if (delimiter != null) {
        errors.at(
            (int) header.getRecordNumber(),
            ("Template must be comma-delimited. Detected '%s' as the column separator. Re-export the"
                    + " file as comma-separated values.")
                .formatted(delimiter));
        return;
      }
    }

    Set<String> seen = new LinkedHashSet<>();
    Set<String> duplicates = new LinkedHashSet<>();
    columns.forEach(
        column -> {
          if (!seen.add(column)) {
            duplicates.add(column);
          }
        });
    if (!duplicates.isEmpty()) {
      duplicates.forEach(
          column ->
              errors.at(
                  (int) header.getRecordNumber(),
                  TemplateColumns.HEADERS.contains(column) ? column : null,
                  "Duplicate header: " + column));
      return;
    }

    if (!TemplateColumns.HEADERS.equals(columns)) {
      errors.at(
          (int) header.getRecordNumber(),
          "Template header must be exactly: " + String.join(",", TemplateColumns.HEADERS));
    }
  }

  private static List<TemplateRow> dataRows(List<CSVRecord> records, TemplateErrors errors) {
    List<TemplateRow> rows = new ArrayList<>();
    for (CSVRecord csvRecord : records.subList(1, records.size())) {
      int row = (int) csvRecord.getRecordNumber();
      if (csvRecord.size() != TemplateColumns.HEADERS.size()) {
        errors.at(
            row,
            "Row must have %d columns but has %d"
                .formatted(TemplateColumns.HEADERS.size(), csvRecord.size()));
        continue;
      }
      rows.add(
          new TemplateRow(
              row,
              csvRecord.get(0),
              csvRecord.get(1),
              csvRecord.get(2),
              csvRecord.get(3),
              csvRecord.get(4),
              csvRecord.get(5)));
    }
    if (rows.isEmpty() && errors.isEmpty()) {
      errors.message("Template file has no data rows");
    }
    return rows;
  }

  /**
   * Resolves the parser for the version the file declares. Every row carries the version, so a row
   * that names an unsupported or different version is reported rather than parsed by the wrong
   * parser.
   */
  private StudyTemplateParser parserFor(List<TemplateRow> rows, TemplateErrors errors) {
    StudyTemplateParser dispatched = null;
    String fileVersion = null;
    for (TemplateRow row : rows) {
      String version = row.templateVersion();
      StudyTemplateParser parser = parsersByVersion.get(version);
      if (version.isEmpty()) {
        errors.at(row.row(), TemplateColumns.TEMPLATE_VERSION, "Template version is required");
      } else if (parser == null) {
        errors.at(
            row.row(),
            TemplateColumns.TEMPLATE_VERSION,
            "Unsupported template version: " + version);
      } else if (dispatched == null) {
        dispatched = parser;
        fileVersion = version;
      } else if (!version.equals(fileVersion)) {
        errors.at(
            row.row(),
            TemplateColumns.TEMPLATE_VERSION,
            "Template version %s does not match version %s used earlier in the file"
                .formatted(version, fileVersion));
      }
    }
    return dispatched;
  }

  private static StudyTemplateValidationResult failed(TemplateErrors errors) {
    List<TemplateValidationError> all = errors.toList();
    if (all.size() <= MAX_ERRORS) {
      return StudyTemplateValidationResult.invalid(all, false);
    }
    List<TemplateValidationError> capped = new ArrayList<>(all.subList(0, MAX_ERRORS));
    capped.add(
        TemplateValidationError.of(
            "Only the first %d errors are reported; %d further errors were omitted"
                .formatted(MAX_ERRORS, all.size() - MAX_ERRORS)));
    return StudyTemplateValidationResult.invalid(capped, true);
  }
}
