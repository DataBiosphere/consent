package org.broadinstitute.consent.http.filters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manage a cache of bearer token to map of `OAUTH2_CLAIM` headers for every request. This is useful
 * in cases where components need, but do not have access to, the full request context.
 */
public class ClaimsCache {

  public final Cache<String, Map<String, String>> cache;
  public static final String OAUTH2_CLAIM_email = "OAUTH2_CLAIM_email";
  public static final String OAUTH2_CLAIM_name = "OAUTH2_CLAIM_name";
  public static final String OAUTH2_CLAIM_access_token = "OAUTH2_CLAIM_access_token";
  public static final String OAUTH2_CLAIM_aud = "OAUTH2_CLAIM_aud";

  @Inject
  public ClaimsCache() {
    cache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
  }

  public void loadCache(String token, MultivaluedMap<String, String> headers) {
    try {
      this.cache.get(
          token,
          () ->
              headers.entrySet().stream()
                  .filter(e -> e.getKey().startsWith("OAUTH2_CLAIM"))
                  .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                  .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().get(0))));
    } catch (Exception _) {
      // header map is caller-supplied; a failure here means no cache entry is stored
    }
  }
}
