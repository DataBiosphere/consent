package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.models.AuthUser;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class ResearcherResourceTest {

    @Mock
    ResearcherService researcherService;

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
        resource = new ResearcherResource(researcherService);
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

}
