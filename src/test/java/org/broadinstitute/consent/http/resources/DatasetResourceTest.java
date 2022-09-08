package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
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
import java.util.Collection;
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
    private User user;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

    @Mock
    private Collection<Dictionary> dictionaries;

    private DatasetResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new DatasetResource(datasetService, userService, darService);
    }

    private String createPropertiesJson(List<DatasetPropertyDTO> properties) {
        DatasetDTO json = new DatasetDTO();
        json.setProperties(properties);
        return new Gson().toJson(json);
    }

    private String createPropertiesJson(String propertyName, String propertyValue) {
        List<DatasetPropertyDTO> jsonProperties = new ArrayList<>();
        jsonProperties.add(new DatasetPropertyDTO(propertyName, propertyValue));
        return createPropertiesJson(jsonProperties);
    }

    private DatasetDTO createMockDatasetDTO() {
        DatasetDTO mockDTO = new DatasetDTO();
        mockDTO.setDataSetId(RandomUtils.nextInt(100, 1000));
        mockDTO.setDatasetName("test");
        mockDTO.addProperty(new DatasetPropertyDTO("Property", "test"));

        return mockDTO;
    }

    @Test
    public void testCreateDatasetSuccess() throws Exception {
        DatasetDTO result = createMockDatasetDTO();
        Consent consent = new Consent();
        String json = createPropertiesJson("Dataset Name", "test");

        when(datasetService.getDatasetByName("test")).thenReturn(null);
        when(datasetService.createDatasetWithConsent(any(), any(), anyInt())).thenReturn(result);
        when(datasetService.createConsentForDataset(any())).thenReturn(consent);
        when(datasetService.getDatasetDTO(any())).thenReturn(result);
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(user.getUserId()).thenReturn(1);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("/api/dataset/1"));
        initResource();
        Response response = resource.createDataset(authUser, uriInfo, json);

        assertEquals(201, response.getStatus());
        assertEquals(result, response.getEntity());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetNoJson() {
        initResource();
        resource.createDataset(authUser, uriInfo, "");
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetNoProperties() {
        initResource();
        resource.createDataset(authUser, uriInfo, "{\"properties\":[]}");
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetNullName() {
        String json = createPropertiesJson("Dataset Name", null);

        initResource();
        resource.createDataset(authUser, uriInfo, json);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetEmptyName() {
        String json = createPropertiesJson("Dataset Name", "");

        initResource();
        resource.createDataset(authUser, uriInfo, json);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetMissingName() {
        String json = createPropertiesJson("Property", "test");

        initResource();
        resource.createDataset(authUser, uriInfo, json);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetInvalidProperty() {
        List<DatasetPropertyDTO> invalidProperties = new ArrayList<>();
        invalidProperties.add(new DatasetPropertyDTO("Invalid Property", "test"));
        when(datasetService.findInvalidProperties(any())).thenReturn(invalidProperties);

        String json = createPropertiesJson(invalidProperties);

        initResource();
        resource.createDataset(authUser, uriInfo, json);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDatasetDuplicateProperties() {
        List<DatasetPropertyDTO> duplicateProperties = new ArrayList<>();
        duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
        duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
        when(datasetService.findDuplicateProperties(any())).thenReturn(duplicateProperties);

        String json = createPropertiesJson(duplicateProperties);

        initResource();
        resource.createDataset(authUser, uriInfo, json);
    }

    @Test(expected = ClientErrorException.class)
    public void testCreateDatasetNameInUse() {
        Dataset inUse = new Dataset();
        when(datasetService.getDatasetByName("test")).thenReturn(inUse);

        String json = createPropertiesJson("Dataset Name", "test");

        initResource();
        resource.createDataset(authUser, uriInfo, json);
    }

    @Test
    public void testCreateDatasetError() throws Exception {
        Consent consent = new Consent();
        String json = createPropertiesJson("Dataset Name", "test");

        when(datasetService.getDatasetByName("test")).thenReturn(null);
        doThrow(new RuntimeException()).when(datasetService).createDatasetWithConsent(any(), any(), anyInt());
        when(datasetService.createConsentForDataset(any())).thenReturn(consent);
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(user.getUserId()).thenReturn(1);
        initResource();
        Response response = resource.createDataset(authUser, uriInfo, json);

        assertEquals(500,response.getStatus());
    }

    @Test
    public void testUpdateDatasetSuccess() {
        Dataset preexistingDataset = new Dataset();
        String json = createPropertiesJson("Dataset Name", "test");
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.of(preexistingDataset));
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(user.getUserId()).thenReturn(1);
        when(user.hasUserRole(any())).thenReturn(true);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(200, response.getStatus());
        assertEquals(Optional.of(preexistingDataset).get(), response.getEntity());
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

    @Test
    public void testUpdateDatasetInvalidProperty() {
        List<DatasetPropertyDTO> invalidProperties = new ArrayList<>();
        invalidProperties.add(new DatasetPropertyDTO("Invalid Property", "test"));
        when(datasetService.findInvalidProperties(any())).thenReturn(invalidProperties);

        Dataset preexistingDataset = new Dataset();
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        String json = createPropertiesJson(invalidProperties);

        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testUpdateDatasetDuplicateProperties() {
        List<DatasetPropertyDTO> duplicateProperties = new ArrayList<>();
        duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
        duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
        when(datasetService.findDuplicateProperties(any())).thenReturn(duplicateProperties);

        Dataset preexistingDataset = new Dataset();
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        String json = createPropertiesJson(duplicateProperties);

        initResource();
        Response response = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testUpdateDatasetNoContent() {
        Dataset preexistingDataset = new Dataset();
        String json = createPropertiesJson("Dataset Name", "test");
        when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
        when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.empty());
        when(authUser.getGoogleUser()).thenReturn(googleUser);
        when(googleUser.getEmail()).thenReturn("email@email.com");
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(user.getUserId()).thenReturn(1);
        when(user.hasUserRole(any())).thenReturn(true);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
        initResource();
        Response responseNoContent = resource.updateDataset(authUser, uriInfo, 1, json);
        assertEquals(204, responseNoContent.getStatus());
    }

    @Test
    public void testDescribeDatasetsSuccess() {
        when(authUser.getEmail()).thenReturn("authUserEmail");
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(datasetService.describeDatasets(anyInt())).thenReturn(Collections.emptySet());
        initResource();
        Response response = resource.describeDataSets(authUser);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribeDatasetsError() {
        when(authUser.getEmail()).thenReturn("authUserEmail");
        when(userService.findUserByEmail(any())).thenReturn(user);
        doThrow(new RuntimeException()).when(datasetService).describeDatasets(anyInt());
        initResource();
        Response response = resource.describeDataSets(authUser);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testValidateDatasetNameSuccess() {
        Dataset testDataset = new Dataset();
        when(datasetService.getDatasetByName("test")).thenReturn(testDataset);
        initResource();
        Response response = resource.validateDatasetName("test");
        assertEquals(200, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testValidateDatasetNameNotFound() {
        initResource();
        resource.validateDatasetName("test");
    }

    @Test
    public void testDescribeDataSetSuccess() {
        DatasetDTO testDTO = createMockDatasetDTO();
        when(datasetService.getDatasetDTO(any())).thenReturn(testDTO);
        initResource();
        Response response = resource.describeDataSet(1);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribeDataSetError() {
        doThrow(new RuntimeException()).when(datasetService).getDatasetDTO(any());
        initResource();
        Response response = resource.describeDataSet(1);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testGetDataSetSample() {
        List<String> header = List.of("attachment; filename=DataSetSample.tsv");
        initResource();
        Response response = resource.getDataSetSample();
        assertEquals(200, response.getStatus());
        assertEquals(header, response.getHeaders().get("Content-Disposition"));
    }

    @Test
    public void testDownloadDatasetsSuccess() {
        List<DatasetDTO> dtoList = new ArrayList<>();
        DatasetDTO testDTO = createMockDatasetDTO();
        dtoList.add(testDTO);

        when(datasetService.describeDataSetsByReceiveOrder(any())).thenReturn(dtoList);
        initResource();

        Response response = resource.downloadDataSets(List.of(1));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDownloadDatasetsHeaderError() {
        doThrow(new RuntimeException()).when(datasetService).describeDictionaryByReceiveOrder();
        initResource();
        Response response = resource.downloadDataSets(List.of(1));
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDownloadDatasetsEmptyList() {
        initResource();
        Response response = resource.downloadDataSets(List.of());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDownloadDatasetsServiceError() {
        doThrow(new RuntimeException()).when(datasetService).describeDataSetsByReceiveOrder(any());
        initResource();
        Response response = resource.downloadDataSets(List.of(1));
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDeleteSuccessAdmin() {
        Dataset dataSet = new Dataset();

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(true);
        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.delete(authUser, 1, null);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteSuccessChairperson() {
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);
        dataSet.setDacId(1);

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        role.setDacId(1);
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.delete(authUser, 1, null);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteErrorNoDacIds() {
        Dataset dataSet = new Dataset();

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.delete(authUser, 1, null);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteErrorNullConsent() {
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        role.setDacId(1);
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.delete(authUser, 1, null);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteErrorMismatch() {
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);
        dataSet.setDacId(2);

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        role.setDacId(1);
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.delete(authUser, 1, null);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDisableDataSetSuccessAdmin() {
        Dataset dataSet = new Dataset();

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(true);
        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.disableDataSet(authUser, 1, true, null);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDisableDataSetSuccessChairperson() {
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);
        dataSet.setDacId(1);

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        role.setDacId(1);
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.disableDataSet(authUser, 1, true, null);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDisableDataSetErrorNoDacIds() {
        Dataset dataSet = new Dataset();

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.disableDataSet(authUser, 1, true, null);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDisableDataSetErrorNullConsent() {
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        role.setDacId(1);
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.disableDataSet(authUser, 1, true, null);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDisableDataSetErrorMismatch() {
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);
        dataSet.setDacId(2);

        when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
        UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        role.setDacId(1);
        when(user.getRoles()).thenReturn(List.of(role));

        when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
        when(datasetService.findDatasetById(any())).thenReturn(dataSet);

        initResource();
        Response response = resource.disableDataSet(authUser, 1, true, null);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDescribeDictionarySuccess() {
        when(datasetService.describeDictionaryByDisplayOrder()).thenReturn(dictionaries);
        initResource();
        Response response = resource.describeDictionary();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribeDictionaryError() {
        doThrow(new RuntimeException()).when(datasetService).describeDictionaryByDisplayOrder();
        initResource();
        Response response = resource.describeDictionary();
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDatasetAutocompleteSuccess() {
        List<Map<String, String>> autocompleteMap = List.of(Collections.EMPTY_MAP);
        when(authUser.getEmail()).thenReturn("testauthuser@test.com");
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(user.getUserId()).thenReturn(0);
        when(datasetService.autoCompleteDatasets(anyString(), anyInt())).thenReturn(autocompleteMap);

        initResource();
        Response response = resource.datasetAutocomplete(authUser, "test");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDatasetAutocompleteError() {
        when(authUser.getEmail()).thenReturn("testauthuser@test.com");
        when(userService.findUserByEmail(anyString())).thenReturn(user);
        when(user.getUserId()).thenReturn(0);
        doThrow(new RuntimeException()).when(datasetService).autoCompleteDatasets(anyString(), anyInt());

        initResource();
        Response response = resource.datasetAutocomplete(authUser, "test");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUpdateNeedsReviewDataSetsSuccess() {
        Dataset dataSet = new Dataset();
        when(datasetService.updateNeedsReviewDatasets(any(), any())).thenReturn(dataSet);

        initResource();
        Response response = resource.updateNeedsReviewDataSets(1, true);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateNeedsReviewDataSetsError() {
        doThrow(new RuntimeException()).when(datasetService).updateNeedsReviewDatasets(any(), any());

        initResource();
        Response response = resource.updateNeedsReviewDataSets(1, true);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDownloadDatasetApprovedUsersSuccess() {
        List<String> header = List.of("attachment; filename=DatasetApprovedUsers.tsv");
        initResource();
        Response response = resource.downloadDatasetApprovedUsers(new AuthUser(), 1);
        assertEquals(200, response.getStatus());
        assertEquals(header, response.getHeaders().get("Content-Disposition"));
    }

    @Test
    public void testDownloadDatasetApprovedUsersError() {
        doThrow(new RuntimeException()).when(darService).getDatasetApprovedUsersContent(any(), any());
        initResource();
        Response response = resource.downloadDatasetApprovedUsers(new AuthUser(), 1);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testFindAllDatasetsAvailableToUser() {
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(datasetService.findAllDatasetsByUser(any())).thenReturn(List.of(new Dataset()));
        initResource();
        Response response = resource.findAllDatasetsAvailableToUser(authUser);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
}
