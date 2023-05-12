package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import java.util.Collections;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DatasetAssociationsResourceTest {

    @Mock
    private DatasetAssociationService service;

    private DatasetAssociationsResource resource;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        when(service.createDatasetUsersAssociation(any(), any())).thenReturn(Collections.emptyList());
        when(service.findDataOwnersRelationWithDataset(any())).thenReturn(Collections.emptyMap());
        when(service.updateDatasetAssociations(any(), any())).thenReturn(Collections.emptyList());
    }

    private void initResource() {
        resource = new DatasetAssociationsResource(service);
    }

    @Test
    public void testAssociateDatasetWithUsers() {
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testAssociateDatasetWithUsers_NotFound() {
        when(service.createDatasetUsersAssociation(any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testAssociateDatasetWithUsers_BadRequest() {
        when(service.createDatasetUsersAssociation(any(), any())).thenThrow(new BadRequestException());
        initResource();
        Response response = resource.associateDatasetWithUsers(RandomUtils.nextInt(1, 100), Collections.emptyList());
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testGetDatasetAssociations() {
        initResource();
        Response response = resource.getDatasetAssociations(RandomUtils.nextInt(1, 100));
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testUpdateDatasetAssociations() {
        initResource();
        Response response = resource.updateDatasetAssociations(RandomUtils.nextInt(1, 100), Collections.emptyList());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testUpdateDatasetAssociations_Notfound() {
        when(service.updateDatasetAssociations(any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.updateDatasetAssociations(RandomUtils.nextInt(1, 100), Collections.emptyList());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

}
