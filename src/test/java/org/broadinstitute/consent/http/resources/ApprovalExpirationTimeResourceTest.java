package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ApprovalExpirationTimeService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ApprovalExpirationTimeResourceTest {

    @Mock
    private ApprovalExpirationTimeService approvalExpirationTimeService;

    @Mock
    private UriInfo uriInfo;

    @Mock
    UriBuilder uriBuilder;

    @Mock
    AuthUser authUser;

    @Mock
    UserService userService;

    @Mock
    User user;

    private ApprovalExpirationTimeResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initResource() {
        when(authUser.getName()).thenReturn("auth user name");
        when(user.getDacUserId()).thenReturn(1);
        when(user.getDisplayName()).thenReturn("display name");
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new ApprovalExpirationTimeResource(approvalExpirationTimeService, userService);
    }

    @Test
    public void testCreateApprovalExpirationTime() throws Exception {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeService.create(any())).thenReturn(approvalExpirationTime);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("/api/approvalExpirationTime/" + RandomUtils.nextInt(1, 100)));
        initResource();

        Response response = resource.createdApprovalExpirationTime(authUser, uriInfo, approvalExpirationTime);
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testDescribeApprovalExpirationTime() {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeService.findApprovalExpirationTime()).thenReturn(approvalExpirationTime);
        initResource();

        Response response = resource.describeApprovalExpirationTime();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDescribe() {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeService.findApprovalExpirationTimeById(any())).thenReturn(approvalExpirationTime);
        initResource();

        Response response = resource.describe(RandomUtils.nextInt(1, 10));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {
        ApprovalExpirationTime approvalExpirationTime = generateApprovalExpirationTime();
        when(approvalExpirationTimeService.update(any(), any())).thenReturn(approvalExpirationTime);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build(anyString())).thenReturn(new URI("/api/approvalExpirationTime/" + RandomUtils.nextInt(1, 100)));
        initResource();

        Response response = resource.update(authUser, uriInfo, approvalExpirationTime, RandomUtils.nextInt(1, 10));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDelete() {
        doNothing().when(approvalExpirationTimeService).deleteApprovalExpirationTime(any());
        initResource();

        Response response = resource.delete(RandomUtils.nextInt(1, 10));
        assertEquals(200, response.getStatus());
    }

    private ApprovalExpirationTime generateApprovalExpirationTime() {
        ApprovalExpirationTime approvalExpirationTime = new ApprovalExpirationTime();
        approvalExpirationTime.setAmountOfDays(7);
        approvalExpirationTime.setUserId(3333);
        return approvalExpirationTime;
    }

}
