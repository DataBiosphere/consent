package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

  @Mock private ContainerRequestContext requestContext;
  @Mock private SecurityContext securityContext;

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
    assertEquals("60", captor.getValue().getHeaderString("Retry-After"));
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
  void testFallsBackToForwardedForHeaderWhenUnauthenticated() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getUserPrincipal()).thenReturn(null);
    when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("1.2.3.4");

    filter.filter(requestContext);
    filter.filter(requestContext);

    verify(requestContext).abortWith(any());
  }

  @Test
  void testFallsBackToUnknownKeyWhenNoPrincipalAndNoForwardedForHeader() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    when(requestContext.getSecurityContext()).thenReturn(null);
    when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn(null);

    filter.filter(requestContext);
    filter.filter(requestContext);

    verify(requestContext).abortWith(any());
  }

  @Test
  void testAuthUserWithNullEmailFallsBackToForwardedFor() {
    RateLimitFilter filter = buildFilter(1, 1, true);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getUserPrincipal()).thenReturn(new AuthUser());
    when(requestContext.getHeaderString("X-Forwarded-For")).thenReturn("5.6.7.8");

    filter.filter(requestContext);
    filter.filter(requestContext);

    verify(requestContext).abortWith(any());
  }
}
