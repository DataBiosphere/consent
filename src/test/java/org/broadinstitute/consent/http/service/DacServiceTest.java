package org.broadinstitute.consent.http.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DacServiceTest {

    private DacService service;

    @Mock
    private DacDAO dacDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private DataSetDAO dataSetDAO;

    @Mock
    private ElectionDAO electionDAO;

    @Mock
    DataAccessRequestDAO dataAccessRequestDAO;

    @Mock
    private UserService userService;

    @Mock
    private VoteService voteService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new DacService(dacDAO, userDAO, dataSetDAO, electionDAO, dataAccessRequestDAO, userService, voteService);
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
        when(dataSetDAO.findDatasetsByDac(anyInt())).thenReturn(Collections.singleton(getDatasetDTOs().get(0)));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(getDacs());
        initService();

        Set<DataSetDTO> dataSets = service.findDatasetsByDacId(getUser(), 1);
        Assert.assertNotNull(dataSets);
        Assert.assertEquals(1, dataSets.size());
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
        User user = getDacUsers().get(0);
        Dac dac = getDacs().get(0);
        when(userService.findUserById(any())).thenReturn(user);
        when(userService.findUserById(any())).thenReturn(user);
        when(dacDAO.findUserRolesForUser(any())).thenReturn(getDacUsers().get(0).getRoles());
        List<Election> elections = getElections().stream().
                peek(e -> e.setElectionType(ElectionType.DATA_ACCESS.getValue())).
                peek(e -> e.setReferenceId(new ObjectId().toHexString())).
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
        verifyZeroInteractions(dacDAO);
        verifyZeroInteractions(voteService);
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
    public void testFilterDarsByDAC_adminCase() {
        // User is an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<Document> documents = getDocuments();

        List<Document> filtered = service.filterDarsByDAC(documents, getUser());
        // As an admin, all docs should be returned.
        Assert.assertEquals(documents.size(), filtered.size());
    }

    @Test
    public void testFilterDarsByDAC_memberCase_1() {
        // Member is not an admin user
        when(userDAO.findUserByEmailAndRoleId(getMember().getEmail(), UserRoles.MEMBER.getRoleId())).thenReturn(getMember());

        // Member has access to DataSet 1
        List<DataSet> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDataSetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are no additional unassociated datasets
        when(dataSetDAO.findNonDACDataSets()).thenReturn(Collections.emptyList());
        initService();

        List<Document> documents = getDocuments();

        List<Document> filtered = service.filterDarsByDAC(documents, getMemberAuthUser());

        // Filtered documents should only contain the ones the user has direct access to:
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDarsByDAC_memberCase_2() {
        // Member is not an admin user
        when(userDAO.findUserByEmailAndRoleId(getMember().getEmail(), UserRoles.MEMBER.getRoleId())).thenReturn(getMember());

        // Member has access to datasets
        List<DataSet> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDataSetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<DataSet> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDataSets()).thenReturn(unassociatedDataSets);
        initService();

        List<Document> documents = getDocuments();

        List<Document> filtered = service.filterDarsByDAC(documents, getMemberAuthUser());

        // Filtered documents should only contain the ones the user has direct access to
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDarsByDAC_memberCase_3() {
        // Member is not an admin user
        when(userDAO.findUserByEmailAndRoleId(getMember().getEmail(), UserRoles.MEMBER.getRoleId())).thenReturn(getMember());

        // Member no direct access to datasets
        List<DataSet> memberDataSets = Collections.emptyList();
        when(dataSetDAO.findDataSetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<DataSet> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDataSets()).thenReturn(unassociatedDataSets);
        initService();

        List<Document> documents = getDocuments();

        List<Document> filtered = service.filterDarsByDAC(documents, getMemberAuthUser());

        // Filtered documents should contain the ones the user has direct access to
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_adminCase() {
        // User is an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getUser());
        // As an admin, all docs should be returned.
        Assert.assertEquals(dars.size(), filtered.size());
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_memberCase_1() {
        // Member is not an admin user
        when(userDAO.findUserByEmailAndRoleId(getMember().getEmail(), UserRoles.MEMBER.getRoleId())).thenReturn(getMember());

        // Member has access to DataSet 1
        List<DataSet> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDataSetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are no additional unassociated datasets
        when(dataSetDAO.findNonDACDataSets()).thenReturn(Collections.emptyList());
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMemberAuthUser());

        // Filtered documents should only contain the ones the user has direct access to:
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_memberCase_2() {
        // Member is not an admin user
        when(userDAO.findUserByEmailAndRoleId(getMember().getEmail(), UserRoles.MEMBER.getRoleId())).thenReturn(getMember());

        // Member has access to datasets
        List<DataSet> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDataSetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<DataSet> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDataSets()).thenReturn(unassociatedDataSets);
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMemberAuthUser());

        // Filtered documents should only contain the ones the user has direct access to
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDataAccessRequestsByDAC_memberCase_3() {
        // Member is not an admin user
        when(userDAO.findUserByEmailAndRoleId(getMember().getEmail(), UserRoles.MEMBER.getRoleId())).thenReturn(getMember());

        // Member no direct access to datasets
        List<DataSet> memberDataSets = Collections.emptyList();
        when(dataSetDAO.findDataSetsByAuthUserEmail(getMember().getEmail())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<DataSet> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDataSets()).thenReturn(unassociatedDataSets);
        initService();

        List<DataAccessRequest> dars = getDataAccessRequests();

        List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMemberAuthUser());

        // Filtered documents should contain the ones the user has direct access to
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManagesByDAC_adminCase() {
        // User is an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<ConsentManage> manages = getConsentManages();

        List<ConsentManage> filtered = service.filterConsentManageByDAC(manages, getUser());
        // As an admin, all consents should be returned.
        Assert.assertEquals(manages.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManagesByDAC_memberCase1() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        List<ConsentManage> manages = getConsentManages();

        List<ConsentManage> filtered = service.filterConsentManageByDAC(manages, getUser());
        // As a member, only direct-associated consents should be returned.
        Assert.assertEquals(memberDacs.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManagesByDAC_memberCase2() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        // There unassociated consent manages:
        List<ConsentManage> unassociatedManages = getConsentManages().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<ConsentManage> manages = getConsentManages();

        List<ConsentManage> allManages = Stream.
                concat(unassociatedManages.stream(), manages.stream()).
                collect(Collectors.toList());

        List<ConsentManage> filtered = service.filterConsentManageByDAC(allManages, getUser());
        // As a member, direct-associated and unassociated consents should be returned
        Assert.assertEquals(memberDacs.size() + unassociatedManages.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManagesByDAC_memberCase3() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member has no direct access to consented datasets
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(Collections.emptyList());
        initService();

        // There unassociated consent manages:
        List<ConsentManage> unassociatedManages = getConsentManages().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<ConsentManage> manages = getConsentManages();

        List<ConsentManage> allManages = Stream.
                concat(unassociatedManages.stream(), manages.stream()).
                collect(Collectors.toList());

        List<ConsentManage> filtered = service.filterConsentManageByDAC(allManages, getUser());
        // As a member, unassociated consents should be returned
        Assert.assertEquals(unassociatedManages.size(), filtered.size());
    }

    @Test
    public void testFilterConsentsByDAC_adminCase() {
        // User is an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<Consent> consents = getConsents();

        Collection<Consent> filtered = service.filterConsentsByDAC(consents, getUser());
        // As an admin, all consents should be returned.
        Assert.assertEquals(consents.size(), filtered.size());
    }

    @Test
    public void testFilterConsentsByDAC_memberCase1() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        List<Consent> consents = getConsents();

        Collection<Consent> filtered = service.filterConsentsByDAC(consents, getUser());
        // As a member, only direct-associated consents should be returned.
        Assert.assertEquals(memberDacs.size(), filtered.size());
    }

    @Test
    public void testFilterConsentsByDAC_memberCase2() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        // There unassociated consent manages:
        List<Consent> unassociatedConsents = getConsents().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<Consent> consents = getConsents();

        List<Consent> allConsents = Stream.
                concat(unassociatedConsents.stream(), consents.stream()).
                collect(Collectors.toList());

        Collection<Consent> filtered = service.filterConsentsByDAC(allConsents, getUser());
        // As a member, direct-associated and unassociated consents should be returned
        Assert.assertEquals(memberDacs.size() + unassociatedConsents.size(), filtered.size());
    }

    @Test
    public void testFilterConsentsByDAC_memberCase3() {
        // User is not an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member has no direct access to consented datasets
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(Collections.emptyList());
        initService();

        // There unassociated consent manages:
        List<Consent> unassociatedConsents = getConsents().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<Consent> consents = getConsents();

        List<Consent> allConsents = Stream.
                concat(unassociatedConsents.stream(), consents.stream()).
                collect(Collectors.toList());

        Collection<Consent> filtered = service.filterConsentsByDAC(allConsents, getUser());
        // As a member, unassociated consents should be returned
        Assert.assertEquals(unassociatedConsents.size(), filtered.size());
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
        List<DataSet> memberDatasets = Collections.singletonList(getDatasets().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        when(dataSetDAO.findDataSetsByAuthUserEmail(anyString())).thenReturn(memberDatasets);
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
        List<DataSet> memberDatasets = Collections.singletonList(getDatasets().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        when(dataSetDAO.findDataSetsByAuthUserEmail(anyString())).thenReturn(memberDatasets);
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
        when(dataSetDAO.findDataSetsByAuthUserEmail(anyString())).thenReturn(Collections.emptyList());
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
    public void testFindDacsByUser_adminCase() {
        List<Dac> dacs = getDacs();
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(dacs);
        when(dacDAO.findAll()).thenReturn(dacs);
        // User is an admin user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<Dac> dacsForUser = service.findDacsByUser(getUser());
        Assert.assertEquals(dacsForUser.size(), dacs.size());
    }

    @Test
    public void testFindDacsByUser_chairCase() {
        List<Dac> dacs = getDacs();
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(dacs);
        when(dacDAO.findAll()).thenReturn(dacs);
        // User is a chairperson user
        when(userDAO.findUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getChair());
        initService();

        List<Dac> dacsForUser = service.findDacsByUser(getUser());
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
     * @return A list of 5 consents with DAC ids
     */
    private List<Consent> getConsents() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    Consent consent = new Consent();
                    consent.setDacId(i);
                    return consent;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 consentManages with DAC ids
     */
    private List<ConsentManage> getConsentManages() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    ConsentManage manage = new ConsentManage();
                    manage.setDacId(i);
                    return manage;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 documents with DataSet ids
     */
    private List<Document> getDocuments() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    List<Integer> dataSetIds = Collections.singletonList(i);
                    Document doc = new Document();
                    doc.put(DarConstants.DATASET_ID, dataSetIds);
                    return doc;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 DataAccessRequest with DataSet ids and Reference ids
     */
    private List<DataAccessRequest> getDataAccessRequests() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    String referenceId = UUID.randomUUID().toString();
                    List<Integer> dataSetIds = Collections.singletonList(i);
                    DataAccessRequest doc = new DataAccessRequest();
                    doc.setReferenceId(referenceId);
                    DataAccessRequestData data = new DataAccessRequestData();
                    data.setDatasetIds(dataSetIds);
                    data.setReferenceId(referenceId);
                    doc.setData(data);
                    return doc;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 datasets with ids
     */
    private List<DataSet> getDatasets() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    DataSet dataSet = new DataSet();
                    dataSet.setDataSetId(i);
                    return dataSet;
                }).collect(Collectors.toList());
    }

    /**
     * @return A list of 5 datasets with ids
     */
    private List<DataSetDTO> getDatasetDTOs() {
        return IntStream.range(1, 5).
                mapToObj(i -> {
                    DataSetDTO dataSet = new DataSetDTO();
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
        chair.setDacUserId(1);
        chair.setDisplayName("Chair");
        chair.setEmail("chair@duos.org");
        chair.setRoles(new ArrayList<>());
        chair.getRoles().add(new UserRole(1, chair.getDacUserId(), UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName(), 1));
        return chair;
    }

    private User getMember() {
        User member = new User();
        member.setDacUserId(2);
        member.setDisplayName("Member");
        member.setEmail("member@duos.org");
        member.setRoles(new ArrayList<>());
        member.getRoles().add(new UserRole(2, member.getDacUserId(), UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName(), 1));
        return member;
    }

    private AuthUser getMemberAuthUser() {
        return new AuthUser(getMember().getEmail());
    }

}
