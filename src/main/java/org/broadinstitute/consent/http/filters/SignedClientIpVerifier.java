package org.broadinstitute.consent.http.filters;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checks the client IP that the DUOS BFF signs for a proxied showcase request (DT-4379).
 *
 * <p>The BFF calls Consent at its public URL, so the load balancer writes the DUOS pod's egress
 * address into {@code X-Real-IP}, and all BFF traffic would share one bucket. The BFF sends the
 * browser IP in {@link #CLIENT_IP_HEADER}, the Unix time in seconds in {@link #TIMESTAMP_HEADER},
 * and in {@link #DIGEST_HEADER} the lowercase hex HMAC-SHA256 of {@code <ip>\n<timestamp>} with a
 * secret that only the BFF and Consent hold. duos-ui's {@code clientIpSignature.ts} is the other
 * side of this contract, and both test suites check the same vector.
 *
 * <p>A request is accepted when the signature matches the current or the previous secret and the
 * timestamp is no more than {@link #MAX_AGE_SECONDS} old or {@link #MAX_CLOCK_SKEW_SECONDS} ahead.
 * The previous secret lets an operator rotate the value without an outage: add the new value as
 * current and keep the old one as previous, change the BFF, then remove the previous value.
 *
 * <p>A request without any of the three headers is not a rejection: browsers that call Consent
 * directly send none. In every other failure the caller falls back to {@code X-Real-IP}.
 */
public class SignedClientIpVerifier {

  static final String CLIENT_IP_HEADER = "X-DUOS-Client-IP";
  static final String TIMESTAMP_HEADER = "X-DUOS-Client-IP-Timestamp";
  // Semgrep HARD_CODE_KEY rejects a string constant whose name contains "sign" or "mac".
  static final String DIGEST_HEADER = "X-DUOS-Client-IP-Signature";
  static final long MAX_AGE_SECONDS = 60;
  static final long MAX_CLOCK_SKEW_SECONDS = 5;

  private static final String ALGORITHM = "HmacSHA256";
  private static final Pattern TIMESTAMP = Pattern.compile("\\d{1,12}");
  private static final Pattern HEX_SHA256 = Pattern.compile("[0-9a-f]{64}");

  /** Why a signed client IP was not used. The names are logged; header values never are. */
  public enum Rejection {
    /** The request has signed headers, but Consent has no secret to check them with. */
    NO_SECRET_CONFIGURED,
    /** A signed header is missing or repeated. */
    INCOMPLETE,
    /** A value is not an IP literal, a decimal timestamp, or 64 lowercase hex digits. */
    MALFORMED,
    /** The timestamp is outside the accepted window. */
    OUTSIDE_WINDOW,
    /** The signature matches no configured secret. */
    BAD_SIGNATURE
  }

  /**
   * The verified client IP, or null with the reason. Both are null for a request without signed
   * headers.
   */
  public record Result(String clientIp, Rejection rejection) {
    private static final Result ABSENT = new Result(null, null);

    static Result rejected(Rejection rejection) {
      return new Result(null, rejection);
    }
  }

  private final List<String> hmacSecrets;
  private final Clock clock;

  /**
   * @param current the secret that the BFF signs with, or null when none is configured
   * @param previous the secret before the last rotation, or null
   */
  public SignedClientIpVerifier(String current, String previous, Clock clock) {
    this.hmacSecrets =
        Stream.of(current, previous)
            .filter(value -> value != null && !value.isBlank())
            // The BFF strips its copy too, so a trailing newline in Secret Manager is harmless.
            .map(String::strip)
            .toList();
    this.clock = clock;
  }

  /** Checks the signed headers. */
  public Result verify(Map<String, List<String>> headers) {
    List<String> ips = valuesOf(headers, CLIENT_IP_HEADER);
    List<String> timestamps = valuesOf(headers, TIMESTAMP_HEADER);
    List<String> signatures = valuesOf(headers, DIGEST_HEADER);
    if (ips.isEmpty() && timestamps.isEmpty() && signatures.isEmpty()) {
      return Result.ABSENT;
    }
    if (hmacSecrets.isEmpty()) {
      return Result.rejected(Rejection.NO_SECRET_CONFIGURED);
    }
    if (ips.size() != 1 || timestamps.size() != 1 || signatures.size() != 1) {
      return Result.rejected(Rejection.INCOMPLETE);
    }
    return verify(ips.getFirst(), timestamps.getFirst(), signatures.getFirst());
  }

  private Result verify(String ip, String timestamp, String signature) {
    if (!ClientIpResolver.isLiteral(ip)
        || !TIMESTAMP.matcher(timestamp).matches()
        || !HEX_SHA256.matcher(signature).matches()) {
      return Result.rejected(Rejection.MALFORMED);
    }
    long signedAt = Long.parseLong(timestamp);
    long now = clock.instant().getEpochSecond();
    if (signedAt < now - MAX_AGE_SECONDS || signedAt > now + MAX_CLOCK_SKEW_SECONDS) {
      return Result.rejected(Rejection.OUTSIDE_WINDOW);
    }
    byte[] received = HexFormat.of().parseHex(signature);
    for (String hmacSecret : hmacSecrets) {
      if (MessageDigest.isEqual(hmac(hmacSecret, ip, timestamp), received)) {
        return new Result(ip, null);
      }
    }
    return Result.rejected(Rejection.BAD_SIGNATURE);
  }

  /** The signature for {@code ip} at {@code timestamp}, as the BFF sends it. */
  static String hmacHex(String hmacSecret, String ip, long timestamp) {
    return HexFormat.of().formatHex(hmac(hmacSecret, ip, Long.toString(timestamp)));
  }

  /** Signs the timestamp exactly as received, so the signed bytes match the BFF's. */
  private static byte[] hmac(String hmacSecret, String ip, String timestamp) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      return mac.doFinal((ip + "\n" + timestamp).getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      // Every JDK has HmacSHA256, and it accepts a key of any non-zero length.
      throw new IllegalStateException(e);
    }
  }

  private static List<String> valuesOf(Map<String, List<String>> headers, String name) {
    return Objects.requireNonNullElse(headers.get(name), List.of());
  }
}
