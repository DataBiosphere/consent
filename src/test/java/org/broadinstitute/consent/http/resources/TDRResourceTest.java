package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.models.AuthUser;
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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

    private TDRResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new TDRResource(tdrService, datasetService, userService, darService);
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

    @Test
    public void testCreateDraftDataAccessRequest() {

        String identifiers = "DUOS-00001, DUOS-00002";

        Dataset d1 = new Dataset();
        d1.setDataSetId(1);

        Dataset d2 = new Dataset();
        d2.setDataSetId(2);

        when(datasetService.findDatasetByIdentifier("DUOS-00001")).thenReturn(d1);
        when(datasetService.findDatasetByIdentifier("DUOS-00002")).thenReturn(d2);

        initResource();

        Response r = resource.createDraftDataAccessRequest(new AuthUser(), uriInfo, identifiers, "New Project");

        assertEquals(201, r.getStatus());
    }


    @Test
    public void testCreateDraftDataAccessRequest500() {
        String identifiers = "DUOS-00001, DUOS-00002";

        when(datasetService.findDatasetByIdentifier("DUOS-00001")).thenReturn(null);
        when(datasetService.findDatasetByIdentifier("DUOS-00002")).thenReturn(null);

        initResource();

        Response r = resource.createDraftDataAccessRequest(new AuthUser(), uriInfo, identifiers, "New Project");

        assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, r.getStatus());
    }

    @Test
    public void testFindOrCreateUser() {
        initResource();

        User tdrUser = resource.findOrCreateUser(new AuthUser());
        assertNotNull(tdrUser);

        doThrow(new NotFoundException()).when(userService).findUserByEmail(any());
        User newTdrUser = resource.findOrCreateUser(any());
        assertNotNull(newTdrUser);
    }
}
