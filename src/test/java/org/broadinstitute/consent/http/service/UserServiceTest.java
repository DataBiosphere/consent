package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.SamDAO;
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
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

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
    private DatasetAssociationDAO datasetAssociationDAO;

    @Mock
    private InstitutionDAO institutionDAO;

    @Mock
    private LibraryCardDAO libraryCardDAO;

    @Mock
    private AcknowledgementDAO acknowledgementDAO;

    @Mock
    private FileStorageObjectDAO fileStorageObjectDAO;

    @Mock
    private SamDAO samDAO;

    @Mock
    private UserServiceDAO userServiceDAO;

    @Mock
    private EmailService emailService;


    private UserService service;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    private void initService() {
        service = new UserService(userDAO, userPropertyDAO, userRoleDAO, voteDAO, datasetAssociationDAO, institutionDAO, libraryCardDAO,
                acknowledgementDAO, fileStorageObjectDAO, samDAO, userServiceDAO, emailService);
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
        UserProperty prop = new UserProperty();
        prop.setPropertyValue("1");
        when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any())).thenReturn(List.of(prop));
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
            fields.setDaaAcceptance(true);
            assertEquals(4, fields.buildUserProperties(user.getUserId()).size());
            service.updateUserFieldsById(fields, user.getUserId());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
        // We added 3 user property values, we should have props for them:
        verify(userDAO, times(1)).updateDisplayName(any(), any());
        verify(userDAO, times(1)).updateInstitutionId(any(), any());
        verify(userDAO, times(1)).updateEmailPreference(any(), any());
        verify(userDAO, times(1)).updateEraCommonsId(any(), any());
        verify(userPropertyDAO, times(1)).insertAll(any());
        // Verify role additions/deletions.
        verify(userRoleDAO, times(1)).insertUserRoles(List.of(so), 1);
        verify(userRoleDAO, times(1)).removeUserRoles(1, List.of(admin.getRoleId()));
    }

    @Test
    public void testUpdateUserFieldsById_SendsEmailWhenSOInitalized() throws Exception {
        UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        UserRole chair = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());

        User user = new User();
        user.setUserId(1);

        when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(List.of(admin, researcher, chair));
        when(userDAO.findUserById(any())).thenReturn(user);
        UserProperty prop = new UserProperty();
        prop.setPropertyValue("1");
        when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any()))
                .thenReturn(List.of()) // first time, no SO id
                .thenReturn(List.of(prop)); // second time, has SO id
        spy(userDAO);
        spy(userPropertyDAO);
        spy(userRoleDAO);
        initService();
        try {
            UserUpdateFields fields = new UserUpdateFields();
            fields.setSelectedSigningOfficialId(1);

            assertEquals(1, fields.buildUserProperties(user.getUserId()).size());
            service.updateUserFieldsById(fields, user.getUserId());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
        // We added 3 user property values, we should have props for them:
        verify(userDAO, never()).updateDisplayName(any(), any());
        verify(userDAO, never()).updateInstitutionId(any(), any());
        verify(userDAO, never()).updateEmailPreference(any(), any());
        verify(userDAO, never()).updateEraCommonsId(any(), any());
        verify(userPropertyDAO, times(1)).insertAll(any());

        verify(emailService, times(1)).sendNewResearcherMessage(any(), any());
    }

    @Test
    public void testUpdateUserFieldsById_NoEmailOnSOChange() throws Exception {
        UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        UserRole chair = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());

        User user = new User();
        user.setUserId(1);

        when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(List.of(admin, researcher, chair));
        when(userDAO.findUserById(any())).thenReturn(user);
        UserProperty prop1 = new UserProperty();
        prop1.setPropertyValue("1");
        UserProperty prop2 = new UserProperty();
        prop2.setPropertyValue("2");
        when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any()))
                .thenReturn(List.of(prop1)) // first SO id
                .thenReturn(List.of(prop2)); // second SO id
        spy(userDAO);
        spy(userPropertyDAO);
        spy(userRoleDAO);
        initService();
        try {
            UserUpdateFields fields = new UserUpdateFields();
            fields.setSelectedSigningOfficialId(2);

            assertEquals(1, fields.buildUserProperties(user.getUserId()).size());
            service.updateUserFieldsById(fields, user.getUserId());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
        // We added 3 user property values, we should have props for them:
        verify(userDAO, never()).updateDisplayName(any(), any());
        verify(userDAO, never()).updateInstitutionId(any(), any());
        verify(userDAO, never()).updateEmailPreference(any(), any());
        verify(userDAO, never()).updateEraCommonsId(any(), any());
        verify(userPropertyDAO, times(1)).insertAll(any());

        verify(emailService, never()).sendNewResearcherMessage(any(), any());
    }

    @Test
    public void testUpdateUserFieldsById_NoEmailOnNoChange() throws Exception {
        UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        UserRole chair = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());

        User user = new User();
        user.setUserId(1);

        when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(List.of(admin, researcher, chair));
        when(userDAO.findUserById(any())).thenReturn(user);
        UserProperty prop = new UserProperty();
        prop.setPropertyValue("1");
        when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any()))
                .thenReturn(List.of(prop)) // first SO id
                .thenReturn(List.of(prop)); // second SO id
        spy(userDAO);
        spy(userPropertyDAO);
        spy(userRoleDAO);
        initService();
        try {
            UserUpdateFields fields = new UserUpdateFields();
            fields.setSelectedSigningOfficialId(1);

            assertEquals(1, fields.buildUserProperties(user.getUserId()).size());
            service.updateUserFieldsById(fields, user.getUserId());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
        // We added 3 user property values, we should have props for them:
        verify(userDAO, never()).updateDisplayName(any(), any());
        verify(userDAO, never()).updateInstitutionId(any(), any());
        verify(userDAO, never()).updateEmailPreference(any(), any());
        verify(userDAO, never()).updateEraCommonsId(any(), any());
        verify(userPropertyDAO, times(1)).insertAll(any());

        verify(emailService, never()).sendNewResearcherMessage(any(), any());
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
            Assertions.fail(e.getMessage());
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
            Assertions.fail(e.getMessage());
        }

        assertEquals(institutionId, u.getInstitutionId());
        assertEquals(u.getUserId(), libraryCard.getUserId());
    }

    @Test
    public void testCreateUserDuplicateEmail() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        when(userDAO.findUserByEmail(any())).thenReturn(u);
        initService();
        try {
            service.createUser(u);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void testCreateUserNoDisplayName() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
        u.setRoles(roles);
        u.setDisplayName(null);
        initService();
        try {
            service.createUser(u);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void testCreateUserNoRoles() {
        User u = generateUser();
        when(userDAO.findUserById(any())).thenReturn(u);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(List.of(generateRole(UserRoles.RESEARCHER.getRoleId())));
        initService();
        User user = service.createUser(u);
        assertFalse(user.getRoles().isEmpty());
        assertEquals(UserRoles.RESEARCHER.getRoleId(),
            user.getRoles().get(0).getRoleId());
    }

    @Test
    public void testCreateUserInvalidRoleCase1() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.CHAIRPERSON.getRoleId()));
        u.setRoles(roles);
        initService();
        try {
            service.createUser(u);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void testCreateUserInvalidRoleCase2() {
        User u = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.MEMBER.getRoleId()));
        u.setRoles(roles);
        initService();
        try {
            service.createUser(u);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void testCreateUserNoEmail() {
        User u = generateUser();
        u.setEmail(null);
        initService();
        try {
            service.createUser(u);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }
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
        Assertions.assertNull(u.getRoles());
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

    @Test
    public void testFindUserByIdNotFound() {
        User u = generateUser();
        when(userDAO.findUserById(any())).thenReturn(null);
        initService();

        try {
            service.findUserById(u.getUserId());
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
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
        Assertions.assertNull(u.getRoles());
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

    @Test
    public void testFindUserByEmailNotFound() {
        User u = generateUser();
        when(userDAO.findUserByEmail(any())).thenReturn(null);
        initService();

        try {
            service.findUserByEmail(u.getEmail());
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
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
            Assertions.fail("Should not fail: " + e.getMessage());
        }
    }

    @Test
    public void testDeleteUserFailure() {
        when(userDAO.findUserByEmail(any())).thenThrow(new NotFoundException());
        initService();
        try {
            service.deleteUserByEmail(RandomStringUtils.random(10, true, false));
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
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

    @Test
    public void testFindUsersByInstitutionIdNullId() {
        initService();
        try {
            service.findUsersByInstitutionId(null);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testFindUsersByInstitutionIdNullInstitution() {
        doThrow(new NotFoundException()).when(institutionDAO).findInstitutionById(anyInt());
        initService();
        try {
            service.findUsersByInstitutionId(1);
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
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
        when(userDAO.getUsersFromInstitutionWithCards(anyInt())).thenReturn(List.of(new User(), new User()));
        initService();

        List<User> users = service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName());
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    public void testGetUsersAsRoleSO_NoInstitution() {
        User u = generateUser();
        u.setInstitutionId(null);
        initService();
        try {
            service.getUsersAsRole(u, UserRoles.SIGNINGOFFICIAL.getRoleName());
        } catch (Exception e) {
            assertTrue(e instanceof NotFoundException);
        }
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
        when(userDAO.findUsersWithLCsAndInstitution()).thenReturn(returnedUsers);
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
        when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(anyInt(), any())).thenReturn(List.of(new UserProperty()));

        initService();
        JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
        assertNotNull(userJson);
        assertTrue(
            userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
        assertTrue(
            userJson.get(UserService.RESEARCHER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
        assertTrue(
            userJson.get(UserService.USER_STATUS_INFO_FIELD).getAsJsonObject().isJsonObject());
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
        when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(anyInt(), any())).thenReturn(List.of(new UserProperty()));

        initService();
        JsonObject userJson = service.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId());
        assertNotNull(userJson);
        assertTrue(
            userJson.get(UserService.LIBRARY_CARDS_FIELD).getAsJsonArray().isJsonArray());
        assertTrue(
            userJson.get(UserService.RESEARCHER_PROPERTIES_FIELD).getAsJsonArray().isJsonArray());
        Assertions.assertNull(userJson.get(UserService.USER_STATUS_INFO_FIELD));
    }

    @Test
    public void testFindOrCreateUser() throws Exception {
        User user = generateUser();
        UserStatus.UserInfo info = new UserStatus.UserInfo()
                .setUserEmail(user.getEmail());
        UserStatus.Enabled enabled = new UserStatus.Enabled()
                .setAllUsersGroup(true)
                .setGoogle(true)
                .setLdap(true);
        UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
        AuthUser authUser = new AuthUser()
                .setEmail(user.getEmail())
                .setAuthToken(RandomStringUtils.random(30, true, false));


        when(userDAO.findUserByEmail(any())).thenReturn(user);
        try {
            when(samDAO.postRegistrationInfo(any())).thenReturn(status);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initService();
        User existingUser = service.findOrCreateUser(authUser);
        assertEquals(existingUser, user);
    }

    @Test
    public void testFindOrCreateUserNewUser() throws Exception {
        User user = generateUser();
        List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
        user.setRoles(roles);
        UserStatus.UserInfo info = new UserStatus.UserInfo()
                .setUserEmail(user.getEmail());
        UserStatus.Enabled enabled = new UserStatus.Enabled()
                .setAllUsersGroup(true)
                .setGoogle(true)
                .setLdap(true);
        UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
        AuthUser authUser = new AuthUser()
                .setName(user.getDisplayName())
                .setEmail(user.getEmail())
                .setAuthToken(RandomStringUtils.random(30, true, false));

        // mock findUserByEmail to throw the NFE on the first call (findOrCreateUser) and then return null (createUser)
        when(userDAO.findUserByEmail(authUser.getEmail())).thenThrow(new NotFoundException()).thenReturn(null);
        when(userDAO.insertUser(any(), any(), any())).thenReturn(user.getUserId());
        when(userRoleDAO.findRoleIdByName(any())).thenReturn(1);
        when(userDAO.findUserById(any())).thenReturn(user);

        try {
            when(samDAO.postRegistrationInfo(any())).thenReturn(status);
        } catch (Exception e) {
            e.printStackTrace();
        }

        spy(userRoleDAO);
        spy(libraryCardDAO);
        spy(userDAO);

        initService();

        User newUser = service.findOrCreateUser(authUser);
        assertEquals(user.getEmail(), newUser.getEmail());
        verify(userRoleDAO, times(1)).insertUserRoles(any(), any());
        verify(libraryCardDAO, times(1)).findAllLibraryCardsByUserEmail(any());
        verify(userDAO, times(1)).insertUser(any(), any(), any());
    }

    @Test
    public void insertUserRoleAndInstitution() {
        boolean encounteredException = false;
        Integer institutionId = 1;
        User testUser = generateUserWithoutInstitution();
        User returnUser = new User();
        returnUser.setUserId(testUser.getUserId());
        returnUser.setEmail(testUser.getEmail());
        returnUser.setDisplayName(testUser.getDisplayName());
        returnUser.setInstitutionId(1);
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        returnUser.addRole(role);
        Assertions.assertNotEquals(testUser.getInstitutionId(), returnUser.getInstitutionId());
        doNothing().when(userServiceDAO).insertRoleAndInstitutionTxn(any(), any(), any());
        when(userDAO.findUserById(anyInt())).thenReturn(returnUser);
        initService();
        try {
            service.insertRoleAndInstitutionForUser(role, institutionId, testUser.getUserId());
        } catch (Exception e) {
            encounteredException = true;
        }
        User fetchedUser = service.findUserById(testUser.getUserId());
        assertEquals(fetchedUser.getUserId(), testUser.getUserId());
        assertEquals(fetchedUser.getInstitutionId(), returnUser.getInstitutionId());
        assertFalse(encounteredException);
    }

    @Test
    public void insertUserRoleAndInstitution_FailingTxn() {
        boolean encounteredException = false;
        Integer institutionId = 1;
        User testUser = generateUserWithoutInstitution();
        Assertions.assertNull(testUser.getInstitutionId());
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        when(userDAO.findUserById(anyInt())).thenReturn(testUser);
        doThrow(new RuntimeException("txn error")).when(userServiceDAO).insertRoleAndInstitutionTxn(any(), any(), any());
        initService();
        try {
            service.insertRoleAndInstitutionForUser(role, institutionId, testUser.getUserId());
        } catch (Exception e) {
            encounteredException = true;
        }
        assertTrue(encounteredException);
    }

    private User generateUserWithoutInstitution() {
        User u = generateUser();
        u.setInstitutionId(null);
        return u;
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
        libraryCard.setInstitutionId(RandomUtils.nextInt(1, 10));
        return libraryCard;
    }

    private UserRole generateRole(int roleId) {
        UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
        assert rolesEnum != null;
        return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
    }

}
