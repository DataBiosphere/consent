package org.broadinstitute.consent.http.service;

import com.google.common.collect.Streams;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.users.handler.DACUserRolesHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class UserRolesHandlerTest {

    @Mock
    private DACUserDAO dacUserDAO;
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
        handler = new DACUserRolesHandler(dacUserDAO, userRoleDAO, electionDAO, voteDAO, datasetAssociationDAO, emailService, dataAccessRequestAPI);
    }

    @Test
    public void testUpdateChairperson() throws Exception {
        DACUser originalChairperson = new DACUser(1, "originalchair@broad.com", "Original Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalChairperson);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateChairpersonWithDelegation() throws Exception {
        DACUser originalChairperson = new DACUser(1, "originalchair@broad.com", "Original Chairperson", new Date(), memberList(), null);
        DACUser delegatedChairperson = new DACUser(2, "delegatedChairperson@broad.com", "Delegated Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        when(voteDAO.findVotesOnOpenElections(originalChairperson.getDacUserId())).thenReturn(randomVotesList(originalChairperson.getDacUserId()));
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(dacUserDAO.findDACUserByEmail("delegatedChairperson@broad.com")).thenReturn(delegatedChairperson);
        when(electionDAO.verifyOpenElections()).thenReturn(1);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalChairperson);
        updateUserMap.put("userToDelegate", delegatedChairperson);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMember() throws Exception {
        DACUser originalMember = new DACUser(1, "originalMember@broad.com", "Original Chairperson", new Date(), alumniList(), null);
        DACUser delegatedMember = new DACUser(2, "delegatedMember@broad.com", "Delegated Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(dacUserDAO.findDACUserByEmail("delegatedMember@broad.com")).thenReturn(delegatedMember);
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalMember);
        updateUserMap.put("userToDelegate", delegatedMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMemberWithDelegation() throws Exception {
        DACUser originalMember = new DACUser(1, "originalMember@broad.com", "Original Chairperson", new Date(), alumniList(), null);
        DACUser delegatedMember = new DACUser(2, "delegatedMember@broad.com", "Delegated Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(voteDAO.findVotesOnOpenElections(originalMember.getDacUserId())).thenReturn(randomVotesList(originalMember.getDacUserId()));
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(alumniList());
        when(dacUserDAO.findDACUserByEmail("delegatedMember@broad.com")).thenReturn(delegatedMember);
        when(electionDAO.verifyOpenElections()).thenReturn(1);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalMember);
        updateUserMap.put("userToDelegate", delegatedMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMultipleRoles() throws Exception {
        DACUser originalDO = new DACUser(1, "originalDO@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(researcherList(), adminList()), null);
        DACUser delegatedDO = new DACUser(2, "delegatedDO@broad.com", "Delegated Chairperson", new Date(), dataownerList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(dataownerList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(concatUserRoleLists(researcherList(), adminList()));
        when(dacUserDAO.findDACUserByEmail("delegatedDO@broad.com")).thenReturn(delegatedDO);
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalDO);
        updateUserMap.put("alternativeDataOwnerUser", delegatedDO);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testPromoteAlumniToMember() throws Exception {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(concatUserRoleLists(alumniList(), researcherList()));
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testPromoteAlumniChairperson() throws Exception {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), chairpersonList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(concatUserRoleLists(alumniList(), researcherList()));
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testAddResearcher() throws Exception {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(researcherList(), memberList()), null);
        DACUser delegatedMember = new DACUser(2, "delegatedMember@broad.com", "Delegated Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(alumniList());
        when(dacUserDAO.findDACUserByEmail("delegatedMember@broad.com")).thenReturn(delegatedMember);
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        when(dacUserDAO.verifyAdminUsers()).thenReturn(2);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        updateUserMap.put("userToDelegate", delegatedMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testAddAlumni() throws Exception {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(alumniList(), memberList()), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        when(dacUserDAO.verifyAdminUsers()).thenReturn(2);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testMemberToChair() throws Exception {
        DACUser originalMember = new DACUser(1, "originalMember@broad.com", "Original Chairperson", new Date(), alumniList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put("updatedUser", originalMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testContainsRole() {
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(getChairpersonRole()));
        boolean result = handler.containsRole(roles, CHAIRPERSON);
        assertTrue("This user is a chairperson ", result);
        result = handler.containsRole(roles, DATAOWNER);
        assertFalse("This user isn't a data owner", result);
    }

    @Test
    public void testContainsAnyRole() {
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
    
    private List<UserRole> concatUserRoleLists(List<UserRole> a, List<UserRole> b) {
        return Streams.concat(a.stream(), b.stream()).collect(Collectors.toList());
    }

    private List<Vote> randomVotesList(Integer dacUserId){
        Vote v1 = new Vote(1, null, dacUserId, new Date(), new Date(), 1, "", "DAC", false, false);
        Vote v2 = new Vote(2, null, dacUserId, new Date(), new Date(), 2, "", "CHAIR", false, false);
        Vote v3 = new Vote(3, null, dacUserId, new Date(), new Date(), 3, "", "DAC", false, false);
        Vote v4 = new Vote(4, null, dacUserId, new Date(), new Date(), 4, "", "CHAIR", false, false);
        return Arrays.asList(v1, v2, v3, v4);
    }

    private List<UserRole> chairpersonList(){
        return Collections.singletonList(getChairpersonRole());
    }

    private List<UserRole> memberList(){
        return Collections.singletonList(getMemberRole());
    }

    private List<UserRole> alumniList(){
        return Collections.singletonList(getAlumniRole());
    }

    private List<UserRole> dataownerList(){
        return Collections.singletonList(getDataOwnerRole());
    }

    private List<UserRole> researcherList(){
        return Collections.singletonList(getResearcherRole());
    }

    private List<UserRole> adminList(){
        return Collections.singletonList(getAdminRole());
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