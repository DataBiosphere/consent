package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcknowledgementServiceTest extends AbstractTestHelper {

  @Mock
  private static AcknowledgementDAO acknowledgementDAO;
  @Mock
  private static DataAccessRequestDAO dataAccessRequestDAO;
  @Mock
  private static EmailService emailService;

  private AcknowledgementService acknowledgementService;

  @BeforeEach
  void setUp() {
    acknowledgementService = new AcknowledgementService(acknowledgementDAO, dataAccessRequestDAO,
        emailService);
  }

  @Test
  void test_noAcknowledgementsForUser() {
    User user = new User(1, "test@domain.com", "Test User", new Date(),
        List.of(UserRoles.Researcher()));
    when(acknowledgementDAO.findAcknowledgementsForUser(anyInt())).thenReturn(new ArrayList<>());
    when(acknowledgementDAO.findAcknowledgementsByKeyForUser(anyString(), anyInt())).thenReturn(
        null);

    assertTrue(acknowledgementService.findAcknowledgementsForUser(user).isEmpty());
    assertNull(acknowledgementService.findAcknowledgementForUserByKey(user, "key1"));
  }

  @Test
  void test_makeAndDeleteAcknowledgementForUser() {
    User user = new User(2, "test@domain.com", "Test User", new Date(),
        List.of(UserRoles.Researcher()));
    String key = "key2";
    List<String> keys = List.of(key);
    Timestamp timestamp = new Timestamp(new Date().getTime());
    Acknowledgement key2Acknowledgement = new Acknowledgement();
    key2Acknowledgement.setUserId(user.getUserId());
    key2Acknowledgement.setAckKey(key);
    key2Acknowledgement.setFirstAcknowledged(timestamp);
    key2Acknowledgement.setLastAcknowledged(timestamp);
    List<Acknowledgement> acknowledgementList = List.of(key2Acknowledgement);
    when(acknowledgementDAO.findAcknowledgementsForUser(any(), any())).thenReturn(
        acknowledgementList);
    when(acknowledgementDAO.findAcknowledgementsForUser(anyInt())).thenReturn(acknowledgementList);
    when(acknowledgementDAO.findAcknowledgementsByKeyForUser(anyString(), anyInt())).thenReturn(
        key2Acknowledgement);
    doNothing().when(acknowledgementDAO).deleteAcknowledgement(anyString(), anyInt());

    Map<String, Acknowledgement> makeResponse = acknowledgementService.makeAcknowledgements(keys,
        user);
    assertEquals(1, makeResponse.size());
    assertTrue(makeResponse.containsKey(key));
    assertEquals(key2Acknowledgement, makeResponse.get(key));

    Map<String, Acknowledgement> lookupResponse = acknowledgementService.findAcknowledgementsForUser(
        user);
    assertEquals(1, lookupResponse.size());
    assertTrue(lookupResponse.containsKey(key));
    assertEquals(key2Acknowledgement, lookupResponse.get(key));

    Acknowledgement singleLookupResponse = acknowledgementService.findAcknowledgementForUserByKey(
        user, key);
    assertEquals(singleLookupResponse, key2Acknowledgement);

    acknowledgementService.deleteAcknowledgementForUserByKey(user, key);
  }

  @Test
  void testMakeAcknowledgementWithCloseout() throws Exception {
    User user = new User(3, "test@domain.com", "Test User", new Date(),
        List.of(UserRoles.Chairperson()));
    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setReferenceId(UUID.randomUUID().toString());
    dar1.setDarCode("DAR-" + randomInt(100, 1000));
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setReferenceId(UUID.randomUUID().toString());
    dar2.setDarCode("DAR-" + randomInt(1001, 2000));
    Timestamp timestamp = new Timestamp(new Date().getTime());

    // Expect an email to go out for this
    Acknowledgement ack1 = new Acknowledgement();
    ack1.setUserId(user.getUserId());
    ack1.setAckKey(AcknowledgementService.DAR_CLOSEOUT_CHAIR_REF + dar1.getReferenceId());
    ack1.setFirstAcknowledged(timestamp);

    // Expect NO email for this acknowledgement
    Acknowledgement ack2 = new Acknowledgement();
    ack2.setUserId(user.getUserId());
    ack2.setAckKey(randomAlphabetic(10));
    ack2.setFirstAcknowledged(timestamp);

    // Expect an email to go out for this
    Acknowledgement ack3 = new Acknowledgement();
    ack3.setUserId(user.getUserId());
    ack3.setAckKey(AcknowledgementService.DAR_CLOSEOUT_CHAIR_REF + dar2.getReferenceId());
    ack3.setFirstAcknowledged(timestamp);

    when(dataAccessRequestDAO.findByReferenceId(dar1.getReferenceId())).thenReturn(dar1);
    when(dataAccessRequestDAO.findByReferenceId(dar2.getReferenceId())).thenReturn(dar2);
    when(acknowledgementDAO.findAcknowledgementsForUser(
        List.of(ack1.getAckKey(), ack2.getAckKey(), ack3.getAckKey()),
        user.getUserId())).thenReturn(List.of(ack1, ack2, ack3));

    acknowledgementService.makeAcknowledgements(
        List.of(ack1.getAckKey(), ack2.getAckKey(), ack3.getAckKey()), user);
    // Only two acknowledgements are closeouts, so only two emails should be sent
    verify(emailService).sendResearcherCloseoutCompletedMessage(user, dar1.getDarCode(),
        dar1.getReferenceId());
    verify(emailService).sendResearcherCloseoutCompletedMessage(user, dar2.getDarCode(),
        dar2.getReferenceId());
  }
}
