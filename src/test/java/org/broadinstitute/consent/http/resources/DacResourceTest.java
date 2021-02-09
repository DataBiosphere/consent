package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacBuilder;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DacResourceTest {

    @Mock
    private DacService dacService;

    @Mock
    private UserService userService;

    private DacResource dacResource;

    private final AuthUser authUser = new AuthUser("test@test.com");

    private final Gson gson = new Gson();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        dacResource = new DacResource(dacService, userService);
    }

    @Test
    public void testFindAll_success_1() {
        when(dacService.findDacsWithMembersOption(true)).thenReturn(Collections.emptyList());

        Response response = dacResource.findAll(authUser, Optional.of(true));
        assertEquals(200, response.getStatus());
        JsonArray dacs = getListFromEntityString(response.getEntity().toString());
        assertEquals(0, dacs.size());
    }

    @Test
    public void testFindAll_success_2() {
        Dac dac = new DacBuilder()
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.findDacsWithMembersOption(true)).thenReturn(Collections.singletonList(dac));

        Response response = dacResource.findAll(authUser, Optional.of(true));
        assertEquals(200, response.getStatus());
        JsonArray dacs = getListFromEntityString(response.getEntity().toString());
        assertEquals(1, dacs.size());
    }

    @Test
    public void testFindAllWithUsers() {
        when(dacService.findDacsWithMembersOption(false)).thenReturn(Collections.emptyList());

        Response response = dacResource.findAll(authUser, Optional.of(false));
        assertEquals(200, response.getStatus());
        JsonArray dacs = getListFromEntityString(response.getEntity().toString());
        assertEquals(0, dacs.size());
    }

    @Test
    public void testCreateDac_success() throws Exception {
        Dac dac = new DacBuilder()
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.createDac(any(), any())).thenReturn(1);
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.createDac(authUser, gson.toJson(dac));
        assertEquals(200, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDac_badRequest_1() throws Exception {
        dacResource.createDac(authUser, null);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDac_badRequest_2() throws Exception {
        Dac dac = new DacBuilder()
                .setName(null)
                .setDescription("description")
                .build();
        when(dacService.createDac(any(), any())).thenReturn(1);
        when(dacService.findById(1)).thenReturn(dac);

        dacResource.createDac(authUser, gson.toJson(dac));
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDac_badRequest_3() throws Exception {
        Dac dac = new DacBuilder()
                .setName("name")
                .setDescription(null)
                .build();
        when(dacService.createDac(any(), any())).thenReturn(1);
        when(dacService.findById(1)).thenReturn(dac);

        dacResource.createDac(authUser, gson.toJson(dac));
    }


    @Test
    public void testUpdateDac_success() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription("description")
                .build();
        doNothing().when(dacService).updateDac(isA(String.class), isA(String.class), isA(Integer.class));
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.updateDac(authUser, gson.toJson(dac));
        assertEquals(200, response.getStatus());
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_1() {
        dacResource.updateDac(authUser, null);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_2() {
        Dac dac = new DacBuilder()
                .setDacId(null)
                .setName("name")
                .setDescription("description")
                .build();
        dacResource.updateDac(authUser, gson.toJson(dac));
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_3() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName(null)
                .setDescription("description")
                .build();
        dacResource.updateDac(authUser, gson.toJson(dac));
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateDac_badRequest_4() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription(null)
                .build();
        dacResource.updateDac(authUser, gson.toJson(dac));
    }

    @Test
    public void testFindById_success() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.findById(dac.getDacId());
        assertEquals(200, response.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testFindById_failure() {
        when(dacService.findById(1)).thenReturn(null);

        dacResource.findById(1);
    }

    @Test
    public void testDeleteDac_success() {
        Dac dac = new DacBuilder()
                .setDacId(1)
                .setName("name")
                .setDescription("description")
                .build();
        when(dacService.findById(1)).thenReturn(dac);

        Response response = dacResource.deleteDac(dac.getDacId());
        assertEquals(200, response.getStatus());

    }

    @Test(expected = NotFoundException.class)
    public void testDeleteDac_failure() {
        when(dacService.findById(1)).thenReturn(null);

        dacResource.deleteDac(1);
    }

    @Test
    public void testAddDacMemberAsAdmin() {
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User admin = buildAdmin(authUser);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(admin);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.addDacMember(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testAddDacMemberAsChairSuccess() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(chair);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.addDacMember(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAddDacMemberAsChairFailure() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        dacResource.addDacMember(authUser, dac.getDacId(), member.getDacUserId());
    }

    @Test
    public void testRemoveDacMemberAsAdmin() {
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User admin = buildAdmin(authUser);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(admin);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.removeDacMember(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testRemoveDacMemberAsChairSuccess() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(chair);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.removeDacMember(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test(expected = NotAuthorizedException.class)
    public void testRemoveDacMemberAsChairFailure() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        dacResource.removeDacMember(authUser, dac.getDacId(), member.getDacUserId());
    }

    @Test
    public void testAddDacChairAsAdmin() {
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User admin = buildAdmin(authUser);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(admin);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.addDacChair(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testAddDacChairAsChairSuccess() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(chair);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.addDacChair(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAddDacChairAsChairFailure() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        dacResource.addDacChair(authUser, dac.getDacId(), member.getDacUserId());
    }

    @Test
    public void testRemoveDacChairAsAdmin() {
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User admin = buildAdmin(authUser);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(admin);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.removeDacChair(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test
    public void testRemoveDacChairAsChairSuccess() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(chair);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        Response response = dacResource.removeDacChair(authUser, dac.getDacId(), member.getDacUserId());
        assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    @Test(expected = NotAuthorizedException.class)
    public void testRemoveDacChairAsChairFailure() {
        User chair = buildChair(authUser);
        Dac dac = buildDac(null);
        when(dacService.findById(any())).thenReturn(dac);
        User member = buildUser();
        when(userService.findUserByEmail(authUser.getName())).thenReturn(chair);
        when(dacService.findUserById(member.getDacUserId())).thenReturn(member);

        dacResource.removeDacChair(authUser, dac.getDacId(), member.getDacUserId());
    }

    private JsonArray getListFromEntityString(String str) {
        return new Gson().fromJson(str, JsonArray.class);
    }

    private Dac buildDac(User chair) {
        Dac dac = new DacBuilder()
            .setDacId(RandomUtils.nextInt())
            .setName("name")
            .setDescription("description")
            .build();
        if (Objects.nonNull(chair)) {
            dac.setChairpersons(Collections.singletonList(chair));
        }
        return dac;
    }

    private User buildAdmin(AuthUser authUser) {
        User user = buildUser();
        user.setDacUserId(RandomUtils.nextInt());
        user.setEmail(authUser.getName());
        UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        user.setRoles(Collections.singletonList(admin));
        return user;
    }

    private User buildChair(AuthUser authUser) {
        User user = buildUser();
        user.setDacUserId(RandomUtils.nextInt());
        user.setEmail(authUser.getName());
        UserRole chair = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        user.setRoles(Collections.singletonList(chair));
        return user;
    }

    private User buildUser() {
        User user = new User();
        user.setDacUserId(RandomUtils.nextInt());
        return user;
    }
}
