package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import liquibase.pro.packaged.D;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DatasetResourceTest {

    @Mock
    private ConsentService consentService;

    @Mock
    private DataAccessRequestService darService;

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

    private DatasetResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new DatasetResource(consentService, datasetService, userService, darService);
    }

    private String createPropertiesJson(List<DataSetPropertyDTO> properties) {
        DatasetDTO json = new DatasetDTO();
        json.setProperties(properties);
        return new Gson().toJson(json);
    }

    private String createPropertiesJson(String propertyName, String propertyValue) {
        List<DataSetPropertyDTO> jsonProperties = new ArrayList<>();
        jsonProperties.add(new DataSetPropertyDTO(propertyName, propertyValue));
        return createPropertiesJson(jsonProperties);
    }

    @Test
    public void testCreateDataset() throws Exception {
        DatasetDTO result = new DatasetDTO();
        Consent consent = new Consent();
        String json = createPropertiesJson("Dataset Name", "test");

        when(datasetService.getDatasetByName("test")).thenReturn(null);
        when(datasetService.createDatasetWithConsent(any(), any(), anyInt())).thenReturn(result);
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
        Response response = resource.createDataset(authUser, uriInfo, json);

        assertEquals(201,response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetNoJson() {
        initResource();
        Response response = resource.createDataset(authUser, uriInfo, "");
        assertEquals(400, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetNoProperties() {
        initResource();
        Response response = resource.createDataset(authUser, uriInfo, "{\"properties\":[]}");
        assertEquals(400, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetMissingName() {
        String json = createPropertiesJson("Dataset Name", null);
        // TODO: Rewrite createDataset to handle empty name strings and entirely missing name properties

        initResource();
        Response response = resource.createDataset(authUser, uriInfo, json);
        assertEquals(400, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetInvalidProperty() {
        List<DataSetPropertyDTO> invalidProperties = new ArrayList<>();
        invalidProperties.add(new DataSetPropertyDTO("Invalid Property", "test"));
        when(datasetService.findInvalidProperties(any())).thenReturn(invalidProperties);

        String json = createPropertiesJson(invalidProperties);

        initResource();
        Response response = resource.createDataset(authUser, uriInfo, json);
        assertEquals(400, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetDuplicateProperties() {
        List<DataSetPropertyDTO> duplicateProperties = new ArrayList<>();
        duplicateProperties.add(new DataSetPropertyDTO("Dataset Name", "test"));
        duplicateProperties.add(new DataSetPropertyDTO("Dataset Name", "test"));
        when(datasetService.findDuplicateProperties(any())).thenReturn(duplicateProperties);

        String json = createPropertiesJson(duplicateProperties);

        initResource();
        Response response = resource.createDataset(authUser, uriInfo, json);
        assertEquals(400, response.getStatus());
    }

    @Test(expected = ClientErrorException.class)
    public void testCreateDatasetNameInUse() {
        DataSet inUse = new DataSet();
        when(datasetService.getDatasetByName("test")).thenReturn(inUse);

        String json = createPropertiesJson("Dataset Name", "test");

        initResource();
        Response response = resource.createDataset(authUser, uriInfo, json);
        assertEquals(409, response.getStatus());
    }

    @Test
    public void testUpdateDataset() {
        DataSet preexistingDataset = new DataSet();
        String json = createPropertiesJson("Dataset Name", "test");
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.of(preexistingDataset));
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(dacUser.getDacUserId()).thenReturn(1);
        when(dacUser.hasUserRole(any())).thenReturn(true);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateDatasetNoJson() {
        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, "");
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testUpdateDatasetNoProperties() {
        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, "{\"properties\":[]}");
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testUpdateDatasetIdNotFound() {
        String json = createPropertiesJson("Dataset Name", "test");
        when(datasetService.findDatasetById(anyInt())).thenReturn(null);

        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(404, response.getStatus());
    }

    public void testUpdateDatasetInvalidProperty() {
        List<DataSetPropertyDTO> invalidProperties = new ArrayList<>();
        invalidProperties.add(new DataSetPropertyDTO("Invalid Property", "test"));
        when(datasetService.findInvalidProperties(any())).thenReturn(invalidProperties);

        String json = createPropertiesJson(invalidProperties);

        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(400, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDatasetDuplicateProperties() {
        List<DataSetPropertyDTO> duplicateProperties = new ArrayList<>();
        duplicateProperties.add(new DataSetPropertyDTO("Dataset Name", "test"));
        duplicateProperties.add(new DataSetPropertyDTO("Dataset Name", "test"));
        when(datasetService.findDuplicateProperties(any())).thenReturn(duplicateProperties);

        String json = createPropertiesJson(duplicateProperties);

        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testUpdateDatasetNoContent() {
        DataSet preexistingDataset = new DataSet();
        String json = createPropertiesJson("Dataset Name", "test");
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.empty());
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(dacUser.getDacUserId()).thenReturn(1);
        when(dacUser.hasUserRole(any())).thenReturn(true);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        initResource();
        Response responseNoContent = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(204, responseNoContent.getStatus());
    }

    @Test
    public void testDatasetAutocomplete() {
        List<Map<String, String>> autocompleteMap = Collections.singletonList(Collections.EMPTY_MAP);
        when(authUser.getEmail()).thenReturn("testauthuser@test.com");
        when(userService.findUserByEmail(anyString())).thenReturn(dacUser);
        when(dacUser.getDacUserId()).thenReturn(0);
        when(datasetService.autoCompleteDatasets(anyString(), anyInt())).thenReturn(autocompleteMap);

        initResource();
        Response response = resource.datasetAutocomplete(authUser, "test");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribeDatasetsSuccess() {
        when(authUser.getEmail()).thenReturn("authUserEmail");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        when(datasetService.describeDatasets(anyInt())).thenReturn(Collections.emptySet());
        initResource();
        Response response = resource.describeDataSets(authUser);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribeDatasetsError() {
        when(authUser.getEmail()).thenReturn("authUserEmail");
        when(userService.findUserByEmail(any())).thenReturn(dacUser);
        doThrow(new RuntimeException()).when(datasetService).describeDatasets(anyInt());
        initResource();
        Response response = resource.describeDataSets(authUser);
        assertEquals(500, response.getStatus());
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

}
