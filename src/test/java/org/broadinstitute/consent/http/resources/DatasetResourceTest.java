package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.ResourceHelpers;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ParseResult;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractDataSetAPI.class,
        AbstractDataAccessRequestAPI.class
})
public class DatasetResourceTest {

    private final static String WRONG_EXT = "dataset/wrongExt.pdf";
    private final static String MISSING_HEADER = "dataset/missingHeader.txt";
    private final static String CORRECT_FILE = "dataset/correctFile.txt";
    private final static String WRONG_IDENTIFIERS = "dataset/wrongIdentifiers.txt";

    @Mock
    private DataSetAPI api;

    @Mock
    private DataAccessRequestAPI dataAccessRequestAPI;

    @Mock
    private DatasetService datasetService;

    @Mock
    private UserService userService;

    private DataSetResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDataSetAPI.class);
        PowerMockito.mockStatic(AbstractDataAccessRequestAPI.class);
    }

    private void initResource() {
        when(AbstractDataSetAPI.getInstance()).thenReturn(api);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        resource = new DataSetResource(datasetService, userService);
    }

    @Test
    public void testCreateDataSetWrongType() throws Exception {
        File file = new File(ResourceHelpers.resourceFilePath(WRONG_EXT));
        MultiPart mp = createFormData(file);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data", file, MediaType.valueOf("text/plain"));
        InputStream is = IOUtils.toInputStream(mp.toString(), Charset.defaultCharset());
        ParseResult result = new ParseResult();
        result.setDatasets(Collections.emptyList());
        result.setErrors(Collections.singletonList("Error!"));
        when(api.overwrite(any(), any())).thenReturn(result);
        when(api.create(any(), any())).thenReturn(result);

        initResource();
        Response response = resource.createDataSet(is, fileDataBodyPart, 1, false);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateMissingHeaders() throws Exception {
        File file = new File(ResourceHelpers.resourceFilePath(MISSING_HEADER));
        MultiPart mp = createFormData(file);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data", file, MediaType.valueOf("text/plain"));
        InputStream is = IOUtils.toInputStream(mp.toString(), Charset.defaultCharset());
        ParseResult result = new ParseResult();
        result.setDatasets(Collections.emptyList());
        result.setErrors(Collections.singletonList("Error!"));
        when(api.overwrite(any(), any())).thenReturn(result);
        when(api.create(any(), any())).thenReturn(result);

        initResource();
        Response response = resource.createDataSet(is, fileDataBodyPart, 1, false);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateConsentIdAlreadyExists() throws Exception {
        File file = new File(ResourceHelpers.resourceFilePath(WRONG_IDENTIFIERS));
        MultiPart mp = createFormData(file);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data", file, MediaType.valueOf("text/plain"));
        InputStream is = IOUtils.toInputStream(mp.toString(), Charset.defaultCharset());
        ParseResult result = new ParseResult();
        result.setDatasets(Collections.emptyList());
        result.setErrors(Collections.singletonList("Error!"));
        when(api.overwrite(any(), any())).thenReturn(result);
        when(api.create(any(), any())).thenReturn(result);

        initResource();
        Response response = resource.createDataSet(is, fileDataBodyPart, 1, false);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCreateDataSet() throws IOException {
        File file = new File(ResourceHelpers.resourceFilePath(CORRECT_FILE));
        MultiPart mp = createFormData(file);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data", file, MediaType.valueOf("text/plain"));
        InputStream is = IOUtils.toInputStream(mp.toString(), Charset.defaultCharset());
        ParseResult result = new ParseResult();
        result.setDatasets(Collections.emptyList());
        result.setErrors(Collections.emptyList());
        when(api.overwrite(any(), any())).thenReturn(result);
        when(api.create(any(), any())).thenReturn(result);

        initResource();
        Response response = resource.createDataSet(is, fileDataBodyPart, 1, false);
        assertEquals(200, response.getStatus());
    }

    private MultiPart createFormData(File file) {
        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data", file, MediaType.valueOf("text/plain"));
        multiPart.bodyPart(fileDataBodyPart);
        return multiPart;
    }

}
