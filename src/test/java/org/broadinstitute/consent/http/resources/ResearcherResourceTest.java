package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.broadinstitute.consent.http.service.users.handler.ResearcherService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class ResearcherResourceTest {

    @Mock
    ResearcherService researcherService;

    @Mock
    UserService userService;

    @Mock
    WhitelistService whitelistService;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    private AuthUser authUser;

    private ResearcherResource resource;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        GoogleUser googleUser = new GoogleUser();
        googleUser.setName("Test User");
        googleUser.setEmail("test@gmail.com");
        authUser = new AuthUser(googleUser);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/api/researcher/"));
    }

    private void initResource() {
        resource = new ResearcherResource(researcherService, userService, whitelistService);
    }

    @Test
    public void testRegisterProperties() {
        when(researcherService.setProperties(any(), any())).thenReturn(Collections.emptyList());
        initResource();
        Response response = resource.registerProperties(authUser, uriInfo, null);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterPropertiesNotFound() {
        when(researcherService.setProperties(any(), any())).thenThrow(new NotFoundException("User Not Found"));
        initResource();
        Response response = resource.registerProperties(authUser, uriInfo, null);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterPropertiesInvalidFields() {
        when(researcherService.setProperties(any(), any())).thenThrow(new IllegalArgumentException("Fields not valid"));
        initResource();
        Response response = resource.registerProperties(authUser, uriInfo, null);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateProperties() {
        initResource();

        Response response1 = resource.updateProperties(authUser, false, null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
        Response response2 = resource.updateProperties(authUser, true, null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
    }

    @Test
    public void testUpdatePropertiesNotFound() {
        when(researcherService.updateProperties(any(), any(), any())).thenThrow(new NotFoundException("User Not Found"));
        initResource();
        Response response = resource.updateProperties(authUser, false, null);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdatePropertiesInvalidFields() {
        when(researcherService.updateProperties(any(), any(), any())).thenThrow(new IllegalArgumentException("Invalid Fields"));
        initResource();
        Response response = resource.updateProperties(authUser, false, null);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDescribeAllResearcherPropertiesAdmin() {
        when(researcherService.describeResearcherPropertiesMap(any())).thenReturn(new HashMap<>());
        DACUser authedDacUser = new DACUser();
        authedDacUser.setDacUserId(1);
        authedDacUser.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
        when(userService.findUserByEmail(anyString())).thenReturn(authedDacUser);
        initResource();

        // Request properties for self
        Response response1 = resource.describeAllResearcherProperties(authUser, 1);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());

        // Request properties for a different user id
        Response response2 = resource.describeAllResearcherProperties(authUser, 2);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
    }

    @Test
    public void testDescribeAllResearcherPropertiesChair() {
        when(researcherService.describeResearcherPropertiesMap(any())).thenReturn(new HashMap<>());
        DACUser authedDacUser = new DACUser();
        authedDacUser.setDacUserId(1);
        authedDacUser.addRole(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
        when(userService.findUserByEmail(anyString())).thenReturn(authedDacUser);
        initResource();

        // Request properties for self
        Response response1 = resource.describeAllResearcherProperties(authUser, 1);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());

        // Request properties for a different user id
        Response response2 = resource.describeAllResearcherProperties(authUser, 2);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
    }

    @Test
    public void testDescribeAllResearcherPropertiesResearcher() {
        when(researcherService.describeResearcherPropertiesMap(any())).thenReturn(new HashMap<>());
        DACUser authedDacUser = new DACUser();
        authedDacUser.setDacUserId(1);
        authedDacUser.addRole(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
        when(userService.findUserByEmail(anyString())).thenReturn(authedDacUser);
        initResource();

        // Request properties for self
        Response response1 = resource.describeAllResearcherProperties(authUser, 1);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());

        // Request properties for a different user id
        Response response2 = resource.describeAllResearcherProperties(authUser, 2);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response2.getStatus());
    }

    @Test
    public void testGetResearcherPropertiesForDARAdmin() {
        when(researcherService.describeResearcherPropertiesMap(any())).thenReturn(new HashMap<>());
        DACUser authedDacUser = new DACUser();
        authedDacUser.setDacUserId(1);
        authedDacUser.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
        when(userService.findUserByEmail(anyString())).thenReturn(authedDacUser);
        initResource();

        // Request properties for self
        Response response1 = resource.getResearcherPropertiesForDAR(authUser, 1);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());

        // Request properties for a different user id
        Response response2 = resource.getResearcherPropertiesForDAR(authUser, 2);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
    }

    @Test
    public void testGetResearcherPropertiesForDARResearcher() {
        when(researcherService.describeResearcherPropertiesMap(any())).thenReturn(new HashMap<>());
        DACUser authedDacUser = new DACUser();
        authedDacUser.setDacUserId(1);
        authedDacUser.addRole(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
        when(userService.findUserByEmail(anyString())).thenReturn(authedDacUser);
        initResource();

        // Request properties for self
        Response response1 = resource.getResearcherPropertiesForDAR(authUser, 1);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());

        // Request properties for a different user id
        Response response2 = resource.getResearcherPropertiesForDAR(authUser, 2);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response2.getStatus());
    }

}
