package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthenticationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthAuthenticatorTest {

  @Mock
  private SamService samService;
  private OAuthAuthenticator oAuthAuthenticator;
  private final ClaimsCache headerCache = ClaimsCache.getInstance();
  private final String bearerToken = RandomStringUtils.randomAlphabetic(100);
  private final MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();

  @BeforeEach
  void setUp() {
    headerCache.cache.invalidateAll();
    headerMap.clear();
  }

  @Test
  void testAuthenticateWithToken() throws AuthenticationException {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerCache.loadCache(bearerToken, headerMap);
    oAuthAuthenticator = new OAuthAuthenticator(samService);
    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
  }

  @Test
  void testAuthenticateGetUserInfoSuccess() throws AuthenticationException {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);
    oAuthAuthenticator = new OAuthAuthenticator(samService);
    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertNotNull(authUser.get().getEmail());
    assertNotNull(authUser.get().getAuthToken());
  }

  /**
   * Test that in the case of a header lookup failure, we don't fail the overall request.
   */
  @Test
  void testAuthenticateGetUserInfoFailure() throws AuthenticationException {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerCache.loadCache(bearerToken, headerMap);
    oAuthAuthenticator = new OAuthAuthenticator(samService);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertEquals(authUser.get().getAuthToken(), bearerToken);
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
    oAuthAuthenticator = new OAuthAuthenticator(samService);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertEquals(authUser.get().getAuthToken(), bearerToken);
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
    when(samService.postRegistrationInfo(any())).thenThrow(new WebApplicationException("errorMessage"));
    oAuthAuthenticator = new OAuthAuthenticator(samService);

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> oAuthAuthenticator.authenticate(bearerToken));
    assertEquals("errorMessage", ex.getMessage());
  }

  /**
   * Test that in the case of a Sam user lookup failure, we then try to register the user.
   * if that fails, we throw an exception.
   */
  @Test
  void testAuthenticateGetUserWithStatusInfoFailurePostUserFailureAuthEx() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("name"));
    headerCache.loadCache(bearerToken, headerMap);
    when(samService.getRegistrationInfo(any())).thenThrow(new NotFoundException());
    when(samService.postRegistrationInfo(any())).thenThrow(new AuthenticationException("errorMessage"));
    oAuthAuthenticator = new OAuthAuthenticator(samService);

    AuthenticationException ex = assertThrows(AuthenticationException.class, () -> oAuthAuthenticator.authenticate(bearerToken));
    assertEquals("AuthUser not able to be registered: '{\"email\":\"email\",\"name\":\"name\"}", ex.getMessage());
  }

  /**
   * Test that in the case of a missing claim headers, we don't fail on Sam user lookup
   */
  @Test
  void testAuthenticateGetUserWithStatusInfoIncompleteClaims() throws Exception {
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerCache.loadCache(bearerToken, headerMap);
    oAuthAuthenticator = new OAuthAuthenticator(samService);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertEquals(authUser.get().getAuthToken(), bearerToken);
    verify(samService, never()).getRegistrationInfo(any());
  }

  /**
   * Test that if the name is "unknown" in the header, we use the email as the name
   */
  @Test
  void testUnknownNameDefaultsToEmail() {
    String bearerToken = RandomStringUtils.randomAlphabetic(100);
    MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_access_token, List.of(bearerToken));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_email, List.of("email"));
    headerMap.put(ClaimsCache.OAUTH2_CLAIM_name, List.of("unknown"));
    headerCache.loadCache(bearerToken, headerMap);
    oAuthAuthenticator = new OAuthAuthenticator(samService);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertEquals(authUser.get().getName(), authUser.get().getEmail());
  }

}
