package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
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

    @Mock
    private InstitutionDAO institutionDAO;

    @Mock
    private LibraryCardDAO libraryCardDAO;

    private UserService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new UserService(userDAO, userPropertyDAO, roleDAO, voteDAO, institutionDAO, libraryCardDAO);
    }

    @Test
    public void createUserTest() {
        User u = generateUser();
        List<UserRole> roles = Collections.singletonList(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(roles);
        when(libraryCardDAO.findAllLibraryCardsByUserEmail(any())).thenReturn(Collections.emptyList());
        initService();
        try {
            service.createUser(u);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createUserWithLibraryCardTest() {
        User u = generateUser();
        LibraryCard libraryCard = generateLibraryCard(u.getEmail());
        Integer institutionId = libraryCard.getInstitutionId();
        List<UserRole> roles = Collections.singletonList(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserById(any())).thenReturn(u);
        when(roleDAO.findRolesByUserId(any())).thenReturn(roles);
        when(libraryCardDAO.findAllLibraryCardsByUserEmail(u.getEmail())).thenReturn(Arrays.asList(libraryCard));
        initService();

        try {
            service.createUser(u);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(institutionId, u.getInstitutionId());
        assertEquals(u.getDacUserId(), libraryCard.getUserId());
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
        assertNull(u.getRoles());
    }

    @Test
    public void testFindUserByIdWithRoles() {
        User u = generateUser();
        List<UserRole> roleList = Arrays.asList(
                generateRole(UserRoles.RESEARCHER.getRoleId()),
                generateRole(UserRoles.MEMBER.getRoleId())
        );
        u.setRoles(roleList);
        when(userDAO.findUserById(any())).thenReturn(u);
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
        assertNull(u.getRoles());
    }

    @Test
    public void testFindUserByEmailWithRoles() {
        User u = generateUser();
        List<UserRole> roleList = Arrays.asList(
                generateRole(UserRoles.RESEARCHER.getRoleId()),
                generateRole(UserRoles.MEMBER.getRoleId())
        );
        u.setRoles(roleList);
        when(userDAO.findUserByEmail(any())).thenReturn(u);
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

    @Test
    public void testUpdateUserStatus() {
        User u = generateUser();
        when(userDAO.findUserById(u.getDacUserId()))
                .thenReturn(u);
        doNothing().when(userDAO).updateUserStatus(any(), any());
        initService();
        User user = service.updateUserStatus(RoleStatus.APPROVED.toString(), u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getDacUserId(), user.getDacUserId());
    }

    @Test
    public void testUpdateUserRationale() {
        User u = generateUser();
        when(userDAO.findUserById(u.getDacUserId()))
                .thenReturn(u);
        doNothing().when(userDAO).updateUserRationale(any(), any());
        initService();
        User user = service.updateUserRationale("test", u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getDacUserId(), user.getDacUserId());
    }

    @Test
    public void testUpdateDACUserById() {
        User u = generateUser();
        when(userDAO.findUserById(u.getDacUserId()))
                .thenReturn(u);
        when(institutionDAO.checkForExistingInstitution(any()))
                .thenReturn(u.getInstitutionId());
        doNothing().when(userDAO).updateUser(any(), any(), any(), any());
        initService();
        Map<String, User> dacUsers = Map.of(UserRolesHandler.UPDATED_USER_KEY, u);
        User user = service.updateDACUserById(dacUsers, u.getDacUserId());
        assertNotNull(user);
        assertEquals(u.getDacUserId(), user.getDacUserId());
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateDACUserById_NonExisting() {
        User u = generateUser();
        when(userDAO.findUserById(u.getDacUserId()))
                .thenReturn(null);
        doNothing().when(userDAO).updateUser(any(), any(), any(), any());
        initService();
        Map<String, User> dacUsers = Map.of(UserRolesHandler.UPDATED_USER_KEY, u);
        User user = service.updateDACUserById(dacUsers, u.getDacUserId());
    }

    @Test
    public void testFindSOsByInstitutionId() {
        User u = generateUser();
        Integer institutionId = u.getInstitutionId();
        when(userDAO.getSOsByInstitution(any())).thenReturn(Arrays.asList(u, u, u));
        initService();
        List<SimplifiedUser> users = service.findSOsByInstitutionId(institutionId);
        assertEquals(3, users.size());
        assertEquals(u.getDisplayName(), users.get(0).displayName);
    }

    @Test
    public void testFindSOsByInstitutionId_NullId() {
        initService();
        List<SimplifiedUser> users = service.findSOsByInstitutionId(null);
        assertEquals(0, users.size());
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
        u.setInstitutionId(RandomUtils.nextInt(1, 100));
        return u;
    }

    private LibraryCard generateLibraryCard(String email) {
        LibraryCard libraryCard = new LibraryCard();
        libraryCard.setId(RandomUtils.nextInt(1, 10));
        libraryCard.setInstitutionId(RandomUtils.nextInt(1, 10));
        libraryCard.setUserEmail(email);
        libraryCard.setUserName(RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(1, 10)));
        return libraryCard;
    }

    private UserRole generateRole(int roleId) {
        UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
        assert rolesEnum != null;
        return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
    }

}
