package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class NihAccountResourceTest {
    @Mock
    private NihService nihService;

    @Mock
    private UserService userService;

    @Mock
    private NIHUserAccount nihAccount;

    @Mock
    private User user;

    @Mock
    private AuthUser authUser;

    private NihAccountResource resource;

    @Before
    public void setUp() {
        openMocks(this);
        when(nihAccount.getStatus()).thenReturn(true);
        when(nihAccount.getEraExpiration()).thenReturn("test");
    }

    @Test
    public void testRegisterResearcherSuccess() {
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new NihAccountResource(nihService, userService);
        Response response = resource.registerResearcher(nihAccount, authUser);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRegisterResearcherNoAuth() {
        when(userService.findUserByEmail(any())).thenReturn(null);
        resource = new NihAccountResource(nihService, userService);
        Response response = resource.registerResearcher(nihAccount, authUser);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testRegisterResearcherError() {
        when(userService.findUserByEmail(any())).thenReturn(user);
        doThrow(new RuntimeException()).when(nihService).authenticateNih(any(), any(), any());
        resource = new NihAccountResource(nihService, userService);
        Response response = resource.registerResearcher(nihAccount, authUser);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testRegisterResearcherNullAccountError() {
        doThrow(new BadRequestException()).when(nihService).validateNihUserAccount(any(), any());
        resource = new NihAccountResource(nihService, userService);
        Response response = resource.registerResearcher(null, authUser);
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDeleteNihAccountSuccess() {
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new NihAccountResource(nihService, userService);
        Response response = resource.deleteNihAccount(authUser);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteNihAccountNoAuth() {
        when(userService.findUserByEmail(any())).thenReturn(null);
        resource = new NihAccountResource(nihService, userService);
        Response response = resource.deleteNihAccount(authUser);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDeleteNihAccountError() {
        when(userService.findUserByEmail(any())).thenReturn(user);
        doThrow(new RuntimeException()).when(nihService).deleteNihAccountById(any());
        resource = new NihAccountResource(nihService, userService);
        Response response = resource.deleteNihAccount(authUser);
        assertEquals(500, response.getStatus());
    }
}
