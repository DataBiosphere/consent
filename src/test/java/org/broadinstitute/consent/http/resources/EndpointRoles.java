package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Shared so a copy filtering fewer verbs cannot quietly stop covering an endpoint. */
final class EndpointRoles {

  private static final Set<Class<? extends Annotation>> HTTP_METHODS =
      Set.of(GET.class, POST.class, PUT.class, PATCH.class, DELETE.class);

  private EndpointRoles() {}

  /** Asserts over every endpoint, so one added later cannot be left out or left unguarded. */
  static void assertEveryEndpointAdmits(Class<?> resource, Set<String> expectedRoles) {
    List<Method> endpoints =
        Arrays.stream(resource.getDeclaredMethods())
            .filter(method -> HTTP_METHODS.stream().anyMatch(method::isAnnotationPresent))
            .toList();

    assertFalse(endpoints.isEmpty(), resource.getSimpleName());
    endpoints.forEach(
        endpoint -> {
          RolesAllowed roles = endpoint.getAnnotation(RolesAllowed.class);
          assertNotNull(roles, endpoint.getName());
          assertEquals(expectedRoles, Set.of(roles.value()), endpoint.getName());
        });
  }
}
