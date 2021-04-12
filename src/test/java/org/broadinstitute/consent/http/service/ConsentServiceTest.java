package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.AssociationDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.jdbi.v3.core.Jdbi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class ConsentServiceTest {

    private ConsentService service;

    @Mock
    ConsentDAO consentDAO;

    @Mock
    ElectionDAO electionDAO;

    @Mock
    VoteDAO voteDAO;

    @Mock
    DacService dacService;

    @Mock
    DataAccessRequestDAO dataAccessRequestDAO;

    @Mock
    AuditService auditService;

    @Mock
    DatasetDAO dataSetDAO;

    @Mock
    AssociationDAO associationDAO;

    @Mock
    DacDAO dacDAO;

    @Mock
    Jdbi jdbi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new ConsentService(consentDAO, electionDAO, voteDAO, dacService, dataAccessRequestDAO, auditService, associationDAO, jdbi, dataSetDAO);
    }

    @Test
    public void testGetById() throws Exception {
        when(consentDAO.findConsentById(anyString())).thenReturn(new Consent());
        Election mockElection = this.getTestElection();
        when(electionDAO.findLastElectionByReferenceIdAndType(anyString(), anyString())).thenReturn(mockElection);
        initService();

        Consent consent = service.getById(UUID.randomUUID().toString());
        Assert.assertNotNull(consent);
        Assert.assertEquals(ElectionStatus.OPEN.getValue(), consent.getLastElectionStatus());
        Assert.assertFalse(consent.getLastElectionArchived());
    }

    @Test
    public void testUpdateConsentDac() {
        doNothing().when(consentDAO).updateConsentDac(anyString(), anyInt());
        initService();

        try {
            service.updateConsentDac(UUID.randomUUID().toString(), RandomUtils.nextInt(1, 10));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDescribeConsentManage() {
        AuthUser user = new AuthUser("user@test.com");
        when(consentDAO.findUnreviewedConsents()).thenReturn(Collections.emptyList());
        when(consentDAO.findConsentManageByStatus(anyString())).thenReturn(Collections.emptyList());
        when(voteDAO.findChairPersonVoteByElectionId(anyInt())).thenReturn(true);
        when(electionDAO.findElectionsWithFinalVoteByTypeAndStatus(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(Collections.emptyList());
        when(dacService.filterConsentManageByDAC(anyList(), any(AuthUser.class))).thenReturn(Collections.emptyList());
        initService();

        try {
            service.describeConsentManage(user);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCreate() {
        Consent testConsent = this.getTestConsent();
        when(consentDAO.checkConsentById(any()))
                .thenReturn(null);
        when(consentDAO.getIdByName(any()))
                .thenReturn(null);
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        doNothing().when(consentDAO).insertConsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        when(consentDAO.findConsentById(any()))
                .thenReturn(testConsent);
        initService();

        Consent consent = service.create(testConsent);
        Assert.assertNotNull(consent);
        Assert.assertEquals(consent.getName(), testConsent.getName());
    }

    @Test
    public void testUpdate() {
        Timestamp updateDate = new Timestamp(new Date().getTime());
        LocalDate localDate = LocalDate.now();
        ZoneId defaultZoneId = ZoneId.systemDefault();
        Consent testConsent = this.getTestConsent();
        Timestamp prevTimestamp = new Timestamp(Date.from(localDate.minusDays(1).atStartOfDay(defaultZoneId).toInstant()).getTime());
        testConsent.setLastUpdate(prevTimestamp);
        testConsent.setSortDate(prevTimestamp);

        doNothing().when(consentDAO).updateConsent("test consent", testConsent.getRequiresManualReview(),
                testConsent.getUseRestriction().toString(), testConsent.getDataUse().toString(),
                testConsent.getDataUseLetter(), testConsent.getName(), testConsent.getDulName(), testConsent.getLastUpdate(),
                testConsent.getSortDate(), testConsent.getTranslatedUseRestriction(), testConsent.getGroupName(), true,
                testConsent.getDacId());
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        when(consentDAO.findConsentById("test consent"))
                .thenReturn(testConsent);

        initService();

        Consent consent = service.update("test consent", testConsent);
        Assert.assertNotNull(consent);
        Assert.assertEquals(consent.getLastUpdate().getDay(), updateDate.getDay());
        Assert.assertEquals(consent.getSortDate().getDay(), updateDate.getDay());
    }

    @Test
    public void testGetAssociation_NoAssociationType() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        when(consentDAO.findAssociationTypesForConsent("test consent"))
                .thenReturn(Arrays.asList("sample"));
        when(consentDAO.findAssociationsByType("test consent", "sample"))
                .thenReturn(Arrays.asList("test object"));
        ConsentAssociation association = buildConsentAssociation("sample", "test object");

        initService();
        List<ConsentAssociation> associationList = service.getAssociation("test consent", null, null);
        Assert.assertNotNull(associationList);
        Assert.assertEquals(associationList.size(), 1);
        Assert.assertEquals(associationList.get(0).getElements().get(0), association.getElements().get(0));
    }

    @Test
    public void testGetAssociation_WithAssociationTypeNoObjecId() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        when(consentDAO.findAssociationTypesForConsent("test consent"))
                .thenReturn(Arrays.asList("sample"));
        when(consentDAO.findAssociationsByType("test consent", "sample"))
                .thenReturn(Arrays.asList("test object"));
        ConsentAssociation association = buildConsentAssociation("sample", "test object");

        initService();
        List<ConsentAssociation> associationList = service.getAssociation("test consent", "sample", null);
        Assert.assertNotNull(associationList);
        Assert.assertEquals(associationList.size(), 1);
        Assert.assertEquals(associationList.get(0).getElements().get(0), association.getElements().get(0));
    }

    @Test
    public void testGetAssociation_WithAssociationTypeAndObjecId() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        when(consentDAO.findAssociationTypesForConsent("test consent"))
                .thenReturn(Arrays.asList("sample"));
        when(consentDAO.findAssociationByTypeAndId("test consent", "sample", "test object"))
                .thenReturn("test object");
        ConsentAssociation association = buildConsentAssociation("sample", "test object");

        initService();
        List<ConsentAssociation> associationList = service.getAssociation("test consent", "sample", "test object");
        Assert.assertNotNull(associationList);
        Assert.assertEquals(associationList.size(), 1);
        Assert.assertEquals(associationList.get(0).getElements().get(0), association.getElements().get(0));
    }

    @Test
    public void testRetrieve() {
        when(consentDAO.findConsentById("test consent"))
                .thenReturn(this.getTestConsent());
        Election mockElection = this.getTestElection();
        when(electionDAO.findLastElectionByReferenceIdAndType(anyString(), anyString())).thenReturn(mockElection);

        initService();
        Consent consent = null;
        try {
            consent = service.retrieve("test consent");
        } catch (UnknownIdentifierException unknownIdentifierException) {
            Assert.fail(unknownIdentifierException.getMessage());
        }
        Assert.assertNotNull(consent);
        Assert.assertEquals(consent.getConsentId(), this.getTestConsent().getConsentId());
        Assert.assertEquals(consent.getLastElectionArchived(), mockElection.getArchived());
        Assert.assertEquals(consent.getLastElectionStatus(), mockElection.getStatus());
    }

    @Test
    public void testGetConsentFromDatasetID() {
        when(consentDAO.findConsentFromDatasetID(any()))
                .thenReturn(this.getTestConsent());

        initService();

        Consent consent = service.getConsentFromDatasetID(1);
        Assert.assertNotNull(consent);
    }

    @Test
    public void testGetUnReviewedConsents() {
        when(consentDAO.findUnreviewedConsents())
                .thenReturn(Arrays.asList(this.getTestConsent()));
        when(dacDAO.findDacsForEmail(any()))
                .thenReturn(Arrays.asList(this.getTestDac()));

        initService();

        Integer unreviewed = service.getUnReviewedConsents(this.getUser());
        Assert.assertEquals(Integer.valueOf(0), unreviewed);
    }

    @Test
    public void testDeleteAssociation_NoAssociationType() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        doNothing().when(consentDAO).deleteAllAssociationsForConsent(any());
        when(consentDAO.findAssociationTypesForConsent("test consent"))
                .thenReturn(Arrays.asList("sample"));
        when(consentDAO.findAssociationsByType("test consent", "sam[le"))
                .thenReturn(Arrays.asList("test object"));

        initService();

        List<ConsentAssociation> consents = service.deleteAssociation("test consent", null, null);
        Assert.assertNotNull(consents);
    }

    @Test
    public void testDeleteAssociation_WithAssociationType() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        doNothing().when(consentDAO).deleteAllAssociationsForType(any(), any());
        when(consentDAO.findAssociationTypesForConsent("test consent"))
                .thenReturn(Arrays.asList("sample"));
        when(consentDAO.findAssociationsByType("test consent", "sam[le"))
                .thenReturn(Arrays.asList("test object"));

        initService();

        List<ConsentAssociation> consents = service.deleteAssociation("test consent", "sample", null);
        Assert.assertNotNull(consents);
    }

    @Test
    public void testDeleteAssociation_WithAssociationTypeAndObjectId() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        doNothing().when(consentDAO).deleteOneAssociation(any(), any(), any());
        when(consentDAO.findAssociationTypesForConsent("test consent"))
                .thenReturn(Arrays.asList("sample"));
        when(consentDAO.findAssociationsByType("test consent", "sam[le"))
                .thenReturn(Arrays.asList("test object"));
        when(consentDAO.findAssociationByTypeAndId(any(), any(), any()))
                .thenReturn("sample");

        initService();

        List<ConsentAssociation> consents = service.deleteAssociation("test consent", "sample", "test object");
        Assert.assertNotNull(consents);
    }

    @Test
    public void testUpdateConsentDul() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        when(consentDAO.findConsentById("test consent"))
                .thenReturn(this.getTestConsent());
        Election mockElection = this.getTestElection();
        when(electionDAO.findLastElectionByReferenceIdAndType(anyString(), anyString())).thenReturn(mockElection);

        Timestamp updateDate = new Timestamp(new Date().getTime());
        LocalDate localDate = LocalDate.now();
        ZoneId defaultZoneId = ZoneId.systemDefault();
        Consent testConsent = this.getTestConsent();
        Timestamp prevTimestamp = new Timestamp(Date.from(localDate.minusDays(1).atStartOfDay(defaultZoneId).toInstant()).getTime());
        testConsent.setLastUpdate(prevTimestamp);
        testConsent.setSortDate(prevTimestamp);

        doNothing().when(consentDAO).updateConsent(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any());

        initService();

        Consent consent = null;
        try {
            consent = service.updateConsentDul("test consent", "data use letter", "dul name");
        } catch (UnknownIdentifierException unknownIdentifierException){
            Assert.fail(unknownIdentifierException.getMessage());
        }

        Assert.assertNotNull(consent);
    }

    @Test
    public void testGetConsentDulUrl() {
        when(consentDAO.findConsentById("test consent"))
                .thenReturn(this.getTestConsent());
        Election mockElection = this.getTestElection();
        when(electionDAO.findLastElectionByReferenceIdAndType(anyString(), anyString())).thenReturn(mockElection);

        initService();

        String dulUrl = null;
        try {
            dulUrl = service.getConsentDulUrl("test consent");
        } catch (UnknownIdentifierException unknownIdentifierException){
            Assert.fail(unknownIdentifierException.getMessage());
        }
        Assert.assertNotNull(dulUrl);
    }

    @Test
    public void testHasWorkspaceAssociation() {
        when(associationDAO.findAssociationIdByTypeAndObjectId(AssociationType.WORKSPACE.getValue(), "test object"))
                .thenReturn(1);
        initService();

        Boolean hasWorkspaceAssociation = service.hasWorkspaceAssociation("test object");

        Assert.assertEquals(true, hasWorkspaceAssociation);
    }

    @Test
    public void testGetByName() {
        when(consentDAO.findConsentByName("test consent"))
                .thenReturn(this.getTestConsent());
        initService();

        Consent consent = null;
        try {
            consent = service.getByName("test consent");
        } catch (UnknownIdentifierException unknownIdentifierException) {
            Assert.fail(unknownIdentifierException.getMessage());
        }

        Assert.assertNotNull(consent);
    }

    private Consent getTestConsent() {
        Consent consent = new Consent(false, new Everything(), "data use",
                "test consent", new Timestamp(new Date().getTime()), new Timestamp(new Date().getTime()),
                new Timestamp(new Date().getTime()), "test consent group");
        consent.setDataUse(new DataUse());

        return consent;
    }

    private DataSet getTestDataset() {
        DataSet dataset = new DataSet();
        dataset.setDataSetId(1);
        dataset.setObjectId("test dataset");
        return dataset;
    }

    private ConsentAssociation buildConsentAssociation(String atype, String... elements) {
        final ArrayList<String> elem_list = new ArrayList<>();
        Collections.addAll(elem_list, elements);
        return new ConsentAssociation(atype, elem_list);
    }

    private Election getTestElection() {
        Election mockElection = new Election();
        mockElection.setStatus(ElectionStatus.OPEN.getValue());
        mockElection.setArchived(false);
        return mockElection;
    }

    private AuthUser getUser() {
        return new AuthUser("User");
    }

    private Dac getTestDac() {
        Dac dac = new Dac();
        dac.setName("test dac");
        dac.setDacId(1);
        return dac;
    }
}
