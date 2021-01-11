package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import io.dropwizard.testing.ResourceHelpers;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
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

    @Mock
    private AuthUser authUser;

    @Mock
    private GoogleUser googleUser;

    @Mock
    private User dacUser;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

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
    public void testCreateDataset() throws Exception {
        DataSetDTO result = new DataSetDTO();
        DataSetDTO json = new DataSetDTO();
        Consent consent = new Consent();
        List<DataSetPropertyDTO> jsonProperties = new ArrayList<>();
        jsonProperties.add(new DataSetPropertyDTO("Dataset Name", "test"));
        json.setProperties(jsonProperties);

        when(datasetService.getDatasetByName("test")).thenReturn(null);
        when(datasetService.createDataset(any(), any(), anyInt())).thenReturn(result);
        when(datasetService.createConsentForDataset(any())).thenReturn(consent);
        when(datasetService.getDatasetDTO(any())).thenReturn(result);
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(dacUser.getDacUserId()).thenReturn(1);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("/api/dataset/1"));
        initResource();
        Response response = resource.createDataset(authUser, uriInfo, new Gson().toJson(json));

        assertEquals(201,response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetErrors() {
        DataSet inUse = new DataSet();
        when(datasetService.getDatasetByName("test")).thenReturn(inUse);

        initResource();
        Response responseNoJson = resource.createDataset(authUser, uriInfo, "");
        assertEquals(400, responseNoJson.getStatus());

        Response responseNoProperties = resource.createDataset(authUser, uriInfo, "{\"properties\":[]}");
        assertEquals(400, responseNoProperties.getStatus());

        Response responseMissingDatasetName = resource.createDataset(authUser, uriInfo,
              "{\"properties\":[{\"propertyName\":\"Species\",\"propertyValue\":\"test\"}]}");
        assertEquals(400, responseMissingDatasetName.getStatus());

        Response responseInvalidProperty = resource.createDataset(authUser, uriInfo,
              "{\"properties\":[{\"propertyName\":\"Invalid Property\",\"propertyValue\":\"test\"}]");
        assertEquals(400, responseInvalidProperty.getStatus());

        Response responseDuplicateProperties = resource.createDataset(authUser, uriInfo,
              "{\"properties\":[{\"propertyName\":\"Dataset Name\",\"propertyValue\":\"test\"},{\"propertyName\":\"Dataset Name\",\"propertyValue\":\"test\"}]}");
        assertEquals(400, responseDuplicateProperties.getStatus());

        Response responseNameInUse = resource.createDataset(authUser, uriInfo,
              "{\"properties\":[{\"propertyName\":\"Dataset Name\",\"propertyValue\":\"test\"}]}");
        assertEquals(409, responseNameInUse.getStatus());
    }

    @Test
    public void testUpdateDataset() {
        DataSet preexistingDataset = new DataSet();
        DataSetDTO json = new DataSetDTO();
        List<DataSetPropertyDTO> jsonProperties = new ArrayList<>();
        jsonProperties.add(new DataSetPropertyDTO("Dataset Name", "test"));
        json.setProperties(jsonProperties);
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.of(preexistingDataset));
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(dacUser.getDacUserId()).thenReturn(1);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, new Gson().toJson(json));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateDatasetNoContent() {
        DataSet preexistingDataset = new DataSet();
        DataSetDTO json = new DataSetDTO();
        List<DataSetPropertyDTO> jsonProperties = new ArrayList<>();
        jsonProperties.add(new DataSetPropertyDTO("Dataset Name", "test"));
        json.setProperties(jsonProperties);
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.empty());
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(dacUser.getDacUserId()).thenReturn(1);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        initResource();
        Response responseNoContent = resource.updateDataset(authUser, uriInfo, 1, new Gson().toJson(json));
        assertEquals(204, responseNoContent.getStatus());
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

    @Test
    public void testDatasetAutocomplete() {
        List<Map<String, String>> autocompleteMap = Collections.singletonList(Collections.EMPTY_MAP);
        when(userService.findUserByEmail(anyString())).thenReturn(dacUser);
        when(dacUser.getDacUserId()).thenReturn(0);
        when(datasetService.autoCompleteDatasets(anyString(), anyInt())).thenReturn(autocompleteMap);

        initResource();
        Response response = resource.datasetAutocomplete(authUser, "test");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribeDatasetsSuccess() {
        when(authUser.getName()).thenReturn("authUserEmail");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(datasetService.describeDatasets(anyInt())).thenReturn(Collections.emptySet());
        initResource();
        Response response = resource.describeDataSets(authUser);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testValidateDatasetNameSuccess() {
        DataSet testDataset = new DataSet();
        testDataset.setDataSetId(1);
        when(datasetService.getDatasetByName("test")).thenReturn(testDataset);
        initResource();
        Response response = resource.validateDatasetName("test");
        assertEquals(200, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testValidateDatasetNameNotFound() {
        initResource();
        Response response = resource.validateDatasetName("test");
        assertEquals(404, response.getStatus());
    }

    private MultiPart createFormData(File file) {
        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data", file, MediaType.valueOf("text/plain"));
        multiPart.bodyPart(fileDataBodyPart);
        return multiPart;
    }

}
