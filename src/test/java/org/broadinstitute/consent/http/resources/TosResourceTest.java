package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class TosResourceTest {

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

    @Test
    public void testGetDuosToSText() throws Exception {
        String mockText = "Sample Terra ToS";
        when(service.getToSText()).thenReturn(mockText);
        initResource();

        Response response = resource.getDUOSToSText();
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
        assertTrue(response.getEntity().toString().contains("DUOS"));
    }
}
