package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Election;
import org.joda.time.DateTimeField;
import org.joda.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

  private ConsentService service;

  @Mock
  private ConsentDAO consentDAO;

  @Mock
  private ElectionDAO electionDAO;

  private void initService() {
    service = new ConsentService(consentDAO, electionDAO);
  }

  @Test
  void testCreate() {
    Consent testConsent = this.getTestConsent();
    when(consentDAO.getIdByName(any()))
        .thenReturn(null);
    doNothing().when(consentDAO)
        .insertConsent(any(), any(), any(), any(), any(), any(), any(), any(), any());
    when(consentDAO.findConsentById(any()))
        .thenReturn(testConsent);
    initService();

    Consent consent = service.create(testConsent);
    assertNotNull(consent);
    assertEquals(consent.getName(), testConsent.getName());
  }

  @Test
  void testUpdate() {
    Timestamp updateDate = new Timestamp(new Date().getTime());
    LocalDate localDate = LocalDate.now();
    ZoneId defaultZoneId = ZoneId.systemDefault();
    Consent testConsent = this.getTestConsent();
    Timestamp prevTimestamp = new Timestamp(
        Date.from(localDate.minusDays(1).atStartOfDay(defaultZoneId).toInstant()).getTime());
    testConsent.setLastUpdate(prevTimestamp);
    testConsent.setSortDate(prevTimestamp);

    doNothing().when(consentDAO).updateConsent(any(), any(), any(), any(), any(), any(), any(),
      any(), any(), any());
    when(consentDAO.checkConsentById("test consent"))
        .thenReturn("test consent");
    when(consentDAO.findConsentById("test consent"))
        .thenReturn(testConsent);

    initService();

    Consent consent = service.update("test consent", testConsent);
    assertNotNull(consent);
    assertEquals(getDayOfYear(consent.getLastUpdate()), getDayOfYear(updateDate));
    assertEquals(getDayOfYear(consent.getSortDate()), getDayOfYear(updateDate));
  }

  private DateTimeField getDayOfYear(Timestamp timestamp) {
    return new Instant(timestamp.getTime()).getChronology().dayOfYear();
  }

  @Test
  void testRetrieve() {
    when(consentDAO.findConsentById("test consent"))
        .thenReturn(this.getTestConsent());
    Election mockElection = this.getTestElection();
    when(electionDAO.findLastElectionByReferenceIdAndType(anyString(), anyString())).thenReturn(
        mockElection);

    initService();
    Consent consent = null;
    try {
      consent = service.retrieve("test consent");
    } catch (UnknownIdentifierException unknownIdentifierException) {
      fail(unknownIdentifierException.getMessage());
    }
    assertNotNull(consent);
    assertEquals(consent.getConsentId(), this.getTestConsent().getConsentId());
    assertEquals(consent.getLastElectionArchived(), mockElection.getArchived());
    assertEquals(consent.getLastElectionStatus(), mockElection.getStatus());
  }

  @Test
  void testGetByName() {
    when(consentDAO.findConsentByName("test consent"))
        .thenReturn(this.getTestConsent());
    initService();

    Consent consent = null;
    try {
      consent = service.getByName("test consent");
    } catch (UnknownIdentifierException unknownIdentifierException) {
      fail(unknownIdentifierException.getMessage());
    }

    assertNotNull(consent);
  }

  private Consent getTestConsent() {
    Consent consent = new Consent(false, "data use",
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
