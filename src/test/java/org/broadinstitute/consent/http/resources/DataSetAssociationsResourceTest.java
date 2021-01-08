package org.broadinstitute.consent.http.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import java.util.Collections;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataSetAssociationsResourceTest {

    @Mock
    private DatasetAssociationService service;

    private DataSetAssociationsResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(service.createDatasetUsersAssociation(any(), any())).thenReturn(Collections.emptyList());
        when(service.findDataOwnersRelationWithDataset(any())).thenReturn(Collections.emptyMap());
        when(service.updateDatasetAssociations(any(), any())).thenReturn(Collections.emptyList());
    }

    private void initResource() {
        resource = new DataSetAssociationsResource(service);
    }

    @Test
    public void testAssociateDatasetWithUsers() {
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(201, response.getStatus());
    }

    @Test
    public void testAssociateDatasetWithUsers_NotFound() {
        when(service.createDatasetUsersAssociation(any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testAssociateDatasetWithUsers_BadRequest() {
        when(service.createDatasetUsersAssociation(any(), any())).thenThrow(new BadRequestException());
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testGetDatasetAssociations() {
        initResource();
        Response response = resource.getDatasetAssociations(RandomUtils.nextInt(1, 100));
        Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testUpdateDatasetAssociations() {
        initResource();
        Response response = resource.updateDatasetAssociations(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testUpdateDatasetAssociations_Notfound() {
        when(service.updateDatasetAssociations(any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.updateDatasetAssociations(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

}
