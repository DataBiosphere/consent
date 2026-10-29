package org.broadinstitute.consent.http.service.showcase;

import static org.broadinstitute.consent.http.service.showcase.ShowcaseLookupException.echoed;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.Map;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Reads a lookup request body as one strict JSON object in valid UTF-8. A field name that repeats
 * at any level is refused, so the BFF proxy and Consent cannot read different values from one body.
 * Nesting is capped, because the deepest valid request is a scope object holding an array of
 * strings.
 */
final class ShowcaseJsonBody {

  static final String NOT_A_JSON_OBJECT = "Request body must be one JSON object";
  static final String TOO_DEEP = "Request body is nested too deeply";

  /** The top object, the scope object, the scope's value array and the string in it. */
  private static final int MAX_DEPTH = 4;

  private static final TypeAdapter<JsonElement> ELEMENT_ADAPTER =
      GsonUtil.getInstance().getAdapter(JsonElement.class);

  private ShowcaseJsonBody() {}

  /** The top-level fields in the order sent. */
  static Map<String, JsonElement> read(byte[] body) {
    if (startsWithByteOrderMark(body)) {
      throw new ShowcaseLookupException(NOT_A_JSON_OBJECT);
    }
    try (JsonReader reader = new JsonReader(strictUtf8(body))) {
      reader.setStrictness(Strictness.STRICT);
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
        throw new ShowcaseLookupException(NOT_A_JSON_OBJECT);
      }
      Map<String, JsonElement> fields = readObject(reader, 1).asMap();
      if (reader.peek() != JsonToken.END_DOCUMENT) {
        throw new ShowcaseLookupException(NOT_A_JSON_OBJECT);
      }
      return new LinkedHashMap<>(fields);
    } catch (IOException | IllegalStateException | JsonParseException e) {
      throw new ShowcaseLookupException(NOT_A_JSON_OBJECT);
    }
  }

  private static JsonElement readValue(JsonReader reader, int depth) throws IOException {
    return switch (reader.peek()) {
      case BEGIN_OBJECT -> readObject(reader, depth);
      case BEGIN_ARRAY -> readArray(reader, depth);
      default -> ELEMENT_ADAPTER.read(reader);
    };
  }

  private static JsonObject readObject(JsonReader reader, int depth) throws IOException {
    checkDepth(depth);
    JsonObject object = new JsonObject();
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (object.has(name)) {
        throw new ShowcaseLookupException("Duplicate field \"%s\"".formatted(echoed(name)));
      }
      object.add(name, readValue(reader, depth + 1));
    }
    reader.endObject();
    return object;
  }

  private static JsonArray readArray(JsonReader reader, int depth) throws IOException {
    checkDepth(depth);
    JsonArray array = new JsonArray();
    reader.beginArray();
    while (reader.hasNext()) {
      array.add(readValue(reader, depth + 1));
    }
    reader.endArray();
    return array;
  }

  /** A container at the maximum depth could only hold a value one level too deep. */
  private static void checkDepth(int depth) {
    if (depth >= MAX_DEPTH) {
      throw new ShowcaseLookupException(TOO_DEEP);
    }
  }

  /**
   * Gson skips a leading byte order mark even when strict, but a strict reader such as {@code
   * JSON.parse} in the BFF refuses it. Refuse it here too, so both sides accept the same bodies.
   */
  private static boolean startsWithByteOrderMark(byte[] body) {
    return body.length >= 3
        && body[0] == (byte) 0xEF
        && body[1] == (byte) 0xBB
        && body[2] == (byte) 0xBF;
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
}
