package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Collections;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.resources.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class UserAuthorizerTest {

  private UserAuthorizer authorizer;
  @Mock
  UserRoleDAO userRoleDAO;
  @Mock
  AuthUser authorizedUser;
  @Mock
  AuthUser unauthorizedUser;
  @Mock
  ContainerRequestContext context;

  @BeforeEach
  public void setUp() {
    openMocks(this);
    when(authorizedUser.getEmail()).thenReturn("Authorized User");
    when(unauthorizedUser.getEmail()).thenReturn("Unauthorized User");
    when(userRoleDAO.findRoleNamesByUserEmail("Authorized User")).thenReturn(
        Collections.singletonList(getChairpersonRole().getName()));
    when(userRoleDAO.findRoleNamesByUserEmail("Unauthorized User")).thenReturn(
        Collections.singletonList(getChairpersonRole().getName()));
    authorizer = new UserAuthorizer(userRoleDAO);
  }

  @Test
  public void testAuthorizeNotAuthorized() {
    assertFalse(authorizer.authorize(unauthorizedUser, Resource.MEMBER, context));
  }

  @Test
  public void testAuthorizeAuthorized() {
    assertTrue(authorizer.authorize(authorizedUser, Resource.CHAIRPERSON, context));
  }

  /* Helper Methods */

  private UserRole getChairpersonRole() {
    return new UserRole(1, "CHAIRPERSON");
  }

}