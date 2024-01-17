package org.broadinstitute.consent.http.filters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.concurrent.TimeUnit;

/**
 * Manage a cache of bearer token to map of headers for every request. This is useful in cases
 * where components need, but do not have, access to the complete request context.
 */
@Singleton
public class RequestHeaderCache {

  private static RequestHeaderCache INSTANCE;
  public final Cache<String, MultivaluedMap<String, String>> cache;
  public final static String OAUTH2_CLAIM_email = "OAUTH2_CLAIM_email";
  public final static String OAUTH2_CLAIM_name = "OAUTH2_CLAIM_name";
  public final static String OAUTH2_CLAIM_access_token = "OAUTH2_CLAIM_access_token";
  public final static String OAUTH2_CLAIM_aud = "OAUTH2_CLAIM_aud";

  private RequestHeaderCache() {
    cache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
  }

  public static RequestHeaderCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RequestHeaderCache();
    }
    return INSTANCE;
  }

  public void loadCache(String token, MultivaluedMap<String, String> headers) {
    if (this.cache.getIfPresent(token) == null) {
      this.cache.put(token, headers);
      this.cache.cleanUp();
    }
  }

}
