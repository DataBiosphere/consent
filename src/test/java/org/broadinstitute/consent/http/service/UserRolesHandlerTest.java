package org.broadinstitute.consent.http.service;

import com.google.common.collect.Streams;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
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
    private DataAccessRequestAPI dataAccessRequestAPI;

    private DACUserRolesHandler handler;


    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        handler = new DACUserRolesHandler(dacUserDAO, userRoleDAO, electionDAO, voteDAO, datasetAssociationDAO, dataAccessRequestAPI);
    }

    @Test
    public void testUpdateChairperson() {
        DACUser originalChairperson = new DACUser(1, "originalchair@broad.com", "Original Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalChairperson);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMember() {
        DACUser originalMember = new DACUser(1, "originalMember@broad.com", "Original Chairperson", new Date(), alumniList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalMember);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testUpdateMultipleRoles() {
        DACUser originalDO = new DACUser(1, "originalDO@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(researcherList(), adminList()), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(dataownerList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(concatUserRoleLists(researcherList(), adminList()));
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalDO);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testPromoteAlumniToMember() {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), memberList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(concatUserRoleLists(alumniList(), researcherList()));
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testPromoteAlumniChairperson() {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), chairpersonList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(concatUserRoleLists(alumniList(), researcherList()));
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testAddResearcher() {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(researcherList(), memberList()), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(alumniList());
        when(userRoleDAO.findRolesByUserId(2)).thenReturn(memberList());
        when(dacUserDAO.verifyAdminUsers()).thenReturn(2);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testAddAlumni() {
        DACUser originalAlumni = new DACUser(1, "originalAlumni@broad.com", "Original Chairperson", new Date(), concatUserRoleLists(alumniList(), memberList()), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(memberList());
        when(dacUserDAO.verifyAdminUsers()).thenReturn(2);
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalAlumni);
        handler.updateRoles(updateUserMap);
    }

    @Test
    public void testMemberToChair() {
        DACUser originalMember = new DACUser(1, "originalMember@broad.com", "Original Chairperson", new Date(), alumniList(), null);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(chairpersonList());
        Map<String, DACUser> updateUserMap = new HashMap<>();
        updateUserMap.put(DACUserRolesHandler.UPDATED_USER_KEY, originalMember);
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