package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

class StudyTemplateV1FixturesTest {

  private static final String FIXTURE_ROOT = "fixtures/study-template/v1/";
  private static final int MAX_BYTES = 5 * 1024 * 1024;
  private static final List<String> HEADERS =
      List.of("templateVersion", "recordType", "recordId", "parentRecordId", "field", "value");
  private static final List<String> VALID_FIXTURES =
      List.of("minimal-valid.csv", "multi-consent-group-valid.csv", "excel-export-valid.csv");
  private static final List<String> INVALID_FIXTURES =
      List.of(
          "duplicate-array-item.csv",
          "duplicate-field.csv",
          "duplicate-header.csv",
          "empty-file.csv",
          "field-values.csv",
          "orphan-file-type.csv",
          "semicolon-delimited.csv",
          "unknown-field.csv",
          "unsupported-version.csv");

  /**
   * Array-typed fields that the contract encodes as one row per item, so the same (recordType,
   * recordId, field) tuple may legitimately repeat. Every other field is single-valued. {@code
   * fileTypes} is an object array and stays a single JSON cell.
   */
  private static final Set<String> SCALAR_ARRAY_FIELDS =
      Set.of(
          "dataTypes",
          "dataCustodianEmail",
          "collaboratingSites",
          "nihICsSupportingStudy",
          "diseaseSpecificUse");

  private static final CSVFormat CSV_FORMAT =
      CSVFormat.RFC4180.builder().setIgnoreEmptyLines(true).get();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void validFixturesAreParseableAndVersioned() throws Exception {
    for (String name : VALID_FIXTURES) {
      byte[] bytes = readResource("valid/" + name);
      List<CSVRecord> records = parse(bytes);

      assertEquals(HEADERS, records.getFirst().toList(), name);
      assertTrue(records.size() > 1, name);

      Set<String> fieldKeys = new HashSet<>();
      for (CSVRecord csvRecord : records.subList(1, records.size())) {
        assertEquals(
            HEADERS.size(), csvRecord.size(), name + " row " + csvRecord.getRecordNumber());
        assertEquals("1", csvRecord.get(0), name + " row " + csvRecord.getRecordNumber());

        String value = csvRecord.get(5);
        assertTrue(fieldKeys.add(fieldKey(csvRecord)), name + " duplicates " + fieldKey(csvRecord));
        assertFalse(value.isEmpty(), name + " row " + csvRecord.getRecordNumber());
        assertFalse(
            value.startsWith("[") || value.startsWith("{"),
            name + " row " + csvRecord.getRecordNumber() + " encodes a value as JSON");
      }
    }
  }

  @Test
  void invalidFixturesAreParseableAndHaveStructuredErrors() throws Exception {
    for (String name : INVALID_FIXTURES) {
      byte[] bytes = readResource("invalid/" + name);
      List<CSVRecord> records = bytes.length == 0 ? List.of() : parse(bytes);
      if (!name.equals("empty-file.csv")) {
        assertTrue(records.size() > 1, name);
      }

      String manifestName = name.replace(".csv", ".errors.json");
      JsonNode errors = OBJECT_MAPPER.readTree(decodeUtf8(readResource("invalid/" + manifestName)));
      assertTrue(errors.isArray(), manifestName);
      assertFalse(errors.isEmpty(), manifestName);

      for (JsonNode error : errors) {
        JsonNode message = error.get("message");
        assertNotNull(message, manifestName);
        assertTrue(message.isTextual() && !message.asText().isBlank(), manifestName);

        if (error.has("row")) {
          int row = error.get("row").asInt();
          assertTrue(row > 0 && row <= records.size(), manifestName);
        }
        if (error.has("column")) {
          assertTrue(HEADERS.contains(error.get("column").asText()), manifestName);
        }
      }
    }
  }

  @Test
  void structuralInvaliditiesRemainIntentional() throws Exception {
    List<CSVRecord> duplicateHeader = parse(readResource("invalid/duplicate-header.csv"));
    List<String> duplicateHeaderValues = duplicateHeader.getFirst().toList();
    assertTrue(
        new HashSet<>(duplicateHeaderValues).size() < duplicateHeaderValues.size(),
        "duplicate-header.csv");

    List<CSVRecord> duplicateField = parse(readResource("invalid/duplicate-field.csv"));
    Set<String> fieldKeys = new HashSet<>();
    assertFalse(
        duplicateField.subList(1, duplicateField.size()).stream()
            .map(StudyTemplateV1FixturesTest::fieldKey)
            .allMatch(fieldKeys::add),
        "duplicate-field.csv");

    // A repeated scalar-array field is only invalid because the item value repeats, so the key that
    // catches it has to include the value.
    List<CSVRecord> duplicateItem = parse(readResource("invalid/duplicate-array-item.csv"));
    Set<String> itemKeys = new HashSet<>();
    assertFalse(
        duplicateItem.subList(1, duplicateItem.size()).stream()
            .map(StudyTemplateV1FixturesTest::fieldKey)
            .allMatch(itemKeys::add),
        "duplicate-array-item.csv");
    assertTrue(
        duplicateItem.subList(1, duplicateItem.size()).stream()
            .anyMatch(csvRecord -> csvRecord.get(5).isEmpty()),
        "duplicate-array-item.csv should also cover an empty item");

    List<CSVRecord> unsupportedVersion = parse(readResource("invalid/unsupported-version.csv"));
    assertTrue(
        unsupportedVersion.subList(1, unsupportedVersion.size()).stream()
            .noneMatch(csvRecord -> csvRecord.get(0).equals("1")),
        "unsupported-version.csv");

    assertEquals(0, readResource("invalid/empty-file.csv").length, "empty-file.csv");
  }

