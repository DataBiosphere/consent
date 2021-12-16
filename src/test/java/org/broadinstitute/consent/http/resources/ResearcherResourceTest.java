package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.ResearcherService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ResearcherResourceTest {

    @Mock
    ResearcherService researcherService;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    private ResearcherResource resource;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/api/researcher/"));
    }

    private void initResource() {
        resource = new ResearcherResource(researcherService);
    }

    @Test
    public void testDeleteAllProperties() {
        doNothing().when(researcherService).deleteResearcherProperties(any());
        initResource();
        Response response = resource.deleteAllProperties(1);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteAllPropertiesError() {
        doThrow(new RuntimeException()).when(researcherService).deleteResearcherProperties(any());
        initResource();
        Response response = resource.deleteAllProperties(1);
        assertEquals(500, response.getStatus());
    }
}
