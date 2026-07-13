package org.broadinstitute.consent.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

public abstract class AbstractTestHelper {

  public static final Instant FIXED_INSTANT = Instant.parse("2025-01-01T00:00:00Z");
  public static final Date FIXED_DATE = Date.from(FIXED_INSTANT);
  public static final Timestamp FIXED_TIMESTAMP = Timestamp.from(FIXED_INSTANT);

  /**
   * This configuration property is set in pom.xml and used to disable test containers for
   * integration tests.
   *
   * @return "true"|"false"
   */
  public static boolean enableTestContainers() {
    String defaultProp = "true";
    return Boolean.parseBoolean(System.getProperty("enableTestContainers", defaultProp));
  }

  public static String randomAlphabetic(int length) {
    return RandomStringUtils.secureStrong().nextAlphabetic(length);
  }

  public static String randomAlphanumeric(int length) {
    return RandomStringUtils.secureStrong().nextAlphanumeric(length);
  }

  public static int randomInt(int startInclusive, int endExclusive) {
    return RandomUtils.secureStrong().randomInt(startInclusive, endExclusive);
  }

  public static boolean randomBoolean() {
    return RandomUtils.secureStrong().randomBoolean();
  }

  public static int nextInt() {
    return RandomUtils.secure().randomInt();
  }

  public JsonArray getJsonArrayFromStreamingOutput(StreamingOutput output) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String jsonString = baos.toString(StandardCharsets.UTF_8);
    return JsonParser.parseString(jsonString).getAsJsonArray();
  }
}
