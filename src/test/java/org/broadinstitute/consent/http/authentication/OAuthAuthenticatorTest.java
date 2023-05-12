package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.gson.Gson;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class OAuthAuthenticatorTest {

  @Mock
  private Client client;

  @Mock
  private SamService samService;

  @Mock
  private WebTarget target;

  @Mock
  private Invocation.Builder builder;

  @Mock
  private Response response;

  private OAuthAuthenticator oAuthAuthenticator;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  @Test
  public void testAuthenticateWithToken() {
    oAuthAuthenticator = new OAuthAuthenticator(client, samService);
    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate("bearer-token");
    assertTrue(authUser.isPresent());
  }

  @Test
  public void testAuthenticateGetUserInfoSuccess() {
    String bearerToken = "bearer-token";
    Gson gson = new Gson();
    GenericUser user = new GenericUser();
    user.setEmail("email");
    user.setName("name");
    when(client.target(anyString())).thenReturn(target);
    when(target.request(MediaType.APPLICATION_JSON_TYPE)).thenReturn(builder);
    when(builder.get(Response.class)).thenReturn(response);
    when(response.readEntity(String.class)).thenReturn(gson.toJson(user));
    when(response.getStatus()).thenReturn(200);
    oAuthAuthenticator = new OAuthAuthenticator(client, samService);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertEquals(user.getEmail(), authUser.get().getEmail());
    assertEquals(user.getName(), authUser.get().getName());
    assertEquals(authUser.get().getAuthToken(), bearerToken);
  }

  /**
   * Test that in the case of a token lookup failure, we don't fail the overall request.
   */
  @Test
  public void testAuthenticateGetUserInfoFailure() {
    String bearerToken = "bearer-token";
    when(client.target(anyString())).thenReturn(target);
    when(target.request(MediaType.APPLICATION_JSON_TYPE)).thenReturn(builder);
    when(builder.get(Response.class)).thenReturn(response);
    when(response.readEntity(String.class)).thenReturn("Bad Request");
    when(response.getStatus()).thenReturn(400);
    oAuthAuthenticator = new OAuthAuthenticator(client, samService);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertEquals(authUser.get().getAuthToken(), bearerToken);
  }

  /**
   * Test that in the case of a Sam user lookup failure, we then try to register the user
   */
  @Test
  public void testAuthenticateGetUserWithStatusInfoFailurePostUserSuccess() throws Exception {
    String bearerToken = "bearer-token";
    when(samService.getRegistrationInfo(any())).thenThrow(new NotFoundException());
    oAuthAuthenticator = new OAuthAuthenticator(client, samService);
    spy(samService);

    Optional<AuthUser> authUser = oAuthAuthenticator.authenticate(bearerToken);
    assertTrue(authUser.isPresent());
    assertEquals(authUser.get().getAuthToken(), bearerToken);
    verify(samService, times(1)).postRegistrationInfo(any());
  }

}
