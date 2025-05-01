package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.resources.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizerTest {

  private AuthorizerHelper authorizerHelper;
  @Mock
  private UserRoleDAO userRoleDAO;
  @Mock
  private AuthUser authorizedUser;
  @Mock
  private AuthUser unauthorizedUser;
  @Mock
  private DuosUser authorizedDuosUser;
  @Mock
  private DuosUser unauthorizedDuosUser;

  @BeforeEach
  void setUp() {
    authorizerHelper = new AuthorizerHelper(userRoleDAO);
  }

  @Test
  void testAuthorizeNotAuthorized() {
    assertFalse(authorizerHelper.authorize(unauthorizedUser, Resource.MEMBER));
    assertFalse(authorizerHelper.authorize(unauthorizedDuosUser, Resource.MEMBER));
  }

  @Test
  void testAuthorizeAuthorized() {
    when(userRoleDAO.findRoleNamesByUserEmail(any()))
      .thenReturn(List.of(UserRoles.CHAIRPERSON.getRoleName()));
    assertTrue(authorizerHelper.authorize(authorizedUser, Resource.CHAIRPERSON));
    assertTrue(authorizerHelper.authorize(authorizedDuosUser, Resource.CHAIRPERSON));
  }

}