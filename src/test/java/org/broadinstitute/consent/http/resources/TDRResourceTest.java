package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class TDRResourceTest {

    @Mock
    private TDRService tdrService;

    @Mock
    private DatasetService datasetService;

    @Mock
    private UserService userService;

    @Mock
    private DataAccessRequestService darService;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder builder;

    private final AuthUser authUser = new AuthUser("test@test.com");
    private final User user = new User(1, authUser.getEmail(), "Display Name", new Date());

    private TDRResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
      try {
        when(builder.path(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(uriInfo.getRequestUriBuilder()).thenReturn(builder);
        resource = new TDRResource(tdrService, datasetService, userService, darService);
      } catch (Exception e) {
        fail("Initialization Exception: " + e.getMessage());
      }
    }

    @Test
    public void testGetApprovedUsersForDataset() {
        List<ApprovedUser> users = List.of(
                new ApprovedUser("asdf1@gmail.com"),
                new ApprovedUser("asdf2@gmail.com"));
        ApprovedUsers approvedUsers = new ApprovedUsers(users);

        Dataset d = new Dataset();


        when(tdrService.getApprovedUsersForDataset(d)).thenReturn(approvedUsers);
        when(datasetService.findDatasetByIdentifier("DUOS-00003")).thenReturn(d);

        initResource();

        Response r = resource.getApprovedUsers(new AuthUser(), "DUOS-00003");

        assertEquals(200, r.getStatus());
        assertEquals(approvedUsers, r.getEntity());
    }

    @Test
    public void testGetApprovedUsersForDataset404() {
        when(datasetService.findDatasetByIdentifier("DUOS-00003")).thenReturn(null);

        initResource();

        Response r = resource.getApprovedUsers(new AuthUser(), "DUOS-00003");

        assertEquals(404, r.getStatus());
    }

    @Test
    public void testGetDatasetByIdentifier() {

        Dataset d = new Dataset();
        d.setName("test");


        when(datasetService.findDatasetByIdentifier("DUOS-00003")).thenReturn(d);

        initResource();

        Response r = resource.getDatasetByIdentifier(new AuthUser(), "DUOS-00003");

        assertEquals(200, r.getStatus());
        assertEquals(d, r.getEntity());
    }


    @Test
    public void testGetDatasetByIdentifier404() {
        when(datasetService.findDatasetByIdentifier("DUOS-00003")).thenReturn(null);

        initResource();

        Response r = resource.getDatasetByIdentifier(new AuthUser(), "DUOS-00003");

        assertEquals(404, r.getStatus());
    }

    // there are two service class methods you would need to mock out
    // userService.findUserByEmail and userService.createUser
    // To do that, look at usages of mockito spy and verify in the current codebase.
    @Test
    public void testCreateDraftDataAccessRequest() {

        String identifiers = "DUOS-00001,DUOS-00002";

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);

        Dataset d2 = new Dataset();
        d2.setDataSetId(2);

        DataAccessRequest newDar = generateDataAccessRequest();

        when(userService.findOrCreateUser(any())).thenReturn(user);
        when(datasetService.findDatasetByIdentifier("DUOS-00001")).thenReturn(d1);
        when(datasetService.findDatasetByIdentifier("DUOS-00002")).thenReturn(d2);
        when(darService.insertDraftDataAccessRequest(any(), any())).thenReturn(newDar);

        // uriInfo for path and build are mocked in initResource
        initResource();

        Response r = resource.createDraftDataAccessRequest(authUser, uriInfo, identifiers, "New Project");

        assertEquals(201, r.getStatus());
    }

    // todo: test case for partial list of identifiers
    @Test
    public void testCreateDraftDataAccessRequestInvalidIdentifiers() {
        String identifiers = "DUOS-00001,DUOS-00002";

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);

        when(datasetService.findDatasetByIdentifier("DUOS-00001")).thenReturn(d1);
        when(datasetService.findDatasetByIdentifier("DUOS-00002")).thenReturn(null);

        initResource();

        Response r = resource.createDraftDataAccessRequest(new AuthUser(), uriInfo, identifiers, "New Project");

        assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, r.getStatus());
    }

    private DataAccessRequest generateDataAccessRequest() {
        Timestamp now = new Timestamp(new Date().getTime());
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        dar.setDatasetIds(Arrays.asList(1, 2));
        dar.setData(data);
        dar.setUserId(user.getUserId());
        dar.setCreateDate(now);
        dar.setUpdateDate(now);
        dar.setSortDate(now);
        return dar;
    }
}
