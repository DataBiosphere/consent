package org.broadinstitute.consent.http.db;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class AcknowledgementDAOTest extends DAOTestHelper {

  @Test
  void createAndRetrieveAcknowledgement() throws InterruptedException {
    User user = createUser();
    Integer user_id = user.getUserId();
    String key = RandomStringUtils.randomAlphabetic(100);
    assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user_id).isEmpty());

    acknowledgementDAO.upsertAcknowledgement(key, user_id);
    Acknowledgement newAcknowledgement = acknowledgementDAO.findAcknowledgementsByKeyForUser(key,
        user_id);
    assertEquals(newAcknowledgement.getFirstAcknowledged(),
        newAcknowledgement.getLastAcknowledged());
    assertEquals(key, newAcknowledgement.getAckKey());
    assertEquals(user_id, newAcknowledgement.getUserId());

    assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
    assertEquals(newAcknowledgement,
        acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0));

    //Theoretically possible that on a fast enough system not enough
    //time will have passed to tick a millisecond in the clock.  We should be lucky enough to see that.
    sleep(1);
    acknowledgementDAO.upsertAcknowledgement(key, user_id);
    assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
    Acknowledgement upsertResult = acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0);
    assertNotEquals(newAcknowledgement, upsertResult);
    assertEquals(newAcknowledgement.getAckKey(), upsertResult.getAckKey());
    assertEquals(newAcknowledgement.getFirstAcknowledged().getTime(),
        upsertResult.getFirstAcknowledged().getTime());
    assertEquals(newAcknowledgement.getUserId(), upsertResult.getUserId());
    assertNotEquals(newAcknowledgement.getLastAcknowledged().getTime(),
        upsertResult.getLastAcknowledged().getTime());
  }

  @Test
  void createAndDeleteAcknowledgement() {
    User user = createUser();
    Integer user_id = user.getUserId();
    String key1 = RandomStringUtils.randomAlphabetic(100);
    String key2 = RandomStringUtils.randomAlphabetic(100);
    assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user_id).isEmpty());

    acknowledgementDAO.upsertAcknowledgement(key1, user_id);
    acknowledgementDAO.upsertAcknowledgement(key2, user_id);
    Acknowledgement upsertResult = acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0);

    assertEquals(2, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
    assertEquals(upsertResult,
        acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0));

    acknowledgementDAO.deleteAcknowledgement(key1, user_id);
    assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
    Acknowledgement secondAcknowledgement = acknowledgementDAO.findAcknowledgementsForUser(user_id)
        .get(0);
    assertEquals(key2, secondAcknowledgement.getAckKey());
  }

  @Test
  void ensureMissingAcknowledgementWorks() {
    User user2 = createUser();
    User user3 = createUser();
    Integer user1Id = user2.getUserId();
    Integer user2Id = user3.getUserId();
    String key1 = RandomStringUtils.randomAlphabetic(100);
    String key2 = RandomStringUtils.randomAlphabetic(100);
    List<String> key_list = Arrays.asList(key1, key2);
    assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user1Id).isEmpty());
    assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user2Id).isEmpty());

    acknowledgementDAO.upsertAcknowledgement(key1, user1Id);
    assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user1Id).size());
    assertEquals(user1Id,
        acknowledgementDAO.findAcknowledgementsByKeyForUser(key1, user1Id).getUserId());
    acknowledgementDAO.upsertAcknowledgement(key1, user2Id);
    assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user2Id).size());
    assertEquals(user2Id,
        acknowledgementDAO.findAcknowledgementsByKeyForUser(key1, user2Id).getUserId());

    acknowledgementDAO.upsertAcknowledgement(key2, user1Id);
    assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user2Id).size());
    assertEquals(user2Id,
        acknowledgementDAO.findAcknowledgementsByKeyForUser(key1, user2Id).getUserId());

    List<Acknowledgement> user1Acknowledgements = acknowledgementDAO.findAcknowledgementsForUser(
        user1Id);
    assertEquals(2, user1Acknowledgements.size());
    user1Acknowledgements.forEach((ack) -> assertEquals(user1Id, ack.getUserId()));

    List<Acknowledgement> user1AcknowledgementsWithList = acknowledgementDAO.findAcknowledgementsForUser(
        key_list, user1Id);
    assertEquals(2, user1AcknowledgementsWithList.size());

    List<Acknowledgement> user2AcknowledgementsWithList = acknowledgementDAO.findAcknowledgementsForUser(
        key_list, user2Id);
    assertEquals(1, user2AcknowledgementsWithList.size());
  }

  @Test
  void testDeleteAcknowledgmentByUserId() {
    User user = createUser();
    String ack = RandomStringUtils.randomAlphabetic(100);
    acknowledgementDAO.upsertAcknowledgement(ack, user.getUserId());
    assertEquals(1,
        acknowledgementDAO.findAcknowledgementsForUser(user.getUserId()).size());
    acknowledgementDAO.deleteAllAcknowledgementsByUser(user.getUserId());
    assertEquals(0,
        acknowledgementDAO.findAcknowledgementsForUser(user.getUserId()).size());
  }
}
