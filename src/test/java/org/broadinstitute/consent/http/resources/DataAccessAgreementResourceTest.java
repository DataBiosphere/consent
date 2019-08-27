package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        DatabaseResearcherAPI.class,
        HttpResponse.class
})
public class DataAccessAgreementResourceTest {

    @Mock
    GCSStore store;

    @Mock
    DatabaseResearcherAPI researcherAPI;

    @Mock
    HttpResponse response;

    private DataAccessAgreementResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(DatabaseResearcherAPI.class);
        PowerMockito.mockStatic(HttpResponse.class);
        resource = new DataAccessAgreementResource(store, researcherAPI);
    }

    @Test
    public void testGetDAA_success() throws Exception {
        Map<String, String> propMap = new HashMap<>();
        propMap.put(ResearcherFields.URL_DAA.getValue(), "gs//url/to/daa");
        propMap.put(ResearcherFields.NAME_DAA.getValue(), "daaName.txt");
        InputStream content = IOUtils.toInputStream("content", Charset.defaultCharset());
        when(researcherAPI.describeResearcherPropertiesForDAR(anyInt())).thenReturn(propMap);
        when(response.getContent()).thenReturn(content);
        when(response.getContentType()).thenReturn(MediaType.TEXT_PLAIN);
        when(store.getStorageDocument(any())).thenReturn(response);

        Response response = resource.getDAA(1);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testGetDAA_failure_case1() {
        when(researcherAPI.describeResearcherPropertiesForDAR(anyInt())).thenReturn(Collections.emptyMap());

        Response response = resource.getDAA(1);
        assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetDAA_failure_case2() throws Exception {
        Map<String, String> propMap = new HashMap<>();
        propMap.put(ResearcherFields.URL_DAA.getValue(), "gs//url/to/daa");
        propMap.put(ResearcherFields.NAME_DAA.getValue(), "daaName.txt");
        InputStream content = IOUtils.toInputStream("content", Charset.defaultCharset());
        when(researcherAPI.describeResearcherPropertiesForDAR(anyInt())).thenReturn(propMap);
        doThrow(Exception.class).when(store).getStorageDocument(anyString());

        Response response = resource.getDAA(1);
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testStoreDAA_success_case1() throws Exception {
        when(store.deleteStorageDocument(any())).thenReturn(true);
        when(store.postStorageDocument(any(), anyString(), anyString())).thenReturn("url");

        InputStream content = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "";

        Response response = resource.storeDAA(content, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testStoreDAA_success_case2() throws Exception {
        when(store.deleteStorageDocument(any())).thenReturn(false);
        when(store.postStorageDocument(any(), anyString(), anyString())).thenReturn("url");

        InputStream content = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "undefined";

        Response response = resource.storeDAA(content, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testStoreDAA_success_case3() throws Exception {
        doThrow(Exception.class).when(store).deleteStorageDocument(any());
        when(store.postStorageDocument(any(), anyString(), anyString())).thenReturn("url");

        InputStream content = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "undefined";

        Response response = resource.storeDAA(content, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void testStoreDAA_failure() throws Exception {
        doThrow(Exception.class).when(store).deleteStorageDocument(any());
        doThrow(Exception.class).when(store).postStorageDocument(any(), anyString(), anyString());

        InputStream content = IOUtils.toInputStream("content", Charset.defaultCharset());
        FormDataBodyPart part = new FormDataBodyPart();
        part.setMediaType(MediaType.TEXT_PLAIN_TYPE);
        String fileName = "test.txt";
        String existentFileUrl = "";

        Response response = resource.storeDAA(content, part, fileName, existentFileUrl);
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

}
