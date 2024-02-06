package org.broadinstitute.consent.http.filters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manage a cache of bearer token to map of `OAUTH2_CLAIM` headers for every request. This is
 * useful in cases where components need, but do not have access to, the full request context.
 */
public class ClaimsCache {

  private static ClaimsCache INSTANCE;
  public final Cache<String, Map<String, String>> cache;
  public final static String OAUTH2_CLAIM_email = "OAUTH2_CLAIM_email";
  public final static String OAUTH2_CLAIM_name = "OAUTH2_CLAIM_name";
  public final static String OAUTH2_CLAIM_access_token = "OAUTH2_CLAIM_access_token";
  public final static String OAUTH2_CLAIM_aud = "OAUTH2_CLAIM_aud";

  private ClaimsCache() {
    cache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
  }

  public static ClaimsCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ClaimsCache();
    }
    return INSTANCE;
  }

  private String getFirst(List<String> headerValues) {
    if (headerValues == null) {
      return null;
    }
    return headerValues.stream().findFirst().orElse(null);
  }

  public void loadCache(String token, MultivaluedMap<String, String> headers) {
    if (this.cache.getIfPresent(token) == null) {
      Map<String, String> claimsMap = headers.entrySet()
          .stream()
          .filter(e -> e.getKey().startsWith("OAUTH2_CLAIM"))
          .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), getFirst(e.getValue())))
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      this.cache.put(token, claimsMap);
      this.cache.cleanUp();
    }
  }

}
