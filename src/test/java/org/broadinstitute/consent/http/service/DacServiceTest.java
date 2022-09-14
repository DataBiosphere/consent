package org.broadinstitute.consent.http.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.ws.rs.BadRequestException;

import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DacServiceTest {

    private DacService service;

    @Mock
    private DacDAO dacDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private DatasetDAO dataSetDAO;

    @Mock
    private ElectionDAO electionDAO;

    @Mock
    DataAccessRequestDAO dataAccessRequestDAO;

    @Mock
    private VoteService voteService;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initService() {
        service = new DacService(dacDAO, userDAO, dataSetDAO, electionDAO, dataAccessRequestDAO, voteService);
    }

    @Test
    public void testFindAll() {
        when(dacDAO.findAll()).thenReturn(Collections.emptyList());
        initService();

        Assert.assertTrue(service.findAll().isEmpty());
    }

    @Test
    public void testFindAllDACUsersBySearchString_case1() {
        when(dacDAO.findAll()).thenReturn(Collections.emptyList());
        when(dacDAO.findAllDACUserMemberships()).thenReturn(Collections.emptyList());
        initService();

        Assert.assertTrue(service.findAllDacsWithMembers().isEmpty());
    }

    @Test
    public void testFindAllDACUsersBySearchString_case2() {
        when(dacDAO.findAll()).thenReturn(getDacs());
        when(dacDAO.findAllDACUserMemberships()).thenReturn(getDacUsers());
        initService();

        List<Dac> dacs = service.findAllDacsWithMembers();
        Assert.assertFalse(dacs.isEmpty());
        Assert.assertEquals(dacs.size(), getDacs().size());
        List<Dac> dacsWithMembers = dacs.
                stream().
                filter(d -> !d.getChairpersons().isEmpty()).
                filter(d -> !d.getMembers().isEmpty()).
                collect(Collectors.toList());
        Assert.assertFalse(dacsWithMembers.isEmpty());
        Assert.assertEquals(1, dacsWithMembers.size());
    }

    @Test
    public void testFindById() {
        int dacId = 1;
        when(dacDAO.findById(dacId)).thenReturn(getDacs().get(0));
        when(dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.CHAIRPERSON.getRoleId())).thenReturn(Collections.singletonList(getDacUsers().get(0)));
        when(dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.MEMBER.getRoleId())).thenReturn(Collections.singletonList(getDacUsers().get(1)));
        initService();

        Dac dac = service.findById(dacId);
        Assert.assertNotNull(dac);
        Assert.assertFalse(dac.getChairpersons().isEmpty());
        Assert.assertFalse(dac.getMembers().isEmpty());
    }

    @Test
    public void testCreateDac() {
        when(dacDAO.createDac(anyString(), anyString(), any())).thenReturn(getDacs().get(0).getDacId());
        initService();

        Integer dacId = service.createDac("name", "description");
        Assert.assertEquals(getDacs().get(0).getDacId(), dacId);
    }

    @Test
    public void testUpdateDac() {
        doNothing().when(dacDAO).updateDac(anyString(), anyString(), any(), any());
        initService();

        try {
            service.updateDac("name", "description", 1);
        } catch (Exception e) {
            Assert.fail("Update should not fail");
        }
    }

    @Test
    public void testDeleteDac() {
        doNothing().when(dacDAO).deleteDacMembers(anyInt());
        doNothing().when(dacDAO).deleteDac(anyInt());
        initService();

        try {
            service.deleteDac(1);
        } catch (Exception e) {
            Assert.fail("Delete should not fail");
        }
    }

    @Test
    public void testFindDatasetsByDacId() {

        List<Dataset> datasets = getDatasets();
        when(dataSetDAO.findDatasetsAssociatedWithDac(1)).thenReturn(datasets);
        initService();

        List<Dataset> returned = service.findDatasetsByDacId( 1);
        Assert.assertNotNull(returned);
        Assert.assertEquals(datasets, returned);
    }

    @Test
    public void testFindMembersByDacId() {
        when(dacDAO.findMembersByDacId(anyInt())).thenReturn(Collections.singletonList(getDacUsers().get(0)));
        when(dacDAO.findUserRolesForUsers(any())).thenReturn(getDacUsers().get(0).getRoles());
        initService();

        List<User> users = service.findMembersByDacId(1);
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testAddDacMember() {
        Gson gson = new Gson();
        User user = getDacUsers().get(0);
        Dac dac = getDacs().get(0);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(dacDAO.findUserRolesForUser(any())).thenReturn(getDacUsers().get(0).getRoles());
        List<Election> elections = getElections().stream().
                map(e -> {
                    Election newE = gson.fromJson(gson.toJson(e), Election.class);
                    newE.setElectionType(ElectionType.DATA_ACCESS.getValue());
                    newE.setReferenceId(UUID.randomUUID().toString());
                    return newE;
                }).
                collect(Collectors.toList());
        DataAccessRequest dar = new DataAccessRequest();
        dar.setData(new DataAccessRequestData());
        dar.getData().setRestriction(new Everything());
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        when(electionDAO.findOpenElectionsByDacId(any())).thenReturn(elections);
        when(voteService.createVotes(any(), any(), anyBoolean())).thenReturn(Collections.emptyList());
        doNothing().when(dacDAO).addDacMember(anyInt(), anyInt(), anyInt());
        initService();

        Role role = new Role(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        User user1 = service.addDacMember(role, user, dac);
        Assert.assertNotNull(user1);
        Assert.assertFalse(user1.getRoles().isEmpty());
        verify(voteService, times(elections.size())).createVotesForUser(any(), any(), any(), anyBoolean());
    }

    @Test
    public void testRemoveDacMember() {
        Role role = new Role(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
        Dac dac = getDacs().get(0);
        User member = getDacUsers().get(1);
        dac.setChairpersons(Collections.singletonList(getDacUsers().get(0)));
        dac.setMembers(Collections.singletonList(member));
        doNothing().when(dacDAO).removeDacMember(anyInt());
        doNothing().when(voteService).deleteOpenDacVotesForUser(any(), any());
        initService();

        try {
            service.removeDacMember(role, member, dac);
        } catch (Exception e) {
            Assert.fail();
        }
        verify(dacDAO, atLeastOnce()).removeDacMember(anyInt());
        verify(voteService, atLeastOnce()).deleteOpenDacVotesForUser(any(), any());
    }

    @Test
    public void testRemoveDacChair() {
        Role role = new Role(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        Dac dac = getDacs().get(0);
        User chair1 = getDacUsers().get(0);
        User chair2 = getDacUsers().get(0);
        dac.setChairpersons(Arrays.asList(chair1, chair2));
        dac.setMembers(Collections.singletonList(getDacUsers().get(1)));
        doNothing().when(dacDAO).removeDacMember(anyInt());
        doNothing().when(voteService).deleteOpenDacVotesForUser(any(), any());
        initService();

        try {
            service.removeDacMember(role, chair1, dac);
        } catch (Exception e) {
            Assert.fail();
        }
        verify(dacDAO, atLeastOnce()).removeDacMember(anyInt());
        verify(voteService, atLeastOnce()).deleteOpenDacVotesForUser(any(), any());
    }

    @Test(expected = BadRequestException.class)
    public void testRemoveDacChairFailure() {
        Role role = new Role(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        Dac dac = getDacs().get(0);
        User chair = getDacUsers().get(0);
        dac.setChairpersons(Collections.singletonList(chair));
        dac.setMembers(Collections.singletonList(getDacUsers().get(1)));
        doNothing().when(dacDAO).removeDacMember(anyInt());
        doNothing().when(voteService).deleteOpenDacVotesForUser(any(), any());
        initService();

        service.removeDacMember(role, chair, dac);
        verify(dacDAO, times(0)).removeDacMember(anyInt());
        verify(voteService, times(0)).deleteOpenDacVotesForUser(any(), any());
    }

    @Test
    public void testIsAuthUserAdmin_case1() {
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        Assert.assertTrue(service.isAuthUserAdmin(getUser()));
    }

    @Test
    public void testIsAuthUserAdmin_case2() {
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);
        initService();

        Assert.assertFalse(service.isAuthUserAdmin(getUser()));
    }

    @Test
    public void testIsAuthUserChair_case1() {
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        Assert.assertTrue(service.isAuthUserAdmin(getUser()));
    }

    @Test
    public void testIsAuthUserChair_case2() {
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);
        initService();

        Assert.assertFalse(service.isAuthUserAdmin(getUser()));
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_adminCase() {
        User user = new User();
        user.setRoles(new ArrayList<>());
        user.getRoles().add(new UserRole( UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));

        // User is an admin user
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, user);
        // As an admin, all docs should be returned.
        Assert.assertEquals(dars.size(), filtered.size());
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_memberCase_1() {
        // Member has access to DataSet 1
        List<Dataset> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDatasetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are no additional unassociated datasets
        when(dataSetDAO.findNonDACDatasets()).thenReturn(Collections.emptyList());
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMember());

        // Filtered documents should only contain the ones the user has direct access to:
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_memberCase_2() {
        // Member has access to datasets
        List<Dataset> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDatasetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<Dataset> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDatasets()).thenReturn(unassociatedDataSets);
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMember());

        // Filtered documents should only contain the ones the user has direct access to
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_memberCase_3() {
        // Member no direct access to datasets
        List<Dataset> memberDataSets = Collections.emptyList();
        when(dataSetDAO.findDatasetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<Dataset> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDatasets()).thenReturn(unassociatedDataSets);
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMember());

        // Filtered documents should contain the ones the user has direct access to
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterElectionsByDAC_adminCase() {
        // User is an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<Election> elections = getElections();

        Collection<Election> filtered = service.filterElectionsByDAC(elections, getUser());
        // As an admin, all consents should be returned.
        Assert.assertEquals(elections.size(), filtered.size());
    }

    @Test
    public void testFilterElectionsByDAC_memberCase1() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consented dataset
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        List<Dataset> memberDatasets = Collections.singletonList(getDatasets().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        when(dataSetDAO.findDatasetsByAuthUserEmail(anyString())).thenReturn(memberDatasets);
        initService();

        List<Election> elections = getElections();

        Collection<Election> filtered = service.filterElectionsByDAC(elections, getUser());
        // As a member, only direct-associated consents should be returned.
        Assert.assertEquals(memberDatasets.size(), filtered.size());
    }

    @Test
    public void testFilterElectionsByDAC_memberCase2() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consented dataset
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        List<Dataset> memberDatasets = Collections.singletonList(getDatasets().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        when(dataSetDAO.findDatasetsByAuthUserEmail(anyString())).thenReturn(memberDatasets);
        initService();

        // There are unassociated elections:
        List<Election> unassociatedElections = getElections().stream().
                peek(e -> e.setDataSetId(null)).
                collect(Collectors.toList());

        List<Election> elections = getElections();

        List<Election> allElections = Stream.
                concat(unassociatedElections.stream(), elections.stream()).
                collect(Collectors.toList());

        Collection<Election> filtered = service.filterElectionsByDAC(allElections, getUser());
        // As a member, both direct-associated and unassociated elections should be returned.
        Assert.assertEquals(memberDatasets.size() + unassociatedElections.size(), filtered.size());
    }

    @Test
    public void testFilterElectionsByDAC_memberCase3() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member has no direct access to elections via DAC or DataSet
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(Collections.emptyList());
        when(dataSetDAO.findDatasetsByAuthUserEmail(anyString())).thenReturn(Collections.emptyList());
        initService();

        // There are unassociated elections:
        List<Election> unassociatedElections = getElections().stream().
                peek(e -> e.setDataSetId(null)).
                collect(Collectors.toList());

        List<Election> elections = getElections();

        List<Election> allElections = Stream.
                concat(unassociatedElections.stream(), elections.stream()).
                collect(Collectors.toList());

        Collection<Election> filtered = service.filterElectionsByDAC(allElections, getUser());
        // As a member, both direct-associated and unassociated elections should be returned.
        Assert.assertEquals(unassociatedElections.size(), filtered.size());
    }

    @Test
    public void testFindDacsByUserAdminCase() {
        List<Dac> dacs = getDacs();
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(dacs);
        when(dacDAO.findAll()).thenReturn(dacs);
        // User is an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<Dac> dacsForUser = service.findDacsWithMembersOption(false);
        Assert.assertEquals(dacsForUser.size(), dacs.size());
    }

    @Test
    public void testFindDacsByUserChairCase() {
        List<Dac> dacs = getDacs();
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(dacs);
        when(dacDAO.findAll()).thenReturn(dacs);
        // User is a chairperson user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getChair());
        initService();

        List<Dac> dacsForUser = service.findDacsWithMembersOption(false);
        Assert.assertEquals(dacsForUser.size(), dacs.size());
    }


    /* Helper functions */


    private AuthUser getUser() {
        return new AuthUser("User");
    }

    /**
     * @return A list of 5 elections with DataSet ids
     */
    private List<Election> getElections() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    Election election = new Election();
                    election.setDataSetId(i);
                    return election;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 consents
     */
    private List<Consent> getConsents() {
        return IntStream.range(1, 5).
                mapToObj(i -> new Consent()).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 DataAccessRequest with DataSet ids and Reference ids
     */
    private List<DataAccessRequest> getDataAccessRequests() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    String referenceId = UUID.randomUUID().toString();
                    List<Integer> dataSetIds = Collections.singletonList(i);
                    DataAccessRequest dar = new DataAccessRequest();
                    dar.setReferenceId(referenceId);
                    DataAccessRequestData data = new DataAccessRequestData();
                    dar.setDatasetIds(dataSetIds);
                    data.setReferenceId(referenceId);
                    dar.setData(data);
                    return dar;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 datasets with ids
     */
    private List<Dataset> getDatasets() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    Dataset dataSet = new Dataset();
                    dataSet.setDataSetId(i);
                    return dataSet;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 datasets with ids
     */
    private List<DatasetDTO> getDatasetDTOs() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    DatasetDTO dataSet = new DatasetDTO();
                    dataSet.setDataSetId(i);
                    return dataSet;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 dacs
     */
    private List<Dac> getDacs() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    Dac dac = new Dac();
                    dac.setDacId(i);
                    dac.setDescription("Dac " + i);
                    dac.setName("Dac " + i);
                    return dac;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of two users in a single DAC
     */
    private List<User> getDacUsers() {
        return Arrays.asList(getChair(), getMember());
    }

    private User getChair() {
        User chair = new User();
        chair.setUserId(1);
        chair.setDisplayName("Chair");
        chair.setEmail("chair@duos.org");
        chair.setRoles(new ArrayList<>());
        chair.getRoles().add(new UserRole(1, chair.getUserId(), UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName(), 1));
        return chair;
    }

    private User getMember() {
        User member = new User();
        member.setUserId(2);
        member.setDisplayName("Member");
        member.setEmail("member@duos.org");
        member.setRoles(new ArrayList<>());
        member.getRoles().add(new UserRole(2, member.getUserId(), UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName(), 1));
        return member;
    }

}
