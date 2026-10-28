package org.broadinstitute.consent.http.service.showcase;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Reads a public Data Showcase lookup request and checks it against the contract before any data is
 * read. The body must be one strict JSON object in valid UTF-8 with no repeated field. Every field
 * must belong to the request's mode, and every value must have the agreed type and stay within
 * {@link ShowcaseLookupLimits}.
 *
 * <p>Only the {@code identifiers} mode exists so far; {@code ranking}, {@code all} and {@code
 * stats} follow in later pull requests and are unknown modes until then.
 */
public final class ShowcaseLookupParser {

  static final String NOT_A_JSON_OBJECT = "Request body must be one JSON object";

  private static final String MODE = "mode";
  private static final String SECTION = "section";
  private static final String IDENTIFIERS = "identifiers";
  private static final Set<String> IDENTIFIERS_FIELDS = Set.of(MODE, SECTION, IDENTIFIERS);

  private static final TypeAdapter<JsonElement> ELEMENT_ADAPTER =
      GsonUtil.getInstance().getAdapter(JsonElement.class);

  private ShowcaseLookupParser() {}

  /**
   * @throws ShowcaseLookupException when the request breaks the contract
   */
  public static ShowcaseIdentifiersLookup parse(byte[] body) {
    Map<String, JsonElement> fields = readFields(body);
    String mode = stringField(fields, MODE);
    if (!IDENTIFIERS.equals(mode)) {
      throw new ShowcaseLookupException("Unknown mode \"%s\"".formatted(mode));
    }
    rejectUnrecognizedFields(fields, IDENTIFIERS_FIELDS, mode);
    return new ShowcaseIdentifiersLookup(section(fields, mode), identifiers(fields));
  }

  /** The top-level fields in the order sent, with each value read as a tree. */
  private static Map<String, JsonElement> readFields(byte[] body) {
    try (JsonReader reader = new JsonReader(strictUtf8(body))) {
      reader.setStrictness(Strictness.STRICT);
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
        throw new ShowcaseLookupException(NOT_A_JSON_OBJECT);
      }
      Map<String, JsonElement> fields = readObject(reader);
      if (reader.peek() != JsonToken.END_DOCUMENT) {
        throw new ShowcaseLookupException(NOT_A_JSON_OBJECT);
      }
      return fields;
    } catch (IOException | IllegalStateException | JsonParseException e) {
      throw new ShowcaseLookupException(NOT_A_JSON_OBJECT);
    }
  }

  private static Map<String, JsonElement> readObject(JsonReader reader) throws IOException {
    Map<String, JsonElement> fields = new LinkedHashMap<>();
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (fields.containsKey(name)) {
        throw new ShowcaseLookupException("Duplicate field \"%s\"".formatted(name));
      }
      fields.put(name, ELEMENT_ADAPTER.read(reader));
    }
    reader.endObject();
    return fields;
  }

  /** Malformed UTF-8 fails the read instead of turning into replacement characters. */
  private static Reader strictUtf8(byte[] body) {
    CharsetDecoder decoder =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    return new InputStreamReader(new ByteArrayInputStream(body), decoder);
  }

  private static void rejectUnrecognizedFields(
      Map<String, JsonElement> fields, Set<String> allowed, String mode) {
    fields.keySet().stream()
        .filter(name -> !allowed.contains(name))
        .findFirst()
        .ifPresent(
            name -> {
              throw new ShowcaseLookupException(
                  "Unrecognized field \"%s\" for mode \"%s\"".formatted(name, mode));
            });
  }

  private static String stringField(Map<String, JsonElement> fields, String name) {
    JsonElement value = requiredField(fields, name);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new ShowcaseLookupException("Field \"%s\" must be a string".formatted(name));
    }
    return value.getAsString();
  }

  private static JsonElement requiredField(Map<String, JsonElement> fields, String name) {
    JsonElement value = fields.get(name);
    if (value == null) {
      throw new ShowcaseLookupException("Missing field \"%s\"".formatted(name));
    }
    return value;
  }

  private static ShowcaseSection section(Map<String, JsonElement> fields, String mode) {
    String key = stringField(fields, SECTION);
    return ShowcaseSection.fromKey(key)
        .orElseThrow(
            () ->
                new ShowcaseLookupException(
                    "Unknown section \"%s\" for mode \"%s\"".formatted(key, mode)));
  }

  /**
   * Counts the submitted entries before duplicates are removed, so that repeats cannot get past the
   * cap.
   */
  private static List<String> identifiers(Map<String, JsonElement> fields) {
    List<String> entries = stringArray(fields, IDENTIFIERS);
    if (entries.size() > ShowcaseLookupLimits.MAX_IDENTIFIERS) {
      throw new ShowcaseLookupException(
          "Field \"%s\" has more than %d entries"
              .formatted(IDENTIFIERS, ShowcaseLookupLimits.MAX_IDENTIFIERS));
    }
    Set<String> unique = new LinkedHashSet<>();
    for (int index = 0; index < entries.size(); index++) {
      unique.add(canonicalIdentifier(entries.get(index), index));
    }
    return List.copyOf(unique);
  }

  private static List<String> stringArray(Map<String, JsonElement> fields, String name) {
    JsonElement value = requiredField(fields, name);
    String typeError = "Field \"%s\" must be an array of strings".formatted(name);
    if (!value.isJsonArray()) {
      throw new ShowcaseLookupException(typeError);
    }
    return value.getAsJsonArray().asList().stream()
        .map(
            entry -> {
              if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new ShowcaseLookupException(typeError);
              }
              return entry.getAsString();
            })
        .toList();
  }

  /**
   * Accepts only the form that the lookup returns, so a configured identifier always matches its
   * result: {@code DUOS-123} is rejected because it comes back as {@code DUOS-000123}.
   */
  private static String canonicalIdentifier(String identifier, int index) {
    if (identifier.length() > ShowcaseLookupLimits.MAX_IDENTIFIER_LENGTH) {
      throw new ShowcaseLookupException(
          "Identifier at index %d is longer than %d characters"
              .formatted(index, ShowcaseLookupLimits.MAX_IDENTIFIER_LENGTH));
    }
    Integer alias = alias(identifier);
    String canonical = Dataset.parseAliasToIdentifier(alias);
    if (!canonical.equals(identifier)) {
      throw new ShowcaseLookupException(
          "Identifier \"%s\" is not canonical; use \"%s\"".formatted(identifier, canonical));
    }
    return identifier;
  }

  private static Integer alias(String identifier) {
    try {
      Integer alias = Dataset.parseIdentifierToAlias(identifier);
      if (alias >= 0) {
        return alias;
      }
    } catch (IllegalArgumentException e) {
      // Reported below with the same message as a negative alias.
    }
    throw new ShowcaseLookupException(
        "Identifier \"%s\" is not a DUOS identifier".formatted(identifier));
  }
}
