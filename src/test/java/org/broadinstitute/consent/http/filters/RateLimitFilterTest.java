package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

  @Mock private ContainerRequestContext requestContext;
  @Mock private SecurityContext securityContext;
  @Mock private UriInfo uriInfo;

  @BeforeEach
  void setUp() {
    // Default to a rate-limited path; tests exercising path exclusion override this.
    lenient().when(requestContext.getUriInfo()).thenReturn(uriInfo);
    lenient().when(uriInfo.getPath()).thenReturn("api/user/me");
  }

  private void mockPath(String path) {
    when(uriInfo.getPath()).thenReturn(path);
  }

  private RateLimitFilter buildFilter(int requestsPerMinute, int podCount, boolean enabled) {
    RateLimitConfiguration config = new RateLimitConfiguration();
    config.setRequestsPerMinute(requestsPerMinute);
    config.setPodCount(podCount);
    config.setEnabled(enabled);
    return new RateLimitFilter(config);
  }

  private void mockAuthenticatedUser(String email) {
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getUserPrincipal()).thenReturn(new AuthUser(email));
  }

  @Test
  void testAllowsRequestsWithinPerPodCapacity() {
    // requestsPerMinute / podCount == 3 per pod
    RateLimitFilter filter = buildFilter(9, 3, true);
    mockAuthenticatedUser("user@example.com");

    filter.filter(requestContext);
    filter.filter(requestContext);
    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testRejectsRequestsExceedingPerPodCapacity() {
    RateLimitFilter filter = buildFilter(9, 3, true);
    mockAuthenticatedUser("user@example.com");

    filter.filter(requestContext);
    filter.filter(requestContext);
    filter.filter(requestContext);
    filter.filter(requestContext);

    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(captor.capture());
    assertEquals(429, captor.getValue().getStatus());
    long retryAfterSeconds = Long.parseLong(captor.getValue().getHeaderString("Retry-After"));
    assertTrue(retryAfterSeconds > 0 && retryAfterSeconds <= 60);
  }

  @Test
  void testDisabledConfigurationNeverRejects() {
    RateLimitFilter filter = buildFilter(1, 1, false);

    filter.filter(requestContext);
    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());
    verify(requestContext, never()).getSecurityContext();
  }

  @Test
  void testDifferentAuthenticatedUsersHaveIndependentBuckets() {
    RateLimitFilter filter = buildFilter(1, 1, true);

    mockAuthenticatedUser("userA@example.com");
    filter.filter(requestContext);

    mockAuthenticatedUser("userB@example.com");
    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testNoSecurityContextIsNeverRateLimited() {
    // e.g. an api/ endpoint the auth filter didn't run for, or the auth filter rejecting the
    // request itself (401) before this filter would ever see it in practice.
    RateLimitFilter filter = buildFilter(1, 1, true);
    when(requestContext.getSecurityContext()).thenReturn(null);

    for (int i = 0; i < 20; i++) {
      filter.filter(requestContext);
    }

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testNullPrincipalIsNeverRateLimited() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getUserPrincipal()).thenReturn(null);

    for (int i = 0; i < 20; i++) {
      filter.filter(requestContext);
    }

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testAuthUserWithNullEmailIsNeverRateLimited() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getUserPrincipal()).thenReturn(new AuthUser());

    for (int i = 0; i < 20; i++) {
      filter.filter(requestContext);
    }

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testAuthUserWithBlankEmailIsNeverRateLimited() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getUserPrincipal()).thenReturn(new AuthUser("   "));

    for (int i = 0; i < 20; i++) {
      filter.filter(requestContext);
    }

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testStatusPathIsNeverRateLimited() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    mockPath("status");

    for (int i = 0; i < 20; i++) {
      filter.filter(requestContext);
    }

    verify(requestContext, never()).abortWith(any());
    verify(requestContext, never()).getSecurityContext();
  }

  @Test
  void testLivenessPathIsNeverRateLimited() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    mockPath("liveness");

    for (int i = 0; i < 20; i++) {
      filter.filter(requestContext);
    }

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testSwaggerPathIsNeverRateLimited() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    mockPath("swagger/index.html");

    for (int i = 0; i < 20; i++) {
      filter.filter(requestContext);
    }

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testApiPathIsStillRateLimitedAlongsideExcludedPaths() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    mockAuthenticatedUser("user@example.com");

    filter.filter(requestContext);
    filter.filter(requestContext);

    verify(requestContext).abortWith(any());
  }
}
