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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
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

  public static final int MAX_TEMPLATE_BYTES = 5 * 1024 * 1024;

  /** The limit is enforced here alone; the resource turns this into a 413. */
  public static final String TOO_LARGE_MESSAGE = "Template file must be no larger than 5 MiB";

  static final int MAX_ERRORS = 100;

  /**
   * Twenty times the largest study the contract sizes for, and far below what 5 MiB of the shortest
   * possible rows holds. Bounding the file alone does not bound the row model built from it.
   */
  static final int MAX_DATA_ROWS = 50_000;

  private static final char BOM = '\uFEFF';
  private static final char NUL = '\u0000';

  /**
   * Blank lines are kept rather than skipped so that a reported row is the physical line it came
   * from; {@link #isBlank} drops them, along with the row of delimiters a spreadsheet writes for a
   * blank line inside its used range.
   */
  private static final CSVFormat CSV_FORMAT =
      CSVFormat.RFC4180.builder().setIgnoreEmptyLines(false).get();

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
    if (csv == null) {
      return failed(errors);
    }

    List<TemplateRow> rows = readRows(csv, errors);
    if (!errors.isEmpty()) {
      return failed(errors);
    }

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

  /**
   * Returns the template text with any leading BOM removed, or {@code null} when unusable. Reads
   * one byte beyond the limit, which is all it takes to know the file exceeds it.
   */
  private static String readTemplate(InputStream content, TemplateErrors errors) {
    byte[] bytes;
    try {
      bytes = content.readNBytes(MAX_TEMPLATE_BYTES + 1);
    } catch (IOException _) {
      errors.message("Template file could not be read");
      return null;
    }
    if (bytes.length > MAX_TEMPLATE_BYTES) {
      throw new TemplateTooLargeException(TOO_LARGE_MESSAGE);
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

  /**
   * Reads the data rows of a template. The file is streamed rather than collected, so a large one
   * costs a row list rather than a parsed copy of itself as well.
   */
  private static List<TemplateRow> readRows(String csv, TemplateErrors errors) {
    ScannedTemplate scanned;
    try (CSVParser parser = CSVParser.parse(csv, CSV_FORMAT)) {
      scanned = scan(parser);
    } catch (IOException | UncheckedIOException | IllegalStateException _) {
      // The parser's own message quotes the offending content, which must not be echoed back.
      errors.message("Template file is not valid CSV");
      return List.of();
    }
    return report(scanned, errors);
  }

  /** One pass over the records: their lines, their NUL cells, the header, and the data rows. */
  private static ScannedTemplate scan(CSVParser parser) {
    TemplateErrors nulErrors = new TemplateErrors();
    TemplateErrors shapeErrors = new TemplateErrors();
    List<TemplateRow> rows = new ArrayList<>();
    CSVRecord header = null;
    int headerRow = 0;
    boolean overRowLimit = false;

    long lastLine = 0;
    for (CSVRecord csvRecord : parser) {
      // The record's own start line, so a reported row is the line the producer sees in their
      // spreadsheet even when blank lines or multi-line quoted values precede it.
      int row = (int) lastLine + 1;
      lastLine = parser.getCurrentLineNumber();
      if (!isBlank(csvRecord)) {
        rejectNulCharacters(csvRecord, row, nulErrors);
        if (header == null) {
          header = csvRecord;
          headerRow = row;
        } else if (rows.size() < MAX_DATA_ROWS) {
          dataRow(csvRecord, row, shapeErrors).ifPresent(rows::add);
        } else {
          overRowLimit = true;
          break;
        }
      }
    }
    return new ScannedTemplate(header, headerRow, rows, nulErrors, shapeErrors, overRowLimit);
  }

  /**
   * Reports the first stage of the scan that found anything — the row limit, then NUL characters,
   * then the header, then row shape — and returns the rows only when none did.
   */
  private static List<TemplateRow> report(ScannedTemplate scanned, TemplateErrors errors) {
    if (scanned.overRowLimit()) {
      errors.message("Template must have no more than %,d data rows".formatted(MAX_DATA_ROWS));
      return List.of();
    }
    if (!scanned.nulErrors().isEmpty()) {
      errors.merge(scanned.nulErrors());
      return List.of();
    }
    if (scanned.header() == null) {
      errors.message("Template file is empty");
      return List.of();
    }
    validateHeader(scanned.header(), scanned.headerRow(), errors);
    if (!errors.isEmpty()) {
      return List.of();
    }
    errors.merge(scanned.shapeErrors());
    if (scanned.rows().isEmpty() && errors.isEmpty()) {
      errors.message("Template file has no data rows");
    }
    return scanned.rows();
  }

  /**
   * Whether every cell of the record is empty. Spreadsheets write a row of delimiters for a blank
   * line inside their used range, which the contract ignores the same way it ignores a blank line.
   */
  private static boolean isBlank(CSVRecord csvRecord) {
    for (int column = 0; column < csvRecord.size(); column++) {
      if (!csvRecord.get(column).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * NUL passes both a strict UTF-8 decode and {@code isBlank}, so without this it would reach a
   * registration string and only fail once the draft is written, where Postgres rejects it inside a
   * {@code jsonb} value. Rejecting it here tells the producer which cell to fix instead of
   * stripping bytes out of their study metadata downstream.
   */
  private static void rejectNulCharacters(CSVRecord csvRecord, int row, TemplateErrors errors) {
    for (int column = 0; column < csvRecord.size(); column++) {
      if (csvRecord.get(column).indexOf(NUL) >= 0) {
        errors.at(row, columnName(column), "Template must not contain a NUL character");
      }
    }
  }

  /** The header this column falls under, or null for a cell beyond the declared columns. */
  private static String columnName(int column) {
    return column < TemplateColumns.HEADERS.size() ? TemplateColumns.HEADERS.get(column) : null;
  }

  private static void validateHeader(CSVRecord header, int row, TemplateErrors errors) {
    List<String> columns = header.toList();
    if (columns.size() == 1) {
      Character delimiter =
          FOREIGN_DELIMITERS.stream()
              .filter(candidate -> columns.getFirst().indexOf(candidate) >= 0)
              .findFirst()
              .orElse(null);
      if (delimiter != null) {
        errors.at(
            row,
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
                  row,
                  TemplateColumns.HEADERS.contains(column) ? column : null,
                  "Duplicate header: " + column));
      return;
    }

    if (!TemplateColumns.HEADERS.equals(columns)) {
      errors.at(
          row, "Template header must be exactly: " + String.join(",", TemplateColumns.HEADERS));
    }
  }

  private static Optional<TemplateRow> dataRow(
      CSVRecord csvRecord, int row, TemplateErrors errors) {
    if (csvRecord.size() != TemplateColumns.HEADERS.size()) {
      errors.at(
          row,
          "Row must have %d columns but has %d"
              .formatted(TemplateColumns.HEADERS.size(), csvRecord.size()));
      return Optional.empty();
    }
    return Optional.of(
        new TemplateRow(
            row,
            csvRecord.get(0),
            csvRecord.get(1),
            csvRecord.get(2),
            csvRecord.get(3),
            csvRecord.get(4),
            csvRecord.get(5)));
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

  /** What one pass over the records collected, before any stage decides what to report. */
  private record ScannedTemplate(
      CSVRecord header,
      int headerRow,
      List<TemplateRow> rows,
      TemplateErrors nulErrors,
      TemplateErrors shapeErrors,
      boolean overRowLimit) {}

  private static StudyTemplateValidationResult failed(TemplateErrors errors) {
    List<TemplateValidationError> all = errors.toList();
    if (errors.count() <= MAX_ERRORS) {
      return StudyTemplateValidationResult.invalid(all, false);
    }
    List<TemplateValidationError> capped = new ArrayList<>(all.subList(0, MAX_ERRORS));
    capped.add(
        TemplateValidationError.of(
            "Only the first %d errors are reported; %d further errors were omitted"
                .formatted(MAX_ERRORS, errors.count() - MAX_ERRORS)));
    return StudyTemplateValidationResult.invalid(capped, true);
  }
}
