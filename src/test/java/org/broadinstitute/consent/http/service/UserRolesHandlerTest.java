package org.broadinstitute.consent.http.service;

import com.google.common.collect.Streams;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
    private DataAccessRequestService dataAccessRequestService;

    UserRolesHandler handler;


    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        handler = new UserRolesHandler(dacUserDAO, dataAccessRequestService, electionDAO, userRoleDAO, voteDAO);
    }

    @Test
    public void testUpdateChairperson() {
        // TODO: Should fail
    }

    @Test
    public void testUpdateMember() {
        // TODO: Should fail
    }

    @Test
    public void testUpdateMultipleRoles() {
        // TODO: Fix
        DACUser originalDO = new DACUser(1, "originalDO@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(researcherList(), adminList()), null);
        DACUser delegatedDO = new DACUser(2, "delegatedDO@broad.com", "Delegated Chairperson", new Date(), dataownerList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(dataownerList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(concatUserRoleLists(researcherList(), adminList()));
        when(dacUserDAO.findDACUserByEmail("delegatedDO@broad.com")).thenReturn(delegatedDO);
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        handler.updateRoles(originalDO);
    }

    @Test
    public void testPromoteAlumniToMember() {
        // TODO: Fix
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(concatUserRoleLists(alumniList(), researcherList()));
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        handler.updateRoles(originalAlumni);
    }

    @Test
    public void testPromoteAlumniChairperson() {
        // TODO: Should FAIL
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), chairpersonList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(concatUserRoleLists(alumniList(), researcherList()));
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        handler.updateRoles(originalAlumni);
    }

    @Test
    public void testAddResearcher() {
        // TODO: Fix
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(researcherList(), memberList()), null);
        DACUser delegatedMember = new DACUser(2, "delegatedMember@broad.com", "Delegated Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(alumniList());
        when(dacUserDAO.findDACUserByEmail("delegatedMember@broad.com")).thenReturn(delegatedMember);
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        when(dacUserDAO.verifyAdminUsers()).thenReturn(2);
        handler.updateRoles(originalAlumni);
    }

    @Test
    public void testAddAlumni() {
        // TODO: Fix
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(alumniList(), memberList()), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        when(dacUserDAO.verifyAdminUsers()).thenReturn(2);
        handler.updateRoles(originalAlumni);
    }

    @Test
    public void testMemberToChair() {
        // TODO: SHOULD FAIL
        DACUser originalMember = new DACUser(1, "originalMember@broad.com", "Original Chairperson", new Date(), alumniList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        handler.updateRoles(originalMember);
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

    private static final String CHAIRPERSON = UserRoles.CHAIRPERSON.getRoleName();
    private static final String MEMBER = UserRoles.MEMBER.getRoleName();
    private static final String ALUMNI = UserRoles.ALUMNI.getRoleName();
    private static final String DATAOWNER = UserRoles.DATAOWNER.getRoleName();
    private static final String RESEARCHER = UserRoles.RESEARCHER.getRoleName();
    private static final String ADMIN = UserRoles.ADMIN.getRoleName();

    private List<UserRole> concatUserRoleLists(List<UserRole> a, List<UserRole> b) {
        return Streams.concat(a.stream(), b.stream()).collect(Collectors.toList());
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
        return new UserRole(1, MEMBER);
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