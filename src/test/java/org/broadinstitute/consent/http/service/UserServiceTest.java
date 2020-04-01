package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    @Mock
    private DACUserDAO userDAO;

    @Mock
    private UserRoleDAO roleDAO;

    private UserService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new UserService(userDAO, roleDAO);
    }

    @Test
    public void testFindUserByIdNoRoles() {
        DACUser u = createUser();
        when(userDAO.findDACUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        DACUser user = service.findUserById(u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertTrue(u.getRoles().isEmpty());
    }

    @Test
    public void testFindUserByIdWithRoles() {
        DACUser u = createUser();
        List<UserRole> roleList = Arrays.asList(
                createRole(UserRoles.RESEARCHER.getRoleId()),
                createRole(UserRoles.MEMBER.getRoleId())
        );
        when(userDAO.findDACUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(roleList);
        initService();

        DACUser user = service.findUserById(u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertFalse(u.getRoles().isEmpty());
        assertEquals(2, u.getRoles().size());
    }

    @Test
    public void testFindUserByIdNotFound() {
        DACUser u = createUser();
        when(userDAO.findDACUserById(any())).thenReturn(null);
        initService();

        DACUser user = service.findUserById(u.getDacUserId());
        assertNull(user);
    }

    @Test
    public void testFindUserByEmailNoRoles() {
        DACUser u = createUser();
        when(userDAO.findDACUserByEmail(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        DACUser user = service.findUserByEmail(u.getEmail());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertTrue(u.getRoles().isEmpty());
    }

    @Test
    public void testFindUserByEmailWithRoles() {
        DACUser u = createUser();
        List<UserRole> roleList = Arrays.asList(
                createRole(UserRoles.RESEARCHER.getRoleId()),
                createRole(UserRoles.MEMBER.getRoleId())
        );
        when(userDAO.findDACUserByEmail(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(roleList);
        initService();

        DACUser user = service.findUserByEmail(u.getEmail());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertFalse(u.getRoles().isEmpty());
        assertEquals(2, u.getRoles().size());
    }

    @Test
    public void testFindUserByEmailNotFound() {
        DACUser u = createUser();
        when(userDAO.findDACUserByEmail(any())).thenReturn(null);
        initService();

        DACUser user = service.findUserByEmail(u.getEmail());
        assertNull(user);
    }

    private DACUser createUser() {
        DACUser u = new DACUser();
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
