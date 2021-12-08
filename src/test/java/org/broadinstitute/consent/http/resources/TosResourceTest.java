package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class TosResourceTest {

    @Mock
    private AuthUser authUser;

    @Mock
    private SamService service;

    private TosResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new TosResource(service);
    }

    @Test
    public void testGetToSText() throws Exception {
        String mockText = "Plain Text";
        when(service.getToSText()).thenReturn(mockText);
        initResource();

        Response response = resource.getToSText();
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
        assertEquals(mockText, response.getEntity().toString());
    }

}
