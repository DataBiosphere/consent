package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import java.util.Arrays;
import java.util.Collections;
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
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({
        AbstractDataAccessRequestAPI.class,
        AbstractConsentAPI.class,
        AbstractMatchProcessAPI.class,
        AbstractUseRestrictionValidatorAPI.class,
        AbstractDACUserAPI.class,
        AbstractElectionAPI.class
})
public class DataAccessRequestResourceTest {

    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private DACUserAPI dacUserAPI;
    @Mock
    private ElectionAPI electionAPI;
    @Mock
    private EmailNotifierService emailNotifierService;
    @Mock
    private ConsentAPI consentAPI;
    @Mock
    private DataAccessRequestAPI dataAccessRequestAPI;
    @Mock
    private UserService userService;
    @Mock
    private AuthUser authUser;
    @Mock
    private User user;

    private DataAccessRequestResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDataAccessRequestAPI.class);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractMatchProcessAPI.class);
        PowerMockito.mockStatic(AbstractUseRestrictionValidatorAPI.class);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        PowerMockito.mockStatic(AbstractElectionAPI.class);
    }

    /**
     * Positive case where a DAR references a numeric dataset id
     */
    @Test
    public void testDescribeConsentForDarCase1() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(user.getDacUserId()).thenReturn(dar.getUserId());
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService, emailNotifierService, userService);
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
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(user.getDacUserId()).thenReturn(dar.getUserId());
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService, emailNotifierService, userService);
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
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(null);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        resource = new DataAccessRequestResource(dataAccessRequestService, emailNotifierService, userService);
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
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        resource = new DataAccessRequestResource(dataAccessRequestService, emailNotifierService, userService);
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
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(dataAccessRequestAPI);
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(user.getDacUserId()).thenReturn(dar.getUserId() + 1);
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(dataAccessRequestService, emailNotifierService, userService);
        resource.describeConsentForDAR(authUser, dar.getReferenceId());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2() {
        DataAccessRequest dar = generateDataAccessRequest();
        DataAccessRequestManage manage = new DataAccessRequestManage();
        manage.setDar(dar);
        when(dataAccessRequestService.describeDataAccessRequestManageV2(any()))
            .thenReturn(Collections.singletonList(manage));
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            emailNotifierService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser);
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
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
