package org.broadinstitute.consent.http.service;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private UserPropertyDAO userPropertyDAO;

    @Mock
    private UserRoleDAO userRoleDAO;

    @Mock
    private VoteDAO voteDAO;

    @Mock
    private InstitutionDAO institutionDAO;

    @Mock
    private LibraryCardDAO libraryCardDAO;

    private UserService service;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initService() {
        service = new UserService(userDAO, userPropertyDAO, userRoleDAO, voteDAO, institutionDAO, libraryCardDAO);
    }

    @Test
    public void testUpdateUserFieldsById() {
        UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        UserRole chair = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());

        User user = new User();
        user.setUserId(1);

        // Note that we're starting out with 1 modifiable role (Admin) and 1 that is not (Chairperson)
        // and one role that should never be removed, but can be added (Researcher)
        // When we update this user, we'll ensure that the new roles are added, old roles are deleted,
        // and the researcher & chairperson roles remain.
        when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(List.of(admin, researcher, chair));
        when(userDAO.findUserById(any())).thenReturn(user);
        spy(userDAO);
        spy(userPropertyDAO);
        spy(userRoleDAO);
        initService();
        try {
            UserUpdateFields fields = new UserUpdateFields();
            // We're modifying this user to have an SO role. This should leave in place
            // both the Researcher and Chairperson roles, but remove the Admin role.
            fields.setUserRoleIds(List.of(so.getRoleId()));
            fields.setDisplayName(RandomStringUtils.random(10, true, false));
            fields.setInstitutionId(1);
            fields.setEmailPreference(true);
            fields.setEraCommonsId(RandomStringUtils.random(10, true, false));
            fields.setSelectedSigningOfficialId(1);
            fields.setSuggestedSigningOfficial(RandomStringUtils.random(10, true, false));
            fields.setSuggestedInstitution(RandomStringUtils.random(10, true, false));
            assertEquals(3, fields.buildUserProperties(user.getUserId()).size());
            service.updateUserFieldsById(fields, user.getUserId());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // We added 3 user property values, we should have props for them:
        verify(userDAO, times(1)).updateDisplayName(any(), any());
        verify(userDAO, times(1)).updateInstitutionId(any(), any());
        verify(userDAO, times(1)).updateEmailPreference(any(), any());
        verify(userDAO, times(1)).updateEraCommonsId(any(), any());
        verify(userPropertyDAO, times(1)).insertAll(any());
        // Verify role additions/deletions.
        verify(userRoleDAO, times(1)).insertUserRoles(List.of(so), 1);
        verify(userRoleDAO, times(1)).removeUserRoles( 1, List.of(admin.getRoleId()));
    }

    @Test
    public void createUserTest() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserById(any())).thenReturn(u);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(roles);
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
        List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserById(any())).thenReturn(u);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(roles);
        when(libraryCardDAO.findAllLibraryCardsByUserEmail(u.getEmail())).thenReturn(List.of(libraryCard));
        initService();

        try {
            service.createUser(u);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(institutionId, u.getInstitutionId());
        assertEquals(u.getUserId(), libraryCard.getUserId());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserDuplicateEmail() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserByEmail(any())).thenReturn(u);
        initService();
        service.createUser(u);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserNoDisplayName() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        u.setDisplayName(null);
        initService();
        service.createUser(u);
    }

    @Test
    public void testCreateUserNoRoles() {
        User u = generateUser();
        when(userDAO.findUserById(any())).thenReturn(u);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(List.of(generateRole(UserRoles.RESEARCHER.getRoleId())));
        initService();
        User user = service.createUser(u);
        assertFalse(user.getRoles().isEmpty());
        assertEquals(UserRoles.RESEARCHER.getRoleId(), user.getRoles().get(0).getRoleId());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserInvalidRoleCase1() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.CHAIRPERSON.getRoleId()));
        u.setRoles(roles);
        initService();
        service.createUser(u);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateUserInvalidRoleCase2() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.MEMBER.getRoleId()));
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
    public void testFindUserById_HasLibraryCards() {
        User u = generateUser();
        LibraryCard one = generateLibraryCard(u);
        LibraryCard two = generateLibraryCard(u);
        List<LibraryCard> cards = List.of(one, two);
        when(userDAO.findUserById(any())).thenReturn(u);
        when(libraryCardDAO.findLibraryCardsByUserId(any())).thenReturn(cards);
        initService();

        User user = service.findUserById(u.getUserId());
        assertNotNull(user);
        assertNotNull(user.getLibraryCards());
        assertEquals(user.getLibraryCards().size(), 2);
        assertEquals(user.getLibraryCards().get(0).getId(), one.getId());
        assertEquals(user.getLibraryCards().get(1).getId(), two.getId());
    }

    @Test
    public void testFindUserByIdNoRoles() {
        User u = generateUser();
        when(userDAO.findUserById(any())).thenReturn(u);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        User user = service.findUserById(u.getUserId());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertNull(u.getRoles());
    }

    @Test
    public void testFindUserByIdWithRoles() {
        User u = generateUser();
        List<UserRole> roleList = List.of(
                generateRole(UserRoles.RESEARCHER.getRoleId()),
                generateRole(UserRoles.MEMBER.getRoleId())
        );
        u.setRoles(roleList);
        when(userDAO.findUserById(any())).thenReturn(u);
        initService();

        User user = service.findUserById(u.getUserId());
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

        service.findUserById(u.getUserId());
    }

    @Test
    public void testFindUserByEmailNoRoles() {
        User u = generateUser();
        when(userDAO.findUserByEmail(any())).thenReturn(u);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.emptyList());
        initService();

        User user = service.findUserByEmail(u.getEmail());
        assertNotNull(user);
        assertEquals(u.getEmail(), user.getEmail());
        assertNull(u.getRoles());
    }

    @Test
    public void testFindUserByEmailWithRoles() {
        User u = generateUser();
        List<UserRole> roleList = List.of(
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
    public void testUpdateDACUserById() {
        User u = generateUser();
        when(userDAO.findUserById(u.getUserId()))
                .thenReturn(u);
        when(institutionDAO.checkForExistingInstitution(any()))
                .thenReturn(u.getInstitutionId());
        doNothing().when(userDAO).updateUser(any(), any(), any());
        initService();
        Map<String, User> dacUsers = Map.of(UserRolesHandler.UPDATED_USER_KEY, u);
        User user = service.updateDACUserById(dacUsers, u.getUserId());
        assertNotNull(user);
        assertEquals(u.getUserId(), user.getUserId());
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateDACUserById_NonExisting() {
        User u = generateUser();
        when(userDAO.findUserById(u.getUserId()))
                .thenReturn(null);
        doNothing().when(userDAO).updateUser(any(), any(), any());
        initService();
        Map<String, User> dacUsers = Map.of(UserRolesHandler.UPDATED_USER_KEY, u);
        service.updateDACUserById(dacUsers, u.getUserId());
    }

    @Test
    public void testFindSOsByInstitutionId() {
        User u = generateUser();
        Integer institutionId = u.getInstitutionId();
        when(userDAO.getSOsByInstitution(any())).thenReturn(List.of(u, u, u));
        initService();
        List<SimplifiedUser> users = service.findSOsByInstitutionId(institutionId);
        assertEquals(3, users.size());
        assertEquals(u.getDisplayName(), users.get(0).displayName);
        assertEquals(u.getEmail(), users.get(0).email);
    }

    @Test
    public void testFindSOsByInstitutionId_NullId() {
        initService();
        List<SimplifiedUser> users = service.findSOsByInstitutionId(null);
        assertEquals(0, users.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindUsersByInstitutionIdNullId() {
        initService();
        service.findUsersByInstitutionId(null);
    }

    @Test(expected = NotFoundException.class)
    public void testFindUsersByInstitutionIdNullInstitution() {
        doThrow(new NotFoundException()).when(institutionDAO).findInstitutionById(anyInt());
        initService();
        service.findUsersByInstitutionId(1);
    }

    @Test
    public void testFindUsersByInstitutionIdSuccess() {
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(new Institution());
        initService();
        List<User> users = service.findUsersByInstitutionId(1);
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void testFindUsersByInstitutionIdSuccessWithUsers() {
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(new Institution());
        when(userDAO.findUsersByInstitution(anyInt())).thenReturn(List.of(new User()));
        initService();
        List<User> users = service.findUsersByInstitutionId(1);
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    public void testGetUsersByUserRole_SO() {
        User u = generateUser();
        u.setInstitutionId(1);
        when(userDAO.getUsersFromInstitutionWithCards(anyInt())).thenReturn(List.of(new User()));
        when(userDAO.getCardsForUnregisteredUsers(anyInt())).thenReturn(List.of(new User()));
        initService();

        List<User> users = service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName());
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test(expected = NotFoundException.class)
    public void testGetUsersAsRoleSO_NoInstitution() {
        User u = generateUser();
        u.setInstitutionId(null);
        initService();
    service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName());
    }

    @Test
    public void testGetUsersAsRoleAdmin() {
        User u1 = generateUser();
        User u2 = generateUser();
        User u3 = generateUser();
        List<User> returnedUsers = new ArrayList<>();
        returnedUsers.add(u1);
        if (!returnedUsers.contains(u2)) {
            returnedUsers.add(u2);
        }
        if (!returnedUsers.contains(u3)) {
            returnedUsers.add(u3);
        }
        when(userDAO.findUsers()).thenReturn(new HashSet<>(returnedUsers));
        initService();
        List<User> users = service.getUsersAsRole(u1, UserRoles.ADMIN.getRoleName());
        assertNotNull(users);
        assertEquals(returnedUsers.size(), users.size());
    }

    @Test
    public void testFindUsersWithNoInstitution() {
        User user = generateUser();
        when(userDAO.getUsersWithNoInstitution()).thenReturn(List.of(user));
        initService();
        List<User> users = service.findUsersWithNoInstitution();
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(user.getUserId(), users.get(0).getUserId());
    }

    @Test
    public void testFindUserWithPropertiesAsJsonObjectById() {
        User user = generateUser();
        UserStatusInfo info = new UserStatusInfo()
            .setUserEmail(user.getEmail())
            .setEnabled(true)
            .setUserSubjectId("subjectId");
        AuthUser authUser = new AuthUser()
            .setEmail(user.getEmail())
            .setAuthToken(RandomStringUtils.random(30, true, false))
            .setUserStatusInfo(info);
        when(userDAO.findUserById(anyInt())).thenReturn(user);
        when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(List.of(new LibraryCard()));
        when(userPropertyDAO.findResearcherPropertiesByUser(anyInt())).thenReturn(List.of(new UserProperty()));

        initService();
        JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
        assertNotNull(userJson);
        assertTrue(userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
        assertTrue(userJson.get(UserService.RESEARCHER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
        assertTrue(userJson.get(UserService.USER_STATUS_INFO_FIELD).getAsJsonObject().isJsonObject());
    }

    @Test
    public void testFindUserWithPropertiesAsJsonObjectByIdNonAuthUser() {
        User user = generateUser();
        UserStatusInfo info = new UserStatusInfo()
            .setUserEmail(user.getEmail())
            .setEnabled(true)
            .setUserSubjectId("subjectId");
        AuthUser authUser = new AuthUser()
            .setEmail("not the user's email address")
            .setAuthToken(RandomStringUtils.random(30, true, false))
            .setUserStatusInfo(info);
        when(userDAO.findUserById(anyInt())).thenReturn(user);
        when(libraryCardDAO.findLibraryCardsByUserId(anyInt())).thenReturn(List.of(new LibraryCard()));
        when(userPropertyDAO.findResearcherPropertiesByUser(anyInt())).thenReturn(List.of(new UserProperty()));

        initService();
        JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
        assertNotNull(userJson);
        assertTrue(userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
        assertTrue(userJson.get(UserService.RESEARCHER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
        assertNull(userJson.get(UserService.USER_STATUS_INFO_FIELD));
    }

    private User generateUser() {
        User u = new User();
        int i1 = RandomUtils.nextInt(10, 50);
        int i2 = RandomUtils.nextInt(10, 50);
        int i3 = RandomUtils.nextInt(5, 25);
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
        u.setUserId(RandomUtils.nextInt(1, 100));
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

    private LibraryCard generateLibraryCard(User user) {
        LibraryCard libraryCard = new LibraryCard();
        libraryCard.setId(RandomUtils.nextInt(1, 10));
        libraryCard.setUserId(user.getUserId());
        libraryCard.setInstitutionId(RandomUtils.nextInt(1,10));
        return libraryCard;
    }

    private UserRole generateRole(int roleId) {
        UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
        assert rolesEnum != null;
        return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
    }

}
