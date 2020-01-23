package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.service.AbstractDataSetAssociationAPI;
import org.broadinstitute.consent.http.service.DataSetAssociationAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skife.jdbi.v2.exceptions.StatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractDataSetAssociationAPI.class})
public class DataSetAssociationsResourceTest {

    @Mock
    DataSetAssociationAPI datasetAssociationAPI;

    private DataSetAssociationsResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDataSetAssociationAPI.class);
        when(datasetAssociationAPI.createDatasetUsersAssociation(any(), any())).thenReturn(Collections.emptyList());
        when(datasetAssociationAPI.findDataOwnersRelationWithDataset(any())).thenReturn(Collections.emptyMap());
        when(datasetAssociationAPI.updateDatasetAssociations(any(), any())).thenReturn(Collections.emptyList());
        when(AbstractDataSetAssociationAPI.getInstance()).thenReturn(datasetAssociationAPI);
    }

    private void initResource() {
        resource = new DataSetAssociationsResource();
    }

    @Test
    public void testAssociateDatasetWithUsers() {
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(201, response.getStatus());
    }

    @Test
    public void testAssociateDatasetWithUsers_NotFound() {
        when(datasetAssociationAPI.createDatasetUsersAssociation(any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testAssociateDatasetWithUsers_BadRequest() {
        when(datasetAssociationAPI.createDatasetUsersAssociation(any(), any())).thenThrow(new BadRequestException());
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
        when(datasetAssociationAPI.updateDatasetAssociations(any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.updateDatasetAssociations(RandomUtils.nextInt(1, 100), Collections.emptyList());
        Assert.assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

}
