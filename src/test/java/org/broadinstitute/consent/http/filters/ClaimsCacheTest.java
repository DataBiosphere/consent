package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimsCacheTest {

  @Mock private MultivaluedMap<String, String> mockHeaders;

  private ClaimsCache claimsCache;

  @BeforeEach
  void setUp() {
    claimsCache = new ClaimsCache();
  }

  @Test
  void testConstructorInitializesCache() {
    assertNotNull(claimsCache.cache);
  }

  @Test
  void testLoadCacheAddsTokenToCache() {
    MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    headers.add(ClaimsCache.OAUTH2_CLAIM_email, "test@example.com");
    headers.add(ClaimsCache.OAUTH2_CLAIM_name, "Test User");

    claimsCache.loadCache("test-token", headers);

    Map<String, String> cached = claimsCache.cache.getIfPresent("test-token");
    assertNotNull(cached);
    assertEquals("test@example.com", cached.get(ClaimsCache.OAUTH2_CLAIM_email));
    assertEquals("Test User", cached.get(ClaimsCache.OAUTH2_CLAIM_name));
  }

  @Test
  void testLoadCacheDoesNotOverwriteExistingToken() {
    MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    headers.add(ClaimsCache.OAUTH2_CLAIM_email, "first@example.com");
    claimsCache.loadCache("test-token", headers);

    MultivaluedHashMap<String, String> headers2 = new MultivaluedHashMap<>();
    headers2.add(ClaimsCache.OAUTH2_CLAIM_email, "second@example.com");
    claimsCache.loadCache("test-token", headers2);

    assertEquals(
        "first@example.com",
        claimsCache.cache.getIfPresent("test-token").get(ClaimsCache.OAUTH2_CLAIM_email));
  }

  @Test
  void testLoadCacheFiltersNonOauthHeaders() {
    MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    headers.add("Authorization", "Bearer token");
    headers.add(ClaimsCache.OAUTH2_CLAIM_email, "test@example.com");

    claimsCache.loadCache("test-token", headers);

    Map<String, String> cached = claimsCache.cache.getIfPresent("test-token");
    assertNotNull(cached);
    assertFalse(cached.containsKey("Authorization"));
    assertTrue(cached.containsKey(ClaimsCache.OAUTH2_CLAIM_email));
  }

  @Test
  void testLoadCacheWithNullHeaderValueListThrows() {
    // A null value list reaches the null branch in getFirst(), but Collectors.toMap
    // does not support null values, so NPE is thrown before the entry is stored.
    Set<Map.Entry<String, List<String>>> entries = new HashSet<>();
    entries.add(new AbstractMap.SimpleEntry<>(ClaimsCache.OAUTH2_CLAIM_email, null));
    when(mockHeaders.entrySet()).thenReturn(entries);

    assertThrows(
        NullPointerException.class, () -> claimsCache.loadCache("test-token", mockHeaders));
  }
}
