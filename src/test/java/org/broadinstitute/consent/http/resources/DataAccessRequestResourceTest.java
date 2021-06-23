package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataAccessRequestResourceTest {

    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private ElectionService electionService;
    @Mock
    private EmailNotifierService emailNotifierService;
    @Mock
    private ConsentService consentService;
    @Mock
    private UserService userService;
    @Mock
    private MatchService matchService;
    @Mock
    private AuthUser authUser;
    @Mock
    private User user;

    private DataAccessRequestResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Positive case where a DAR references a numeric dataset id
     */
    @Test
    public void testDescribeConsentForDarCase1() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(user.getDacUserId()).thenReturn(dar.getUserId());
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService, electionService);
        Consent consent = resource.describeConsentForDAR(authUser, dar.getReferenceId());
        assertNotNull(consent);
    }

    /**
     * Positive case where a DAR references a string dataset id
     */
    @Test
    public void testDescribeConsentForDarCase2() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        DataSet dataSet = new DataSet();
        dataSet.setDataSetId(1);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(user.getDacUserId()).thenReturn(dar.getUserId());
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService, electionService);
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
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService, electionService);
        resource.describeConsentForDAR(authUser, dar.getReferenceId());
    }

    /**
     * Negative case where a DAR does not reference a dataset id
     */
    @Test(expected = NotFoundException.class)
    public void testDescribeConsentForDarCase4() {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.getData().setDatasetIds(null);
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        resource = new DataAccessRequestResource(dataAccessRequestService, userService, consentService, electionService);
        resource.describeConsentForDAR(authUser, dar.getReferenceId());
    }

    /**
     * Negative case where user does not have access
     */
    @Test(expected = ForbiddenException.class)
    public void testDescribeConsentForDarCase5() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        DataSet dataSet = new DataSet();
        dataSet.setDataSetId(1);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(user.getDacUserId()).thenReturn(dar.getUserId() + 1);
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService,userService, consentService, electionService);
        resource.describeConsentForDAR(authUser, dar.getReferenceId());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2() {
        DataAccessRequest dar = generateDataAccessRequest();
        DataAccessRequestManage manage = new DataAccessRequestManage();
        manage.setDar(dar);
        when(dataAccessRequestService.describeDataAccessRequestManageV2(any(), any()))
            .thenReturn(Collections.singletonList(manage));
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService,
            consentService, electionService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.empty());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UserNotFound() {
        when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService,
          consentService, electionService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.empty());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UserMissingRole() {
        when(userService.findUserByEmail(any())).thenReturn(new User());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService,
          consentService, electionService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("SigningOfficial"));
        assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }

    private DataAccessRequest generateDataAccessRequest() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        data.setDatasetIds(Arrays.asList(1, 2));
        dar.setData(data);
        dar.setUserId(1);
        return dar;
    }

}