  @Test
  void utf8BomIsAcceptedAtTheBeginning() throws Exception {
    byte[] fixture = readResource("valid/minimal-valid.csv");
    byte[] withBom = new byte[fixture.length + 3];
    withBom[0] = (byte) 0xEF;
    withBom[1] = (byte) 0xBB;
    withBom[2] = (byte) 0xBF;
    System.arraycopy(fixture, 0, withBom, 3, fixture.length);

    assertEquals(HEADERS, parse(withBom).getFirst().toList());
  }

  @Test
  void excelStyleExportIsAccepted() throws Exception {
    byte[] excelExport = readResource("valid/excel-export-valid.csv");

    assertEquals(
        (byte) 0xEF, excelExport[0], "excel-export-valid.csv should start with a UTF-8 BOM");
    assertEquals((byte) 0xBB, excelExport[1]);
    assertEquals((byte) 0xBF, excelExport[2]);
    assertTrue(
        decodeUtf8(excelExport).contains("\r\n"),
        "excel-export-valid.csv should use CRLF record separators");

    // BOM and CRLF are the only differences from the plain minimal fixture.
    List<List<String>> excelRows = parse(excelExport).stream().map(CSVRecord::toList).toList();
    List<List<String>> minimalRows =
        parse(readResource("valid/minimal-valid.csv")).stream().map(CSVRecord::toList).toList();
    assertEquals(minimalRows, excelRows);
  }

  @Test
  void semicolonDelimitedExportIsDetectableAsSuch() throws Exception {
    List<CSVRecord> records = parse(readResource("invalid/semicolon-delimited.csv"));

    // Read as comma-separated values, a European-locale export collapses to one column per row.
    // That is the signal the parser uses to report the delimiter instead of a missing header.
    List<String> header = records.getFirst().toList();
    assertEquals(1, header.size(), "semicolon-delimited.csv");
    assertTrue(header.getFirst().contains(";"), "semicolon-delimited.csv");
    assertNotEquals(HEADERS, header, "semicolon-delimited.csv");
  }

  @Test
  void noFixtureEncodesAValueAsJson() throws Exception {
    // v1 expresses structured wire values as rows, never as an embedded document. fileTypes, the
    // one array of objects, is its own record type.
    for (String name : VALID_FIXTURES) {
      assertNoJsonValues("valid/" + name);
    }
    for (String name : INVALID_FIXTURES) {
      assertNoJsonValues("invalid/" + name);
    }
  }

  @Test
  void fileTypeRecordsReferenceADeclaredConsentGroup() throws Exception {
    for (String name : VALID_FIXTURES) {
      List<CSVRecord> records = parse(readResource("valid/" + name));
      Set<String> consentGroupIds = recordIdsOfType(records, "consentGroup");

      records.stream()
          .filter(csvRecord -> csvRecord.get(1).equals("fileType"))
          .forEach(
              csvRecord ->
                  assertTrue(
                      consentGroupIds.contains(csvRecord.get(3)),
                      name + " row " + csvRecord.getRecordNumber() + " has an orphan parent"));
    }

    List<CSVRecord> orphan = parse(readResource("invalid/orphan-file-type.csv"));
    Set<String> consentGroupIds = recordIdsOfType(orphan, "consentGroup");
    assertTrue(
        orphan.stream()
            .filter(csvRecord -> csvRecord.get(1).equals("fileType"))
            .anyMatch(csvRecord -> !consentGroupIds.contains(csvRecord.get(3))),
        "orphan-file-type.csv");
  }

  private static void assertNoJsonValues(String relativePath) throws IOException {
    byte[] bytes = readResource(relativePath);
    if (bytes.length == 0) {
      return;
    }
    for (CSVRecord csvRecord : parse(bytes)) {
      if (csvRecord.size() < HEADERS.size()) {
        continue;
      }
      String value = csvRecord.get(5);
      assertFalse(
          value.startsWith("[") || value.startsWith("{"),
          relativePath + " row " + csvRecord.getRecordNumber() + " encodes a value as JSON");
    }
  }

  private static Set<String> recordIdsOfType(List<CSVRecord> records, String recordType) {
    return records.stream()
        .filter(csvRecord -> csvRecord.get(1).equals(recordType))
        .map(csvRecord -> csvRecord.get(2))
        .collect(Collectors.toSet());
  }

  private static String fieldKey(CSVRecord csvRecord) {
    String field = csvRecord.get(4);
    String key = csvRecord.get(1) + '\0' + csvRecord.get(2) + '\0' + field;
    return SCALAR_ARRAY_FIELDS.contains(field) ? key + '\0' + csvRecord.get(5) : key;
  }

  private static byte[] readResource(String relativePath) throws IOException {
    String resourcePath = FIXTURE_ROOT + relativePath;
    try (InputStream input =
        StudyTemplateV1FixturesTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
      assertNotNull(input, resourcePath);
      byte[] bytes = input.readAllBytes();
      assertTrue(bytes.length <= MAX_BYTES, resourcePath);
      return bytes;
    }
  }

  private static List<CSVRecord> parse(byte[] bytes) throws IOException {
    String csv = decodeUtf8(bytes);
    if (csv.startsWith("\uFEFF")) {
      csv = csv.substring(1);
    }
    try (CSVParser parser = CSVParser.parse(csv, CSV_FORMAT)) {
      return parser.getRecords();
    }
  }

  private static String decodeUtf8(byte[] bytes) throws CharacterCodingException {
    return StandardCharsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString();
  }
}
