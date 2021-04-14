package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatasetResourceTest {

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
        MockitoAnnotations.initMocks(this);
    }

    private void initResource() {
        resource = new DatasetResource(datasetService, userService, darService);
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
    public void testDatasetAutocomplete() {
        List<Map<String, String>> autocompleteMap = Collections.singletonList(Collections.EMPTY_MAP);
        when(authUser.getName()).thenReturn("testauthuser@test.com");
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

}
