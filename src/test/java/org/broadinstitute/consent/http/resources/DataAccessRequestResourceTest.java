package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataAccessRequestResourceTest {

    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private UserService userService;
    @Mock
    private AuthUser authUser;

    private DataAccessRequestResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_NoRoleWithAccess() {
        User so = new User();
        so.setUserId(1);
        so.setRoles(List.of(new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(so);
        DataAccessRequest dar = generateDataAccessRequest();
        DataAccessRequestManage manage = new DataAccessRequestManage();
        manage.setDar(dar);
        when(dataAccessRequestService.describeDataAccessRequestManageV2(any(), any()))
            .thenReturn(Collections.singletonList(manage));
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.empty());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_NoRoleNoAccess() {
        User user = new User();
        user.setRoles(Collections.singletonList(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(user);
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.empty());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_WithRole() {
        User so = new User();
        so.setUserId(1);
        so.setRoles(List.of(new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(so);
        DataAccessRequest dar = generateDataAccessRequest();
        DataAccessRequestManage manage = new DataAccessRequestManage();
        manage.setDar(dar);
        when(dataAccessRequestService.describeDataAccessRequestManageV2(any(), any()))
            .thenReturn(Collections.singletonList(manage));
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of(UserRoles.SIGNINGOFFICIAL.getRoleName()));
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_errorResearcher() {
        User researcher = new User();
        researcher.setUserId(1);
        researcher.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(researcher);
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of(UserRoles.RESEARCHER.getRoleName()));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_errorAdmin() {
        User admin = new User();
        admin.setUserId(1);
        admin.setRoles(List.of(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(admin);
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of(UserRoles.ADMIN.getRoleName()));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_errorChair() {
        User chair = new User();
        chair.setUserId(1);
        chair.setRoles(List.of(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(chair);
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of(UserRoles.CHAIRPERSON.getRoleName()));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_errorMember() {
        User member = new User();
        member.setUserId(1);
        member.setRoles(List.of(new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName())));
        when(userService.findUserByEmail(any())).thenReturn(member);
        resource = new DataAccessRequestResource(
            dataAccessRequestService,
            userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of(UserRoles.MEMBER.getRoleName()));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UserNotFound() {
        when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.empty());
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UserMissingRole() {
        when(userService.findUserByEmail(any())).thenReturn(new User());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("SigningOfficial"));
        assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_UnsupportedRoleName() {
        when(userService.findUserByEmail(any())).thenReturn(new User());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("Member"));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void testDescribeManageDataAccessRequestsV2_InvalidRoleName() {
        when(userService.findUserByEmail(any())).thenReturn(new User());
        resource = new DataAccessRequestResource(
          dataAccessRequestService,
          userService);
        Response response = resource.describeManageDataAccessRequestsV2(authUser, Optional.of("BadRequest"));
        assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }


    private DataAccessRequest generateDataAccessRequest() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        dar.setDatasetIds(Arrays.asList(1, 2));
        dar.setData(data);
        dar.setUserId(1);
        return dar;
    }

}
