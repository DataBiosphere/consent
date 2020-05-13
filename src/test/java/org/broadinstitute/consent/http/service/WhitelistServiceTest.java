package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.util.WhitelistCache;
import org.broadinstitute.consent.http.util.WhitelistParserTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.BadRequestException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class WhitelistServiceTest {

    @Mock
    GCSService gcsService;

    @Mock
    WhitelistCache cache;

    WhitelistService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(gcsService.postWhitelist(any(), any())).thenReturn(new GenericUrl("http://localhost:8000/whitelist.txt"));
        service = new WhitelistService(gcsService, cache);
    }

    @Test
    public void testPostWhitelist_valid() {
        String fileData = new WhitelistParserTest().makeSampleWhitelistFile(2, false);
        GenericUrl url = service.postWhitelist(fileData);
        assertNotNull(url);
    }

    @Test(expected = BadRequestException.class)
    public void testPostWhitelist_invalid() {
        String fileData = new WhitelistParserTest().makeSampleWhitelistFile(2, true);
        service.postWhitelist(fileData);
    }

    @Test
    public void testValidateWhitelist_valid() {
        String fileData = new WhitelistParserTest().makeSampleWhitelistFile(2, false);
        boolean valid = service.validateWhitelist(fileData);
        assertTrue(valid);
    }

    @Test
    public void testValidateWhitelist_invalid() {
        String fileData = new WhitelistParserTest().makeSampleWhitelistFile(2, true);
        boolean valid = service.validateWhitelist(fileData);
        assertFalse(valid);
    }

}
