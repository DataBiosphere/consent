package org.broadinstitute.consent.http.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.ListUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.users.handler.DACUserRolesHandler;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class UserRolesHandlerTest {

    @Mock
    private UserDAO userDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private UserRoleDAO userRoleDAO;
    @Mock
    private DataSetAssociationDAO datasetAssociationDAO;
    @Mock
    private EmailNotifierService emailService;
    @Mock
    private DataAccessRequestAPI dataAccessRequestAPI;

    DACUserRolesHandler handler;


    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        handler = new DACUserRolesHandler(userDAO, userRoleDAO, electionDAO, voteDAO, datasetAssociationDAO, emailService, dataAccessRequestAPI);
    }

    @Test
    public void testUpdateChairperson() throws Exception {
        User originalChairperson = new User(1, "originalchair@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalChairperson);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateChairpersonWithDelegation() throws Exception {
        User originalChairperson = new User(1, "originalchair@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList(), null);
        User delegatedChairperson = new User(2, "delegatedChairperson@broad.com", "Delegated Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        when(voteDAO.findVotesOnOpenElections(originalChairperson.getUserId())).thenReturn(randomVotesList(originalChairperson.getUserId()));
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(userDAO.findDACUserByEmail("delegatedChairperson@broad.com")).thenReturn(delegatedChairperson);
        when(electionDAO.verifyOpenElections()).thenReturn(1);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalChairperson);
        updateUserMap.put("userToDelegate", delegatedChairperson);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMember() throws Exception {
        User originalMember = new User(1, "originalMember@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), alumniList(), null);
        User delegatedMember = new User(2, "delegatedMember@broad.com", "Delegated Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(userDAO.findDACUserByEmail("delegatedMember@broad.com")).thenReturn(delegatedMember);
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalMember);
        updateUserMap.put("userToDelegate", delegatedMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMemberWithDelegation() throws Exception {
        User originalMember = new User(1, "originalMember@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), alumniList(), null);
        User delegatedMember = new User(2, "delegatedMember@broad.com", "Delegated Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(voteDAO.findVotesOnOpenElections(originalMember.getUserId())).thenReturn(randomVotesList(originalMember.getUserId()));
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(alumniList());
        when(userDAO.findDACUserByEmail("delegatedMember@broad.com")).thenReturn(delegatedMember);
        when(electionDAO.verifyOpenElections()).thenReturn(1);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalMember);
        updateUserMap.put("userToDelegate", delegatedMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMultipleRoles() throws Exception {
        User originalDO = new User(1, "originalDO@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), ListUtils.union(researcherList(), adminList()), null);
        User delegatedDO = new User(2, "delegatedDO@broad.com", "Delegated Chairperson", RoleStatus.PENDING.toString(), new Date(), dataownerList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(dataownerList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(ListUtils.union(researcherList(), adminList()));
        when(userDAO.findDACUserByEmail("delegatedDO@broad.com")).thenReturn(delegatedDO);
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalDO);
        updateUserMap.put("alternativeDataOwnerUser", delegatedDO);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testPromoteAlumniToMember() throws Exception {
        User originalAlumni = new User(1, "originalAlumni@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(ListUtils.union(alumniList(), researcherList()));
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testPromoteAlumniChairperson() throws Exception {
        User originalAlumni = new User(1, "originalAlumni@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), chairpersonList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(ListUtils.union(alumniList(), researcherList()));
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test(expected = UserRoleHandlerException.class)
    public void testAddResearcherException() throws Exception {
        User originalAlumni = new User(1, "originalAlumni@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), ListUtils.union(researcherList(), memberList()), null);
        User delegatedMember = new User(2, "delegatedMember@broad.com", "Delegated Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(alumniList());
        when(userDAO.findDACUserByEmail("delegatedMember@broad.com")).thenReturn(delegatedMember);
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        when(userDAO.verifyAdminUsers()).thenReturn(2);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        updateUserMap.put("userToDelegate", delegatedMember);
        handler.updateRoles(updateUserMap);
    }

    @Test(expected = UserRoleHandlerException.class)
    public void testAddAlumniException() throws Exception {
        User originalAlumni = new User(1, "originalAlumni@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), ListUtils.union(alumniList(), memberList()), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        when(userDAO.verifyAdminUsers()).thenReturn(2);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testMemberToChair() throws Exception {
        User originalMember = new User(1, "originalMember@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), alumniList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, User> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testContainsRole() throws Exception {
        List<UserRole> roles = new ArrayList<>(Arrays.asList(getChairpersonRole()));
        boolean result = handler.containsRole(roles, CHAIRPERSON);
        assertTrue("This user is a chairperson ", result);
        result = handler.containsRole(roles, DATAOWNER);
        assertFalse("This user isn't a data owner", result);
    }

    @Test
    public void testContainsAnyRole() throws Exception {
        List<UserRole> roles = new ArrayList<>(Arrays.asList(getChairpersonRole(), getDataOwnerRole(), getAdminRole()));
        assertTrue("This user has admin role ", handler.containsAnyRole(roles, new String[]{ADMIN, RESEARCHER}));
        assertFalse("This user is not an alumni ", handler.containsAnyRole(roles, new String[]{ALUMNI, RESEARCHER}));
    }

    /** Private helper methods **/

    private static final String CHAIRPERSON = "Chairperson";
    private static final String DACMEMBER = "Member";
    private static final String ALUMNI = "Alumni";
    private static final String DATAOWNER = "DataOwner";
    private static final String RESEARCHER = "Researcher";
    private static final String ADMIN = "Admin";

    private List<Vote> randomVotesList(Integer dacUserId){
        Vote v1 = new Vote(1, null, dacUserId, new Date(), new Date(), 1, "", "DAC", false, false);
        Vote v2 = new Vote(2, null, dacUserId, new Date(), new Date(), 2, "", "CHAIR", false, false);
        Vote v3 = new Vote(3, null, dacUserId, new Date(), new Date(), 3, "", "DAC", false, false);
        Vote v4 = new Vote(4, null, dacUserId, new Date(), new Date(), 4, "", "CHAIR", false, false);
        return Arrays.asList(v1, v2, v3, v4);
    }

    private List<UserRole> chairpersonList(){
        return Arrays.asList(getChairpersonRole());
    }

    private List<UserRole> memberList(){
        return Arrays.asList(getMemberRole());
    }

    private List<UserRole> alumniList(){
        return Arrays.asList(getAlumniRole());
    }

    private List<UserRole> dataownerList(){
        return Arrays.asList(getDataOwnerRole());
    }

    private List<UserRole> researcherList(){
        return Arrays.asList(getResearcherRole());
    }

    private List<UserRole> adminList(){
        return Arrays.asList(getAdminRole());
    }

    private UserRole getMemberRole() {
        return new UserRole(1, DACMEMBER);
    }

    private UserRole getChairpersonRole() {
        return new UserRole(2, CHAIRPERSON);
    }

    private UserRole getAlumniRole() {
        return new UserRole(3, ALUMNI);
    }

    private UserRole getAdminRole() {
        return new UserRole(4, ADMIN);
    }

    private UserRole getResearcherRole() {
        return new UserRole(5, RESEARCHER);
    }

    private UserRole getDataOwnerRole() {
        return new UserRole(6, DATAOWNER);
    }

}