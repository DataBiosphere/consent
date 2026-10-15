package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.configurations.PublicRateLimitConfiguration;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks the limit through Jersey's own routing, not by calling the filter. A path that Jersey
 * routes to a {@code showcase} resource must count against the same bucket as the canonical path,
 * even when the raw path does not start with {@code showcase/}.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class PublicRateLimitRoutingTest {

  private static final String CLIENT = "192.0.2.50";
  private static final HttpServletRequest SERVLET_REQUEST = mock(HttpServletRequest.class);

  // Each test gets a fresh extension, so each starts with a full bucket.
  private final ResourceExtension resource =
      ResourceExtension.builder()
          .addResource(new ShowcaseResource())
          .addProvider(
              new PreMatchingDelegate(
                  new PublicRateLimitFilter(oneRequestPerMinute(), SERVLET_REQUEST)))
          .build();

  /**
   * Jersey injects {@code @Context} fields into every registered provider, which would replace the
   * mock servlet request with the in-memory container's proxy. Registering this wrapper instead
   * keeps the mock in the real filter. It is pre-matching, as the real filter is.
   */
  @PreMatching
  @Priority(Priorities.AUTHENTICATION)
  private record PreMatchingDelegate(PublicRateLimitFilter filter)
      implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
      filter.filter(requestContext);
    }
  }

  @Path("showcase/datasets")
  public static class ShowcaseResource {
    @GET
    public String datasets() {
      return "ok";
    }
  }

  private static RateLimitConfiguration oneRequestPerMinute() {
    PublicRateLimitConfiguration showcase = new PublicRateLimitConfiguration();
    showcase.setRequestsPerMinute(1);
    showcase.setTrustedProxies(List.of("127.0.0.1/32"));
    RateLimitConfiguration config = new RateLimitConfiguration();
    config.setEnabled(true);
    config.setRequestsPerMinute(100);
    config.setPodCount(1);
    config.setShowcase(showcase);
    return config;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "showcase;x=1/datasets", // a matrix parameter on the first segment
        "showcase;/datasets", // an empty matrix parameter
        "showcase/datasets;x=1" // a matrix parameter on the last segment
      })
  void testVariantIsRoutedToTheShowcaseResource(String variant) {
    when(SERVLET_REQUEST.getRemoteAddr()).thenReturn(CLIENT);

    assertEquals(200, status(variant));
  }

  @ParameterizedTest
  @ValueSource(strings = {"showcase;x=1/datasets", "showcase;/datasets", "showcase/datasets;x=1"})
  void testVariantCountsAgainstTheSameBucket(String variant) {
    when(SERVLET_REQUEST.getRemoteAddr()).thenReturn(CLIENT);

    assertEquals(200, status("showcase/datasets"));
    assertEquals(429, status(variant));
  }

  private int status(String path) {
    try (Response response = resource.target(path).request().get()) {
      return response.getStatus();
    }
  }
}
