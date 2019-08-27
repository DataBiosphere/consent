package org.broadinstitute.consent.http.resources;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.service.users.handler.DatabaseResearcherAPI;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        DatabaseResearcherAPI.class
})
public class DataAccessAgreementResourceTest {

    @Mock
    GCSStore store;

    @Mock
    DatabaseResearcherAPI researcherAPI;

    private DataAccessAgreementResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(DatabaseResearcherAPI.class);
        resource = new DataAccessAgreementResource(store, researcherAPI);
    }

    @Test
    public void testStoreDAA_success_case1() throws Exception {
        when(store.deleteStorageDocument(any())).thenReturn(true);
        when(store.postStorageDocument(any(), anyString(), anyString())).thenReturn("url");

        InputStream uploadedDAA = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "";

        Response response = resource.storeDAA(uploadedDAA, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testStoreDAA_success_case2() throws Exception {
        when(store.deleteStorageDocument(any())).thenReturn(false);
        when(store.postStorageDocument(any(), anyString(), anyString())).thenReturn("url");

        InputStream uploadedDAA = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "undefined";

        Response response = resource.storeDAA(uploadedDAA, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testStoreDAA_success_case3() throws Exception {
        doThrow(Exception.class).when(store).deleteStorageDocument(any());
        when(store.postStorageDocument(any(), anyString(), anyString())).thenReturn("url");

        InputStream uploadedDAA = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "undefined";

        Response response = resource.storeDAA(uploadedDAA, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), 200);
    }

    @Test
    public void testStoreDAA_failure() throws Exception {
        doThrow(Exception.class).when(store).deleteStorageDocument(any());
        doThrow(Exception.class).when(store).postStorageDocument(any(), anyString(), anyString());

        InputStream uploadedDAA = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "";

        Response response = resource.storeDAA(uploadedDAA, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), 500);
    }

}
