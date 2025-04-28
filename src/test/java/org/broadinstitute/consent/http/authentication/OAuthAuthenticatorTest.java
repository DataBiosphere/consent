package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosAuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatus.UserInfo;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthAuthenticatorTest extends AbstractTestHelper {

  @Mock
  private SamService samService;
  @Mock
  private UserService userService;
  private OAuthAuthenticator oAuthAuthenticator;
  private final ClaimsCache headerCache = ClaimsCache.getInstance();
  private final String bearerToken = randomAlphabetic(100);
  private final MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();

  @BeforeEach
  void setUp() {
    headerCache.cache.invalidateAll();
    oAuthAuthenticator = new OAuthAuthenticator(samService, userService);
  }

  @Test
  void testAuthenticateWithToken() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerCache.loadCache(bearerToken, headerMap);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
  }

  @Test
  void testAuthenticateGetUserInfoSuccess() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertNotNull(authUser.orElseThrow().getEmail());
    assertNotNull(authUser.get().getAuthToken());
  }

  /**
   * Test that in the case of a header lookup failure, we don't fail the overall request.
   */
  @Test
  void testAuthenticateGetUserInfoFailure() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerCache.loadCache(bearerToken, headerMap);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertEquals(bearerToken, authUser.orElseThrow().getAuthToken());
  }

  /**
   * Test that in the case of a Sam user lookup failure, we then try to register the user
   */
  @Test
  void testAuthenticateGetUserWithStatusInfoFailurePostUserSuccess() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);
    when(samService.getRegistrationInfo(any())).thenThrow(new NotFoundException());
    when(samService.postRegistrationInfo(any())).thenReturn(
        new UserStatus()
            .setUserInfo(new UserInfo().setUserEmail("email").setUserSubjectId("subjectId")));

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertEquals(bearerToken, authUser.orElseThrow().getAuthToken());
    verify(samService, times(1)).postRegistrationInfo(any());
  }

  /**
   * Test that in the case of a Sam user lookup failure, we then try to register the user.
   * if that fails, we throw an exception.
   */
  @Test
  void testAuthenticateGetUserWithStatusInfoFailurePostUserFailureWebAppEx() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerCache.loadCache(bearerToken, headerMap);
    when(samService.getRegistrationInfo(any())).thenThrow(new NotFoundException());
    when(samService.postRegistrationInfo(any())).thenThrow(new Exception("errorMessage"));

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> oAuthAuthenticator.authenticate(bearerToken));
    assertEquals("errorMessage", ex.getMessage());
  }

  /**
   * Test that in the case of a missing claim headers, we don't fail on Sam user lookup
   */
  @Test
  void testAuthenticateGetUserWithStatusInfoIncompleteClaims() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerCache.loadCache(bearerToken, headerMap);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertEquals(authUser.orElseThrow().getAuthToken(), bearerToken);
    verify(samService, never()).getRegistrationInfo(any());
  }

  /**
   * Test that if the name is "unknown" in the header, we use the email as the name
   */
  @Test
  void testUnknownNameDefaultsToEmail() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("unknown"));
    headerCache.loadCache(bearerToken, headerMap);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertEquals(authUser.orElseThrow().getName(), authUser.orElseThrow().getEmail());
  }

  @Test
  void testAuthenticateGetUserInfoWithDUOSUser() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);
    when(userService.findUserByEmail(headerMap.get(ClaimsCache.OAUTH2_CLAIM_email).get(0))).thenReturn(new User());

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertInstanceOf(DuosAuthUser.class, authUser.orElseThrow());
    assertNotNull(((DuosAuthUser) authUser.orElseThrow()).getUser());
  }

  @Test
  void testAuthenticateGetUserInfoWithDUOSUserNotFound() {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);
    when(userService.findUserByEmail(headerMap.get(ClaimsCache.OAUTH2_CLAIM_email).get(0))).thenThrow(new NotFoundException());

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertInstanceOf(AuthUser.class, authUser.orElseThrow());
    assertFalse(authUser.get() instanceof DuosAuthUser);
  }

}
