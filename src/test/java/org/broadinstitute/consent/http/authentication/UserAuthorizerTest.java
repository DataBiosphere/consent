package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.resources.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAuthorizerTest {

  private UserAuthorizer authorizer;
  @Mock
  private UserRoleDAO userRoleDAO;
  @Mock
  private AuthUser authorizedUser;
  @Mock
  private AuthUser unauthorizedUser;
  @Mock
  private ContainerRequestContext context;

  @BeforeEach
  void setUp() {
    authorizer = new UserAuthorizer(userRoleDAO);
  }

  @Test
  void testAuthorizeNotAuthorized() {
    assertFalse(authorizer.authorize(unauthorizedUser, Resource.MEMBER, context));
  }

  @Test
  void testAuthorizeAuthorized() {
    when(userRoleDAO.findRoleNamesByUserEmail(any()))
      .thenReturn(List.of(UserRoles.CHAIRPERSON.getRoleName()));
    assertTrue(authorizer.authorize(authorizedUser, Resource.CHAIRPERSON, context));
  }

}