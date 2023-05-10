package org.broadinstitute.consent.http.db;

import static java.lang.Thread.sleep;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AcknowledgementDAOTest extends DAOTestHelper {
    @Test
    public void createAndRetrieveAcknowledgement() throws InterruptedException {
        User user = createUser();
        Integer user_id = user.getUserId();
        String key = RandomStringUtils.randomAlphabetic(100);
        Assertions.assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user_id).isEmpty());

        acknowledgementDAO.upsertAcknowledgement(key, user_id);
        Acknowledgement newAcknowledgement = acknowledgementDAO.findAcknowledgementsByKeyForUser(key, user_id);
        Assertions.assertEquals(newAcknowledgement.getFirstAcknowledged(),
            newAcknowledgement.getLastAcknowledged());
        Assertions.assertEquals(key, newAcknowledgement.getAckKey());
        Assertions.assertEquals(user_id, newAcknowledgement.getUserId());

        Assertions.assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
        Assertions.assertEquals(newAcknowledgement,
            acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0));

        //Theoretically possible that on a fast enough system not enough
        //time will have passed to tick a millisecond in the clock.  We should be lucky enough to see that.
        sleep(1);
        acknowledgementDAO.upsertAcknowledgement(key, user_id);
        Assertions.assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
        Acknowledgement upsertResult = acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0);
        Assertions.assertNotEquals(newAcknowledgement, upsertResult);
        Assertions.assertEquals(newAcknowledgement.getAckKey(), upsertResult.getAckKey());
        Assertions.assertEquals(newAcknowledgement.getFirstAcknowledged().getTime(),
            upsertResult.getFirstAcknowledged().getTime());
        Assertions.assertEquals(newAcknowledgement.getUserId(), upsertResult.getUserId());
        Assertions.assertNotEquals(newAcknowledgement.getLastAcknowledged().getTime(),
            upsertResult.getLastAcknowledged().getTime());
    }

    @Test
    public void createAndDeleteAcknowledgement() {
        User user = createUser();
        Integer user_id = user.getUserId();
        String key1 = RandomStringUtils.randomAlphabetic(100);
        String key2 = RandomStringUtils.randomAlphabetic(100);
        Assertions.assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user_id).isEmpty());

        acknowledgementDAO.upsertAcknowledgement(key1, user_id);
        acknowledgementDAO.upsertAcknowledgement(key2, user_id);
        Acknowledgement upsertResult = acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0);

        Assertions.assertEquals(2, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
        Assertions.assertEquals(upsertResult,
            acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0));

        acknowledgementDAO.deleteAcknowledgement(key1, user_id);
        Assertions.assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user_id).size());
        Acknowledgement secondAcknowledgement = acknowledgementDAO.findAcknowledgementsForUser(user_id).get(0);
        Assertions.assertEquals(key2, secondAcknowledgement.getAckKey());
    }

    @Test
    public void ensureMissingAcknowledgementWorks() {
        User user2 = createUser();
        User user3 = createUser();
        Integer user1Id = user2.getUserId();
        Integer user2Id = user3.getUserId();
        String key1 = RandomStringUtils.randomAlphabetic(100);
        String key2 = RandomStringUtils.randomAlphabetic(100);
        List<String> key_list = Arrays.asList(key1, key2);
        Assertions.assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user1Id).isEmpty());
        Assertions.assertTrue(acknowledgementDAO.findAcknowledgementsForUser(user2Id).isEmpty());

        acknowledgementDAO.upsertAcknowledgement(key1, user1Id);
        Assertions.assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user1Id).size());
        Assertions.assertEquals(user1Id,
            acknowledgementDAO.findAcknowledgementsByKeyForUser(key1, user1Id).getUserId());
        acknowledgementDAO.upsertAcknowledgement(key1, user2Id);
        Assertions.assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user2Id).size());
        Assertions.assertEquals(user2Id,
            acknowledgementDAO.findAcknowledgementsByKeyForUser(key1, user2Id).getUserId());

        acknowledgementDAO.upsertAcknowledgement(key2, user1Id);
        Assertions.assertEquals(1, acknowledgementDAO.findAcknowledgementsForUser(user2Id).size());
        Assertions.assertEquals(user2Id,
            acknowledgementDAO.findAcknowledgementsByKeyForUser(key1, user2Id).getUserId());

        List<Acknowledgement> user1Acknowledgements = acknowledgementDAO.findAcknowledgementsForUser(user1Id);
        Assertions.assertEquals(2, user1Acknowledgements.size());
        user1Acknowledgements.forEach((ack) -> Assertions.assertEquals(user1Id, ack.getUserId()));

        List<Acknowledgement> user1AcknowledgementsWithList = acknowledgementDAO.findAcknowledgementsForUser(key_list, user1Id);
        Assertions.assertEquals(2, user1AcknowledgementsWithList.size());

        List<Acknowledgement> user2AcknowledgementsWithList = acknowledgementDAO.findAcknowledgementsForUser(key_list, user2Id);
        Assertions.assertEquals(1, user2AcknowledgementsWithList.size());
    }

    @Test
    public void testDeleteAcknowledgmentByUserId() {
        User user = createUser();
        String ack = RandomStringUtils.randomAlphabetic(100);
        acknowledgementDAO.upsertAcknowledgement(ack, user.getUserId());
        Assertions.assertEquals(1,
            acknowledgementDAO.findAcknowledgementsForUser(user.getUserId()).size());
        acknowledgementDAO.deleteAllAcknowledgementsByUser(user.getUserId());
        Assertions.assertEquals(0,
            acknowledgementDAO.findAcknowledgementsForUser(user.getUserId()).size());
    }
}
