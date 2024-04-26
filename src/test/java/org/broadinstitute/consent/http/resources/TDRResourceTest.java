package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TDRResourceTest {

  @Mock
  private TDRService tdrService;

  @Mock
  private DatasetService datasetService;

  @Mock
  private UserService userService;

  @Mock
  private DataAccessRequestService darService;

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date());

  private TDRResource resource;

  private void initResource() {
    try {
      resource = new TDRResource(tdrService, datasetService, userService, darService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
  }

  @Test
  void testGetApprovedUsersForDataset() {
    String ds = "DUOS-00003";
    List<ApprovedUser> users = List.of(
        new ApprovedUser("asdf1@gmail.com"),
        new ApprovedUser("asdf2@gmail.com"));
    ApprovedUsers approvedUsers = new ApprovedUsers(users);

    Dataset d = new Dataset();

    when(tdrService.getApprovedUsersForDataset(any(), any())).thenReturn(approvedUsers);
    when(datasetService.findDatasetByIdentifier(ds)).thenReturn(d);

    initResource();

    Response r = resource.getApprovedUsers(new AuthUser(), ds);
    assertEquals(200, r.getStatus());
    assertEquals(approvedUsers, r.getEntity());
  }

  @Test
  void testGetApprovedUsersForDataset404() {
    when(datasetService.findDatasetByIdentifier("DUOS-00003")).thenReturn(null);

    initResource();

    Response r = resource.getApprovedUsers(new AuthUser(), "DUOS-00003");

    assertEquals(404, r.getStatus());
  }

  @Test
  void testGetDatasetByIdentifier() {

    Dataset d = new Dataset();
    d.setName("test");

    when(datasetService.findDatasetByIdentifier("DUOS-00003")).thenReturn(d);

    initResource();

    Response r = resource.getDatasetByIdentifier(new AuthUser(), "DUOS-00003");

    assertEquals(200, r.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(d), r.getEntity());
  }


  @Test
  void testGetDatasetByIdentifier404() {
    when(datasetService.findDatasetByIdentifier("DUOS-00003")).thenReturn(null);

    initResource();

    Response r = resource.getDatasetByIdentifier(new AuthUser(), "DUOS-00003");

    assertEquals(404, r.getStatus());
  }

  // Created response when a new DAR draft is successful
  @Test
  void testCreateDraftDataAccessRequest() throws Exception {
    String identifiers = "DUOS-00001, DUOS-00002";
    List<Integer> identifierList = Arrays.stream(identifiers.split(","))
        .map(String::trim)
        .filter(identifier -> !identifier.isBlank())
        .map(Dataset::parseIdentifierToAlias)
        .toList();

    Dataset d1 = new Dataset();
    d1.setDataSetId(1);
    d1.setAlias(1);

    Dataset d2 = new Dataset();
    d2.setDataSetId(2);
    d2.setAlias(2);

    DataAccessRequest newDar = generateDataAccessRequest();

    when(userService.findOrCreateUser(any())).thenReturn(user);
    when(tdrService.getDatasetsByIdentifier(identifierList)).thenReturn(List.of(d1, d2));
    when(darService.insertDraftDataAccessRequest(any(), any())).thenReturn(newDar);

    initResource();

    String expectedUri = "api/dar/v2/" + newDar.getReferenceId();

    Response r = resource.createDraftDataAccessRequest(authUser, identifiers, "New Project");
    assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
    assertEquals(r.getLocation().toString(), expectedUri);
  }

  // Bad Request response (400) when no identifiers are provided
  @Test
  void testCreateDraftDataAccessRequestNoIdentifiers() throws Exception {
    when(userService.findOrCreateUser(any())).thenReturn(user);

    initResource();

    Response r = resource.createDraftDataAccessRequest(authUser, null, null);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
  }

  // Not Found response (404) with list of invalid identifiers if any do not match to a dataset
  @Test
  void testCreateDraftDataAccessRequestInvalidIdentifiers() throws Exception {
    String identifiers = "DUOS-00001, DUOS-00002";
    List<Integer> identifierList = Arrays.stream(identifiers.split(","))
        .map(String::trim)
        .filter(identifier -> !identifier.isBlank())
        .map(Dataset::parseIdentifierToAlias)
        .toList();

    Dataset d1 = new Dataset();
    d1.setDataSetId(1);
    d1.setAlias(1);

    when(userService.findOrCreateUser(any())).thenReturn(user);
    when(tdrService.getDatasetsByIdentifier(identifierList)).thenReturn(List.of(d1));

    initResource();

    Response r = resource.createDraftDataAccessRequest(authUser, identifiers, "New Project");
    assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
    Error notFoundError = (Error) r.getEntity();
    assertEquals("Invalid dataset identifiers were provided: [DUOS-00002]",
        notFoundError.message());
  }

  private DataAccessRequest generateDataAccessRequest() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setReferenceId(UUID.randomUUID().toString());
    data.setReferenceId(dar.getReferenceId());
    dar.setDatasetIds(Arrays.asList(1, 2));
    dar.setData(data);
    dar.setUserId(user.getUserId());
    return dar;
  }
}
