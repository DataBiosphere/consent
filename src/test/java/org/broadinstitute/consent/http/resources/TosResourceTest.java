package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class TosResourceTest {

    @Mock
    private SamService service;

    private TosResource resource;

    @BeforeEach
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
