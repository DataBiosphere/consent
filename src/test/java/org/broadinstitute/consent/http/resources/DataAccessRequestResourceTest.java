package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataAccessRequestResourceTest {

    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private ConsentService consentService;
    @Mock
    private UserService userService;
    @Mock
    private AuthUser authUser;
    @Mock
    private User user;

    private DataAccessRequestResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    //TODO: Test getDataAccessRequestModalSummary using WithLogHandler interface

    /**
     * Positive case where a DAR references a numeric dataset id
     */
    @Test
    public void testDescribeConsentForDarCase1() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findDatasetIdsByReferenceId(any())).thenReturn(List.of(1));
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(user.getDacUserId()).thenReturn(dar.getUserId());
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService);
        Consent consent = resource.describeConsentForDAR(authUser, dar.getReferenceId());
        assertNotNull(consent);
    }

    /**
     * Positive case where a DAR references a string dataset id
     */
    @Test
    public void testDescribeConsentForDarCase2() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findDatasetIdsByReferenceId(any())).thenReturn(List.of(1));
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(user.getDacUserId()).thenReturn(dar.getUserId());
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService);
        Consent consent = resource.describeConsentForDAR(authUser, dar.getReferenceId());
        assertNotNull(consent);
    }

    /**
     * Negative case where a DAR references an invalid dataset id
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeConsentForDarCase3() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(null);
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService);
        resource.describeConsentForDAR(authUser, dar.getReferenceId());
    }

    /**
     * Negative case where a DAR does not reference a dataset id
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeConsentForDarCase4() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService);
        resource.describeConsentForDAR(authUser, dar.getReferenceId());
    }

    /**
     * Negative case where user does not have access
     */
    @Test(expected = ForbiddenException.class)
    public void testDescribeConsentForDarCase5() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        Dataset dataSet = new Dataset();
        dataSet.setDataSetId(1);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(user.getDacUserId()).thenReturn(dar.getUserId() + 1);
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService,userService, consentService);
        resource.describeConsentForDAR(authUser, dar.getReferenceId());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2() {
        User user = new User();
        user.setRoles(Collections.singletonList(new UserRole(4, UserRoles.ADMIN.getRoleName())));
        DataAccessRequest dar = generateDataAccessRequest();
        DataAccessRequestManage manage = new DataAccessRequestManage();
        manage.setDar(dar);
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(dataAccessRequestService.describeDataAccessRequestManageV2(any(), any()))
            .thenReturn(Collections.singletonList(manage));
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService,
            consentService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.empty());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_WithRole() {
        User researcher = new User();
        researcher.setDacUserId(1);
        researcher.setRoles(Arrays.asList(new UserRole(5, UserRoles.RESEARCHER.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(researcher);
        DataAccessRequest dar = generateDataAccessRequest();
        DataAccessRequestManage manage = new DataAccessRequestManage();
        manage.setDar(dar);
        when(dataAccessRequestService.describeDataAccessRequestManageV2(any(), any()))
            .thenReturn(Collections.singletonList(manage));
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService,
            consentService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("Researcher"));
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UserNotFound() {
        when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService,
          consentService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.empty());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UserMissingRole() {
        when(userService.findUserByEmail(any())).thenReturn(new User());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService,
          consentService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("SigningOfficial"));
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UnsupportedRoleName() {
        when(userService.findUserByEmail(any())).thenReturn(new User());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService,
          consentService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("Member"));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_InvalidRoleName() {
        when(userService.findUserByEmail(any())).thenReturn(new User());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService,
          consentService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("BadRequest"));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }


    private DataAccessRequest generateDataAccessRequest() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        dataAccessRequestService.insertDARDatasetRelation(dar.getReferenceId(), 1);
        dar.setData(data);
        dar.setUserId(1);
        return dar;
    }

}
