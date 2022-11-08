package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.service.ElectionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ConsentElectionResourceTest {

    @Mock
    ElectionService electionService;

    @Mock
    UriInfo info;

    @Mock
    UriBuilder builder;

    private ConsentElectionResource resource;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(info.getRequestUriBuilder()).thenReturn(builder);
    }

    @Test
    public void testDeleteElection() {
        doNothing().when(electionService).deleteElection(anyInt());
        initResource();

        Response response = resource.deleteElection(UUID.randomUUID().toString(), info, RandomUtils.nextInt(1, 10));
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private void initResource() {
        resource = new ConsentElectionResource(electionService);
    }

}
