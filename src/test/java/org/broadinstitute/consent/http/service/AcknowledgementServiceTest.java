package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class AcknowledgementServiceTest {

  @Mock
  private static AcknowledgementDAO acknowledgementDAO;
  private AcknowledgementService acknowledgementService;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initService() {
    acknowledgementService = new AcknowledgementService(acknowledgementDAO);
  }

  @Test
  public void test_noAcknowledgementsForUser() {
    User user = new User(1, "test@domain.com", "Test User", new Date(),
        List.of(UserRoles.Researcher()));
    when(acknowledgementDAO.findAcknowledgementsForUser(anyInt())).thenReturn(new ArrayList<>());
    when(acknowledgementDAO.findAcknowledgementsByKeyForUser(anyString(), anyInt())).thenReturn(
        null);
    initService();
    assertTrue(acknowledgementService.findAcknowledgementsForUser(user).isEmpty());
    assertNull(acknowledgementService.findAcknowledgementForUserByKey(user, "key1"));
  }

  @Test
  public void test_makeAndDeleteAcknowledgementForUser() {
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
    initService();

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
}
