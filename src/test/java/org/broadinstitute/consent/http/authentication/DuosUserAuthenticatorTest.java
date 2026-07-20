package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuosUserAuthenticatorTest extends AbstractTestHelper {

  @Mock private SamService samService;
  @Mock private UserService userService;

  private DuosUserAuthenticator authenticator;
  private final ClaimsCache claimsCache = new ClaimsCache();
  private final String bearerToken = randomAlphabetic(100);
  private final MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();

  @BeforeEach
  void setUp() {
    AuthorizationHelper authorizationHelper =
        new AuthorizationHelper(samService, userService, claimsCache);
    authenticator = new DuosUserAuthenticator(authorizationHelper);
    claimsCache.cache.invalidateAll();
  }

  /** When the bearer token has no cache entry, authenticate returns empty and logs an error. */
  @Test
  void testAuthenticateTokenNotInCache() {
    Optional<DuosUser> result = authenticator.authenticate(bearerToken);

    assertFalse(result.isPresent());
  }

  /** When headers are cached and the user is found, authenticate returns a populated DuosUser. */
  @Test
  void testAuthenticateUserFound() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("test@example.com"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("Test User"));
    claimsCache.loadCache(bearerToken, headerMap);

    User user = new User();
    user.setEmail("test@example.com");
    when(userService.findUserByEmail("test@example.com")).thenReturn(user);

    Optional<DuosUser> result = authenticator.authenticate(bearerToken);

    assertTrue(result.isPresent());
    assertNotNull(result.get().getEmail());
  }

  /** When the user is not found in the local store, authenticate returns empty. */
  @Test
  void testAuthenticateUserNotFound() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("unknown@example.com"));
    claimsCache.loadCache(bearerToken, headerMap);

    doThrow(NotFoundException.class).when(userService).findUserByEmail(anyString());

    Optional<DuosUser> result = authenticator.authenticate(bearerToken);

    assertFalse(result.isPresent());
  }

  /**
   * When the email claim is absent from the cached headers, buildAuthUserFromHeaders throws
   * NotAuthorizedException, which propagates out of authenticate.
   */
  @Test
  void testAuthenticateMissingEmailThrows() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    // No email header — intentionally omitted
    claimsCache.loadCache(bearerToken, headerMap);

    assertThrows(NotAuthorizedException.class, () -> authenticator.authenticate(bearerToken));
  }

  /** When Sam returns user status info, the resulting DuosUser carries that status info. */
  @Test
  void testAuthenticateSetsUserStatusInfo() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("status@example.com"));
    claimsCache.loadCache(bearerToken, headerMap);

    UserStatusInfo statusInfo =
        new UserStatusInfo().setUserEmail("status@example.com").setUserSubjectId("sub-001");
    when(samService.getCombinedUserStatusInfo(any())).thenReturn(statusInfo);

    User user = new User();
    user.setEmail("status@example.com");
    when(userService.findUserByEmail("status@example.com")).thenReturn(user);

    Optional<DuosUser> result = authenticator.authenticate(bearerToken);

    assertTrue(result.isPresent());
    assertNotNull(result.get().getUserStatusInfo());
    verify(samService).getCombinedUserStatusInfo(any());
  }

  /**
   * When only the required email header is cached (no token or name), authenticate still succeeds
   * and returns a DuosUser.
   */
  @Test
  void testAuthenticateWithMinimalHeaders() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("minimal@example.com"));
    claimsCache.loadCache(bearerToken, headerMap);

    User user = new User();
    user.setEmail("minimal@example.com");
    when(userService.findUserByEmail("minimal@example.com")).thenReturn(user);

    Optional<DuosUser> result = authenticator.authenticate(bearerToken);

    assertTrue(result.isPresent());
  }
}
