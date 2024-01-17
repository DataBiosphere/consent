package org.broadinstitute.consent.http.filters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class RequestHeaderCache {

  private static RequestHeaderCache INSTANCE;

  private RequestHeaderCache() {
  }

  public static RequestHeaderCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RequestHeaderCache();
    }
    return INSTANCE;
  }

  public Cache<String, MultivaluedMap<String, String>> cache = CacheBuilder
      .newBuilder()
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .build();

  public void loadCache(String token, MultivaluedMap<String, String> headers) {
    if (this.cache.getIfPresent(token) == null) {
      this.cache.put(token, headers);
      this.cache.cleanUp();
    }
  }

}
