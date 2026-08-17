package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatus.UserInfo;
import org.broadinstitute.consent.http.resources.Resource;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationHelperTest extends AbstractTestHelper {

  @Mock private SamService samService;
  @Mock private UserService userService;
  @Mock private AuthUser authorizedUser;
  @Mock private AuthUser unauthorizedUser;
  @Mock private DuosUser authorizedDuosUser;
  @Mock private DuosUser unauthorizedDuosUser;

  private AuthorizationHelper authorizationHelper;
  private DuosUserAuthenticator duosUserAuthenticator;
  private OAuthAuthenticator oAuthAuthenticator;
  private final ClaimsCache headerCache = new ClaimsCache();
  private final String bearerToken = randomAlphabetic(100);
  private final MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();

  @BeforeEach
  void setUp() {
    authorizationHelper = new AuthorizationHelper(samService, userService, headerCache);
    headerCache.cache.invalidateAll();
    duosUserAuthenticator = new DuosUserAuthenticator(authorizationHelper);
    oAuthAuthenticator = new OAuthAuthenticator(authorizationHelper);
  }

  @Test
  void testAuthorized() {
    unauthorizedUser.setEmail("email");
    unauthorizedDuosUser.setEmail(unauthorizedUser.getEmail());
    User user = new User();
    user.setEmail(unauthorizedUser.getEmail());
    user.addRole(UserRoles.Chairperson());
    when(userService.findUserByEmail(unauthorizedUser.getEmail())).thenReturn(user);
    assertTrue(authorizationHelper.authorize(authorizedUser, Resource.CHAIRPERSON));
    assertTrue(authorizationHelper.authorize(authorizedDuosUser, Resource.CHAIRPERSON));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        Resource.MEMBER,
        Resource.CHAIRPERSON,
        Resource.SIGNINGOFFICIAL,
        Resource.ADMIN,
        Resource.DATASUBMITTER,
        Resource.ITDIRECTOR
      })
  void testNotAuthorized(String roleName) {
    unauthorizedUser.setEmail("email");
    unauthorizedDuosUser.setEmail(unauthorizedUser.getEmail());
    User user = new User();
    user.setEmail(unauthorizedUser.getEmail());
    user.addRole(UserRoles.Researcher());
    when(userService.findUserByEmail(unauthorizedUser.getEmail())).thenReturn(user);
    assertFalse(authorizationHelper.authorize(unauthorizedUser, roleName));
    assertFalse(authorizationHelper.authorize(unauthorizedDuosUser, roleName));
  }

  @Test
  void testAuthenticateWithToken() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerCache.loadCache(bearerToken, headerMap);

    assertDoesNotThrow(() -> oAuthAuthenticator.authenticate(bearerToken));
    assertDoesNotThrow(() -> duosUserAuthenticator.authenticate(bearerToken));
  }

  @Test
  void testAuthenticateGetUserInfoSuccess() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);

    AuthUser authUser = oAuthAuthenticator.authenticate(bearerToken).orElseThrow();
    assertNotNull(authUser.getEmail());
    assertNotNull(authUser.getAuthToken());
  }

  /** Test that in the case of a header lookup failure, we don't fail the overall request. */
  @Test
  void testAuthenticateGetUserInfoFailure() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerCache.loadCache(bearerToken, headerMap);

    AuthUser authUser = oAuthAuthenticator.authenticate(bearerToken).orElseThrow();
    assertEquals(bearerToken, authUser.getAuthToken());
    // A DuosUser is not created if the user is not found; the 404 surfaces instead of a 401 so
    // callers can tell an unregistered user apart from a rejected token.
    doThrow(NotFoundException.class).when(userService).findUserByEmail(anyString());
    assertThrows(NotFoundException.class, () -> duosUserAuthenticator.authenticate(bearerToken));
  }

  /** Test that in the case of a Sam user lookup failure, we then try to register the user */
  @Test
  void testAuthenticateGetUserWithStatusInfoFailurePostUserSuccess() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);
    when(samService.getCombinedUserStatusInfo(any())).thenThrow(new NotFoundException());
    when(samService.postRegistrationInfo(any()))
        .thenReturn(
            new UserStatus()
                .setUserInfo(new UserInfo().setUserEmail("email").setUserSubjectId("subjectId")));

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertEquals(bearerToken, authUser.orElseThrow().getAuthToken());
    verify(samService, times(1)).postRegistrationInfo(any());
  }

  /**
   * Test that in the case of a Sam user lookup failure, we then try to register the user. if that
   * fails, we throw an exception.
   */
  @Test
  void testAuthenticateGetUserWithStatusInfoFailurePostUserFailureWebAppEx() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerCache.loadCache(bearerToken, headerMap);
    when(samService.getCombinedUserStatusInfo(any())).thenThrow(new NotFoundException());
    when(samService.postRegistrationInfo(any())).thenThrow(new Exception("errorMessage"));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> oAuthAuthenticator.authenticate(bearerToken));
    assertEquals("errorMessage", ex.getMessage());
  }

  /**
   * Test that in the case of a missing claim headers (other than email), we don't fail on Sam user
   * lookup
   */
  @Test
  void testAuthenticateGetUserWithStatusInfoIncompleteClaims() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerCache.loadCache(bearerToken, headerMap);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertEquals(authUser.orElseThrow().getAuthToken(), bearerToken);
  }

  /** Test that in the case of a missing email header, we throw an exception. */
  @Test
  void testAuthenticateGetUserWithStatusInfMissingEmailClaimsThrows() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerCache.loadCache(bearerToken, headerMap);

    assertThrows(NotAuthorizedException.class, () -> oAuthAuthenticator.authenticate(bearerToken));
  }

  /** Test that if the name is "unknown" in the header, we use the email as the name */
  @Test
  void testUnknownNameDefaultsToEmail() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("unknown"));
    headerCache.loadCache(bearerToken, headerMap);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertEquals(authUser.orElseThrow().getName(), authUser.orElseThrow().getEmail());
  }
}
