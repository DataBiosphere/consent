package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class WhitelistServiceTest {

    @Mock
    GCSStore gcsStore;

    WhitelistService service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(gcsStore.postWhitelist(any(), any())).thenReturn(new GenericUrl("http://localhost:8000/whitelist.txt"));
        service = new WhitelistService(gcsStore);
    }

    @Test
    public void testPostWhitelist() throws Exception {
        GenericUrl url = service.postWhitelist("file data");
        assertNotNull(url);
    }

}
