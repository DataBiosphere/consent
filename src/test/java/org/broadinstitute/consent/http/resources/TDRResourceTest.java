package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.TDRService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class TDRResourceTest {

    @Mock
    private TDRService tdrService;

    @Mock
    private DatasetService datasetService;

    private TDRResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new TDRResource(tdrService, datasetService);
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

}
