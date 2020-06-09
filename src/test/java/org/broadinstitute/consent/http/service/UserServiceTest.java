package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private ResearcherPropertyDAO researcherPropertyDAO;

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
        service = new UserService(userDAO, researcherPropertyDAO, roleDAO, voteDAO);
    }

    @Test
    public void testFindUserByIdNoRoles() {
        User u = createUser();
        when(userDAO.findDACUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        User user = service.findUserById(u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertTrue(u.getRoles().isEmpty());
    }

    @Test
    public void testFindUserByIdWithRoles() {
        User u = createUser();
        List<UserRole> roleList = Arrays.asList(
                createRole(UserRoles.RESEARCHER.getRoleId()),
                createRole(UserRoles.MEMBER.getRoleId())
        );
        when(userDAO.findDACUserById(any())).thenReturn(u);
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
        User u = createUser();
        when(userDAO.findDACUserById(any())).thenReturn(null);
        initService();

        service.findUserById(u.getDacUserId());
    }

    @Test
    public void testFindUserByEmailNoRoles() {
        User u = createUser();
        when(userDAO.findDACUserByEmail(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        User user = service.findUserByEmail(u.getEmail());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertTrue(u.getRoles().isEmpty());
    }

    @Test
    public void testFindUserByEmailWithRoles() {
        User u = createUser();
        List<UserRole> roleList = Arrays.asList(
                createRole(UserRoles.RESEARCHER.getRoleId()),
                createRole(UserRoles.MEMBER.getRoleId())
        );
        when(userDAO.findDACUserByEmail(any())).thenReturn(u);
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
        User u = createUser();
        when(userDAO.findDACUserByEmail(any())).thenReturn(null);
        initService();

        service.findUserByEmail(u.getEmail());
    }

    @Test
    public void testDeleteUser() {
        User u = createUser();
        doNothing().when(researcherPropertyDAO).deleteAllPropertiesByUser(any());
        when(userDAO.findDACUserByEmail(any())).thenReturn(u);
        initService();

        try {
            service.deleteUserByEmail(RandomStringUtils.random(10, true, false));
        } catch (Exception e) {
            fail("Should not fail: " + e.getMessage());
        }
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteUserFailure() {
        when(userDAO.findDACUserByEmail(any())).thenThrow(new NotFoundException());
        initService();
        service.deleteUserByEmail(RandomStringUtils.random(10, true, false));
    }

    private User createUser() {
        User u = new User();
        int i1 = RandomUtils.nextInt(5, 10);
        int i2 = RandomUtils.nextInt(5, 10);
        int i3 = RandomUtils.nextInt(3, 5);
        String email = RandomStringUtils.randomAlphabetic(i1) +
                "@" +
                RandomStringUtils.randomAlphabetic(i2) +
                "." +
                RandomStringUtils.randomAlphabetic(i3);
        u.setEmail(email);
        u.setDacUserId(RandomUtils.nextInt(1, 100));
        return u;
    }

    private UserRole createRole(int roleId) {
        UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
        assert rolesEnum != null;
        return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
    }

}
