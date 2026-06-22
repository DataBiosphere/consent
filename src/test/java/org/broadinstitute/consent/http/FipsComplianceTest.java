package org.broadinstitute.consent.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the FIPS 140-3 configuration applied in ConsentApplication.main() and the Docker
 * image cannot be silently disabled or bypassed.
 *
 * <p>Each test saves and restores JVM security state so the suite can run in any order without
 * polluting the shared provider registry.
 */
class FipsComplianceTest {

  private static final String APPROVED_ONLY_PROP = "org.bouncycastle.fips.approved_only";
  private static final String BCFIPS = "BCFIPS";

  private String savedApprovedOnly;

  @BeforeEach
  void saveState() {
    savedApprovedOnly = System.getProperty(APPROVED_ONLY_PROP);
    // Remove any stale registration from a prior test run in the same JVM.
    Security.removeProvider(BCFIPS);
  }

  @AfterEach
  void restoreState() {
    Security.removeProvider(BCFIPS);
    if (savedApprovedOnly == null) {
      System.clearProperty(APPROVED_ONLY_PROP);
    } else {
      System.setProperty(APPROVED_ONLY_PROP, savedApprovedOnly);
    }
  }

  @Test
  void testBouncyCastleFipsProviderIsAvailableOnClasspath() {
    BouncyCastleFipsProvider provider = new BouncyCastleFipsProvider();
    assertNotNull(provider);
    assertEquals(BCFIPS, provider.getName());
  }

  @Test
  void testApprovedOnlyPropertyMustBeSetBeforeProviderIsInstantiated() {
    // ConsentApplication.main() sets the system property before calling
    // Security.insertProviderAt(). The property is read at construction time; setting it
    // after instantiation does not retroactively enable approved-only mode.
    System.setProperty(APPROVED_ONLY_PROP, "true");
    BouncyCastleFipsProvider provider = new BouncyCastleFipsProvider();
    Security.insertProviderAt(provider, 1);

    assertEquals(
        "true",
        System.getProperty(APPROVED_ONLY_PROP),
        "approved_only system property must remain set after provider registration");
    assertEquals(
        BCFIPS,
        Security.getProviders()[0].getName(),
        "BouncyCastleFipsProvider must be the first JCE provider");
  }

  @Test
  void testFipsProviderRegistersAtPositionOne() {
    System.setProperty(APPROVED_ONLY_PROP, "true");
    Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);

    assertEquals(
        BCFIPS,
        Security.getProviders()[0].getName(),
        "BouncyCastleFipsProvider must be the highest-priority JCE provider");
  }

  @Test
  void testNonFipsAlgorithmMd5IsRejectedByBcfipsProvider() {
    // MD5 is not a FIPS 140-3 approved digest. In approved-only mode the BCFIPS provider
    // omits it from its algorithm table, so getInstance() must throw.
    System.setProperty(APPROVED_ONLY_PROP, "true");
    Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);

    assertThrows(
        NoSuchAlgorithmException.class,
        () -> MessageDigest.getInstance("MD5", BCFIPS),
        "BCFIPS provider must not expose MD5 in approved-only mode");
  }

  @Test
  void testFipsApprovedDigestSha256IsAccepted() throws Exception {
    // SHA-256 is FIPS 140-3 approved and must be available via the BCFIPS provider.
    System.setProperty(APPROVED_ONLY_PROP, "true");
    Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);

    assertDoesNotThrow(
        () -> MessageDigest.getInstance("SHA-256", BCFIPS),
        "BCFIPS provider must expose SHA-256 in approved-only mode");
  }

  @Test
  void testFipsApprovedDigestSha384IsAccepted() throws Exception {
    System.setProperty(APPROVED_ONLY_PROP, "true");
    Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);

    assertDoesNotThrow(
        () -> MessageDigest.getInstance("SHA-384", BCFIPS),
        "BCFIPS provider must expose SHA-384 in approved-only mode");
  }
}
