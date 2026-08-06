package org.broadinstitute.consent.http.models.dto.registration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
      List.of("minimal-valid.csv", "multi-consent-group-valid.csv");
  private static final List<String> INVALID_FIXTURES =
      List.of(
          "duplicate-field.csv",
          "duplicate-header.csv",
          "empty-file.csv",
          "field-values.csv",
          "unknown-field.csv",
          "unsupported-version.csv");

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

        String fieldKey = csvRecord.get(1) + '\0' + csvRecord.get(2) + '\0' + csvRecord.get(4);
        assertTrue(fieldKeys.add(fieldKey), name + " duplicates " + fieldKey);

        String value = csvRecord.get(5);
        if (value.startsWith("[") || value.startsWith("{")) {
          assertDoesNotThrow(
              () -> OBJECT_MAPPER.readTree(value), name + " row " + csvRecord.getRecordNumber());
        }
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
            .map(csvRecord -> csvRecord.get(1) + '\0' + csvRecord.get(2) + '\0' + csvRecord.get(4))
            .allMatch(fieldKeys::add),
        "duplicate-field.csv");

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
