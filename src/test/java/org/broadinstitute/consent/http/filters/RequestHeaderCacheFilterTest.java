package org.broadinstitute.consent.http.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestHeaderCacheFilterTest {

  @Mock private ClaimsCache claimsCache;
  @Mock private ContainerRequestContext requestContext;
  @Mock private MultivaluedMap<String, String> headers;

  private RequestHeaderCacheFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RequestHeaderCacheFilter(claimsCache);
    when(requestContext.getHeaders()).thenReturn(headers);
  }

  @Test
  void testFilterCallsLoadCacheWhenTokenPresent() throws Exception {
    when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer test-token");

    filter.filter(requestContext);

    verify(claimsCache).loadCache("test-token", headers);
  }

  @Test
  void testFilterDoesNotCallLoadCacheWhenNoAuthorizationHeader() throws Exception {
    filter.filter(requestContext);

    verify(claimsCache, never()).loadCache(any(), any());
  }
}
