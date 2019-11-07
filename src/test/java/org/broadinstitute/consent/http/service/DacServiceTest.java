package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class DacServiceTest {

    private DacService service;

    @Mock
    DacDAO dacDAO;

    @Mock
    DACUserDAO dacUserDAO;

    @Mock
    DataSetDAO dataSetDAO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new DacService(dacDAO, dacUserDAO, dataSetDAO);
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
    public void testFindUserById() {
        when(dacDAO.findUserById(anyInt())).thenReturn(getDacUsers().get(0));
        when(dacDAO.findUserRolesForUser(anyInt())).thenReturn(getDacUsers().get(0).getRoles());
        initService();

        DACUser user = service.findUserById(1);
        Assert.assertNotNull(user);
        Assert.assertFalse(user.getRoles().isEmpty());
    }

    @Test
    public void testFindMembersByDacId() {
        when(dacDAO.findMembersByDacId(anyInt())).thenReturn(Collections.singletonList(getDacUsers().get(0)));
        when(dacDAO.findUserRolesForUsers(any())).thenReturn(getDacUsers().get(0).getRoles());
        initService();

        List<DACUser> users = service.findMembersByDacId(1);
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testAddDacMember() {
        when(dacDAO.findUserById(anyInt())).thenReturn(getDacUsers().get(0));
        when(dacDAO.findUserRolesForUser(anyInt())).thenReturn(getDacUsers().get(0).getRoles());
        doNothing().when(dacDAO).addDacMember(anyInt(), anyInt(), anyInt());
        initService();

        Role role = new Role(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
        DACUser user = service.addDacMember(role, getDacUsers().get(0), getDacs().get(0));
        Assert.assertNotNull(user);
        Assert.assertFalse(user.getRoles().isEmpty());
    }

    @Test
    public void testIsAuthUserAdmin_case1() {
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        AuthUser user = new AuthUser("Admin");
        Assert.assertTrue(service.isAuthUserAdmin(user));
    }

    @Test
    public void testIsAuthUserAdmin_case2() {
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);
        initService();

        AuthUser user = new AuthUser("Admin");
        Assert.assertFalse(service.isAuthUserAdmin(user));
    }

    @Test
    public void testIsAuthUserChair_case1() {
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        AuthUser user = new AuthUser("Chair");
        Assert.assertTrue(service.isAuthUserAdmin(user));
    }

    @Test
    public void testIsAuthUserChair_case2() {
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);
        initService();

        AuthUser user = new AuthUser("Chair");
        Assert.assertFalse(service.isAuthUserAdmin(user));
    }

    @Test
    public void testFilterDarsByDAC_adminCase() {
        // User is an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<Document> documents = getDocuments();
        AuthUser user = new AuthUser("Admin");

        List<Document> filtered = service.filterDarsByDAC(documents, user);
        // As an admin, all docs should be returned.
        Assert.assertEquals(documents.size(), filtered.size());
    }

    @Test
    public void testFilterDarsByDAC_memberCase_1() {
        // Member is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member has access to DataSet 1
        List<DataSet> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDataSetsByAuthUserEmail(anyString())).thenReturn(memberDataSets);

        // There are no additional unassociated datasets
        when(dataSetDAO.findNonDACDataSets()).thenReturn(Collections.emptyList());
        initService();

        List<Document> documents = getDocuments();
        AuthUser user = new AuthUser("Chair");

        List<Document> filtered = service.filterDarsByDAC(documents, user);

        // Filtered documents should only contain the ones the user has direct access to:
        Assert.assertEquals(memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDarsByDAC_memberCase_2() {
        // Member is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member has access to datasets
        List<DataSet> memberDataSets = Collections.singletonList(getDatasets().get(0));
        when(dataSetDAO.findDataSetsByAuthUserEmail(anyString())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<DataSet> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDataSets()).thenReturn(unassociatedDataSets);
        initService();

        List<Document> documents = getDocuments();
        AuthUser user = new AuthUser("Chair");

        List<Document> filtered = service.filterDarsByDAC(documents, user);

        // Filtered documents should contain the ones the user has direct access to in addition to
        // the unassociated ones:
        Assert.assertEquals(unassociatedDataSets.size() + memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterDarsByDAC_memberCase_3() {
        // Member is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member no direct access to datasets
        List<DataSet> memberDataSets = Collections.emptyList();
        when(dataSetDAO.findDataSetsByAuthUserEmail(anyString())).thenReturn(memberDataSets);

        // There are additional unassociated datasets
        List<DataSet> unassociatedDataSets = getDatasets().subList(1, getDatasets().size());
        when(dataSetDAO.findNonDACDataSets()).thenReturn(unassociatedDataSets);
        initService();

        List<Document> documents = getDocuments();
        AuthUser user = new AuthUser("Chair");

        List<Document> filtered = service.filterDarsByDAC(documents, user);

        // Filtered documents should contain the ones the user has direct access to in addition to
        // the unassociated ones:
        Assert.assertEquals(unassociatedDataSets.size() + memberDataSets.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManageByDAC_adminCase() {
        // User is an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<ConsentManage> manages = getConsentManages();
        AuthUser user = new AuthUser("Admin");

        List<ConsentManage> filtered = service.filterConsentManageByDAC(manages, user);
        // As an admin, all consents should be returned.
        Assert.assertEquals(manages.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManageByDAC_memberCase1() {
        // User is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        List<ConsentManage> manages = getConsentManages();
        AuthUser user = new AuthUser("Admin");

        List<ConsentManage> filtered = service.filterConsentManageByDAC(manages, user);
        // As a member, only direct-associated consents should be returned.
        Assert.assertEquals(memberDacs.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManageByDAC_memberCase2() {
        // User is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        // There unassociated consent manages:
        List<ConsentManage> unassociatedManages = getConsentManages().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<ConsentManage> manages = getConsentManages();
        AuthUser user = new AuthUser("Admin");

        List<ConsentManage> allManages = Stream.
                concat(unassociatedManages.stream(), manages.stream()).
                collect(Collectors.toList());

        List<ConsentManage> filtered = service.filterConsentManageByDAC(allManages, user);
        // As a member, direct-associated and unassociated consents should be returned
        Assert.assertEquals(memberDacs.size() + unassociatedManages.size(), filtered.size());
    }

    @Test
    public void testFilterConsentManageByDAC_memberCase3() {
        // User is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member has no direct access to consented datasets
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(Collections.emptyList());
        initService();

        // There unassociated consent manages:
        List<ConsentManage> unassociatedManages = getConsentManages().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<ConsentManage> manages = getConsentManages();
        AuthUser user = new AuthUser("Admin");

        List<ConsentManage> allManages = Stream.
                concat(unassociatedManages.stream(), manages.stream()).
                collect(Collectors.toList());

        List<ConsentManage> filtered = service.filterConsentManageByDAC(allManages, user);
        // As a member, unassociated consents should be returned
        Assert.assertEquals(unassociatedManages.size(), filtered.size());
    }

    @Test
    public void testFilterConsentByDAC_adminCase() {
        // User is an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(getDacUsers().get(0));
        initService();

        List<Consent> consents = getConsents();
        AuthUser user = new AuthUser("Admin");

        Collection<Consent> filtered = service.filterConsentsByDAC(consents, user);
        // As an admin, all consents should be returned.
        Assert.assertEquals(consents.size(), filtered.size());
    }

    @Test
    public void testFilterConsentByDAC_memberCase1() {
        // User is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        List<Consent> consents = getConsents();
        AuthUser user = new AuthUser("Admin");

        Collection<Consent> filtered = service.filterConsentsByDAC(consents, user);
        // As a member, only direct-associated consents should be returned.
        Assert.assertEquals(memberDacs.size(), filtered.size());
    }

    @Test
    public void testFilterConsentByDAC_memberCase2() {
        // User is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member is a member of one DAC that has a single consent
        List<Dac> memberDacs = Collections.singletonList(getDacs().get(0));
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(memberDacs);
        initService();

        // There unassociated consent manages:
        List<Consent> unassociatedConsents = getConsents().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<Consent> consents = getConsents();
        AuthUser user = new AuthUser("Admin");

        List<Consent> allConsents = Stream.
                concat(unassociatedConsents.stream(), consents.stream()).
                collect(Collectors.toList());

        Collection<Consent> filtered = service.filterConsentsByDAC(allConsents, user);
        // As a member, direct-associated and unassociated consents should be returned
        Assert.assertEquals(memberDacs.size() + unassociatedConsents.size(), filtered.size());
    }

    @Test
    public void testFilterConsentByDAC_memberCase3() {
        // User is not an admin user
        when(dacUserDAO.findDACUserByEmailAndRoleId(anyString(), anyInt())).thenReturn(null);

        // Member has no direct access to consented datasets
        when(dacDAO.findDacsForEmail(anyString())).thenReturn(Collections.emptyList());
        initService();

        // There unassociated consent manages:
        List<Consent> unassociatedConsents = getConsents().stream().
                peek(c -> c.setDacId(null)).
                collect(Collectors.toList());

        List<Consent> consents = getConsents();
        AuthUser user = new AuthUser("Admin");

        List<Consent> allConsents = Stream.
                concat(unassociatedConsents.stream(), consents.stream()).
                collect(Collectors.toList());

        Collection<Consent> filtered = service.filterConsentsByDAC(allConsents, user);
        // As a member, unassociated consents should be returned
        Assert.assertEquals(unassociatedConsents.size(), filtered.size());
    }


    /* Helper functions */


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
    private List<DACUser> getDacUsers() {
        DACUser chair = new DACUser();
        chair.setDacUserId(1);
        chair.setDisplayName("Chair");
        chair.setEmail("chair@duos.org");
        chair.setRoles(new ArrayList<>());
        chair.getRoles().add(new UserRole(1, chair.getDacUserId(), UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName(), 1));

        DACUser member = new DACUser();
        member.setDacUserId(2);
        member.setDisplayName("Member");
        member.setEmail("member@duos.org");
        member.setRoles(new ArrayList<>());
        member.getRoles().add(new UserRole(1, member.getDacUserId(), UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName(), 1));

        List<DACUser> users = new ArrayList<>();
        users.add(chair);
        users.add(member);
        return users;
    }

}
