package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.joda.time.DateTimeField;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ConsentServiceTest {

    private ConsentService service;

    @Mock
    ConsentDAO consentDAO;

    @Mock
    ElectionDAO electionDAO;

    @Mock
    UseRestrictionConverter useRestrictionConverter;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initService() {
        service = new ConsentService(consentDAO, electionDAO, useRestrictionConverter);
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
        doNothing().when(consentDAO).insertConsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
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
                testConsent.getSortDate(), testConsent.getTranslatedUseRestriction(), testConsent.getGroupName(), true);
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        when(consentDAO.findConsentById("test consent"))
                .thenReturn(testConsent);

        initService();

        Consent consent = service.update("test consent", testConsent);
        Assert.assertNotNull(consent);
        Assert.assertEquals(getDayOfYear(consent.getLastUpdate()), getDayOfYear(updateDate));
        Assert.assertEquals(getDayOfYear(consent.getSortDate()), getDayOfYear(updateDate));
    }

    private DateTimeField getDayOfYear(Timestamp timestamp) {
        return new Instant(timestamp.getTime()).getChronology().dayOfYear();
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
    public void testUpdateConsentDul() {
        when(consentDAO.checkConsentById("test consent"))
                .thenReturn("test consent");
        when(consentDAO.findConsentById("test consent"))
                .thenReturn(this.getTestConsent());
        Election mockElection = this.getTestElection();
        when(electionDAO.findLastElectionByReferenceIdAndType(anyString(), anyString())).thenReturn(mockElection);

        LocalDate localDate = LocalDate.now();
        ZoneId defaultZoneId = ZoneId.systemDefault();
        Consent testConsent = this.getTestConsent();
        Timestamp prevTimestamp = new Timestamp(Date.from(localDate.minusDays(1).atStartOfDay(defaultZoneId).toInstant()).getTime());
        testConsent.setLastUpdate(prevTimestamp);
        testConsent.setSortDate(prevTimestamp);

        doNothing().when(consentDAO).updateConsent(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any());

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

    private Election getTestElection() {
        Election mockElection = new Election();
        mockElection.setStatus(ElectionStatus.OPEN.getValue());
        mockElection.setArchived(false);
        return mockElection;
    }

}
