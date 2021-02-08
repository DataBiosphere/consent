package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private UserPropertyDAO userPropertyDAO;

    @Mock
    private UserRoleDAO roleDAO;

    @Mock
    private VoteDAO voteDAO;

    private UserService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new UserService(userDAO, userPropertyDAO, roleDAO, voteDAO);
    }

    @Test
    public void createUserTest() {
        User u = generateUser();
        List<UserRole> roles = Collections.singletonList(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(roles);
        initService();
        try {
            service.createUser(u);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserDuplicateEmail() {
        User u = generateUser();
        List<UserRole> roles = Collections.singletonList(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserByEmail(any())).thenReturn(u);
        initService();
        service.createUser(u);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserNoDisplayName() {
        User u = generateUser();
        List<UserRole> roles = Collections.singletonList(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        u.setDisplayName(null);
        initService();
        service.createUser(u);
    }

    @Test
    public void testCreateUserNoRoles() {
        User u = generateUser();
        when(userDAO.findUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(Collections.singletonList(generateRole(UserRoles.RESEARCHER.getRoleId())));
        initService();
        User user = service.createUser(u);
        assertFalse(user.getRoles().isEmpty());
        assertEquals(UserRoles.RESEARCHER.getRoleId(), user.getRoles().get(0).getRoleId());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserInvalidRoleCase1() {
        User u = generateUser();
        List<UserRole> roles = Collections.singletonList(generateRole(UserRoles.CHAIRPERSON.getRoleId()));
        u.setRoles(roles);
        initService();
        service.createUser(u);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserInvalidRoleCase2() {
        User u = generateUser();
        List<UserRole> roles = Collections.singletonList(generateRole(UserRoles.MEMBER.getRoleId()));
        u.setRoles(roles);
        initService();
        service.createUser(u);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserNoEmail() {
        User u = generateUser();
        u.setEmail(null);
        initService();
        service.createUser(u);
    }

    @Test
    public void testFindUserByIdNoRoles() {
        User u = generateUser();
        when(userDAO.findUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        User user = service.findUserById(u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertTrue(u.getRoles().isEmpty());
    }

    @Test
    public void testFindUserByIdWithRoles() {
        User u = generateUser();
        List<UserRole> roleList = Arrays.asList(
                generateRole(UserRoles.RESEARCHER.getRoleId()),
                generateRole(UserRoles.MEMBER.getRoleId())
        );
        when(userDAO.findUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(roleList);
        initService();

        User user = service.findUserById(u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertFalse(u.getRoles().isEmpty());
        assertEquals(2, u.getRoles().size());
    }

    @Test(expected = NotFoundException.class)
    public void testFindUserByIdNotFound() {
        User u = generateUser();
        when(userDAO.findUserById(any())).thenReturn(null);
        initService();

        service.findUserById(u.getDacUserId());
    }

    @Test
    public void testFindUserByEmailNoRoles() {
        User u = generateUser();
        when(userDAO.findUserByEmail(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        User user = service.findUserByEmail(u.getEmail());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertTrue(u.getRoles().isEmpty());
    }

    @Test
    public void testFindUserByEmailWithRoles() {
        User u = generateUser();
        List<UserRole> roleList = Arrays.asList(
                generateRole(UserRoles.RESEARCHER.getRoleId()),
                generateRole(UserRoles.MEMBER.getRoleId())
        );
        when(userDAO.findUserByEmail(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(roleList);
        initService();

        User user = service.findUserByEmail(u.getEmail());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertFalse(u.getRoles().isEmpty());
        assertEquals(2, u.getRoles().size());
    }

    @Test(expected = NotFoundException.class)
    public void testFindUserByEmailNotFound() {
        User u = generateUser();
        when(userDAO.findUserByEmail(any())).thenReturn(null);
        initService();

        service.findUserByEmail(u.getEmail());
    }

    @Test
    public void testDeleteUser() {
        User u = generateUser();
        doNothing().when(userPropertyDAO).deleteAllPropertiesByUser(any());
        when(userDAO.findUserByEmail(any())).thenReturn(u);
        initService();

        try {
            service.deleteUserByEmail(RandomStringUtils.random(10, true, false));
        } catch (Exception e) {
            fail("Should not fail: " + e.getMessage());
        }
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteUserFailure() {
        when(userDAO.findUserByEmail(any())).thenThrow(new NotFoundException());
        initService();
        service.deleteUserByEmail(RandomStringUtils.random(10, true, false));
    }

    private User generateUser() {
        User u = new User();
        int i1 = RandomUtils.nextInt(5, 10);
        int i2 = RandomUtils.nextInt(5, 10);
        int i3 = RandomUtils.nextInt(3, 5);
        String email = RandomStringUtils.randomAlphabetic(i1) +
                "@" +
                RandomStringUtils.randomAlphabetic(i2) +
                "." +
                RandomStringUtils.randomAlphabetic(i3);
        String displayName = RandomStringUtils.randomAlphabetic(i1) +
                " " +
                RandomStringUtils.randomAlphabetic(i2);
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setDacUserId(RandomUtils.nextInt(1, 100));
        return u;
    }

    private UserRole generateRole(int roleId) {
        UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
        assert rolesEnum != null;
        return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
    }

}
