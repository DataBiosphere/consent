package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.Acknowledgment;
import org.broadinstitute.consent.http.models.User;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AcknowledgmentDAOTest extends DAOTestHelper {
    @Test
    public void createAndRetrieveAcknowledgment() throws InterruptedException {
        User user = createUser();
        Integer user_id = user.getUserId();
        String key = RandomStringUtils.randomAlphabetic(100);
        assertTrue(acknowledgmentDAO.findAcknowledgmentsForUser(user_id).isEmpty());

        acknowledgmentDAO.upsertAcknowledgment(key, user_id);
        Acknowledgment newAcknowledgment = acknowledgmentDAO.findAcknowledgmentsByKeyForUser(key, user_id);
        assertEquals(newAcknowledgment.getFirstAcknowledged(), newAcknowledgment.getLastAcknowledged());
        assertEquals(key, newAcknowledgment.getAckKey());
        assertEquals(user_id, newAcknowledgment.getUserId());

        assertEquals(1, acknowledgmentDAO.findAcknowledgmentsForUser(user_id).size());
        assertEquals(newAcknowledgment, acknowledgmentDAO.findAcknowledgmentsForUser(user_id).get(0));

        //Theoretically possible that on a fast enough system not enough
        //time will have passed to tick a millisecond in the clock.  We should be lucky enough to see that.
        sleep(1);
        acknowledgmentDAO.upsertAcknowledgment(key, user_id);
        assertEquals(1, acknowledgmentDAO.findAcknowledgmentsForUser(user_id).size());
        Acknowledgment upsertResult = acknowledgmentDAO.findAcknowledgmentsForUser(user_id).get(0);
        assertNotEquals(newAcknowledgment, upsertResult);
        assertEquals(newAcknowledgment.getAckKey(), upsertResult.getAckKey());
        assertEquals(newAcknowledgment.getFirstAcknowledged().getTime(), upsertResult.getFirstAcknowledged().getTime());
        assertEquals(newAcknowledgment.getUserId(), upsertResult.getUserId());
        assertNotEquals(newAcknowledgment.getLastAcknowledged().getTime(), upsertResult.getLastAcknowledged().getTime());
    }

    @Test
    public void createAndDeleteAcknowledgment() {
        User user = createUser();
        Integer user_id = user.getUserId();
        String key1 = RandomStringUtils.randomAlphabetic(100);
        String key2 = RandomStringUtils.randomAlphabetic(100);
        assertTrue(acknowledgmentDAO.findAcknowledgmentsForUser(user_id).isEmpty());

        acknowledgmentDAO.upsertAcknowledgment(key1, user_id);
        acknowledgmentDAO.upsertAcknowledgment(key2, user_id);
        Acknowledgment upsertResult = acknowledgmentDAO.findAcknowledgmentsForUser(user_id).get(0);

        assertEquals(2, acknowledgmentDAO.findAcknowledgmentsForUser(user_id).size());
        assertEquals(upsertResult, acknowledgmentDAO.findAcknowledgmentsForUser(user_id).get(0));

        acknowledgmentDAO.deleteAcknowledgment(key1, user_id);
        assertEquals(1, acknowledgmentDAO.findAcknowledgmentsForUser(user_id).size());
        Acknowledgment secondAcknowledgment = acknowledgmentDAO.findAcknowledgmentsForUser(user_id).get(0);
        assertEquals(key2, secondAcknowledgment.getAckKey());
    }

    @Test
    public void ensureMissingAcknowledgmentWorks(){
        User user2 = createUser();
        User user3 = createUser();
        Integer user1Id = user2.getUserId();
        Integer user2Id = user3.getUserId();
        String key1 = RandomStringUtils.randomAlphabetic(100);
        String key2 = RandomStringUtils.randomAlphabetic(100);
        List<String> key_list = Arrays.asList(key1, key2);
        assertTrue(acknowledgmentDAO.findAcknowledgmentsForUser(user1Id).isEmpty());
        assertTrue(acknowledgmentDAO.findAcknowledgmentsForUser(user2Id).isEmpty());

        acknowledgmentDAO.upsertAcknowledgment(key1, user1Id);
        assertEquals(1, acknowledgmentDAO.findAcknowledgmentsForUser(user1Id).size());
        assertEquals(user1Id, acknowledgmentDAO.findAcknowledgmentsByKeyForUser(key1, user1Id).getUserId());
        acknowledgmentDAO.upsertAcknowledgment(key1, user2Id);
        assertEquals(1, acknowledgmentDAO.findAcknowledgmentsForUser(user2Id).size());
        assertEquals(user2Id, acknowledgmentDAO.findAcknowledgmentsByKeyForUser(key1, user2Id).getUserId());

        acknowledgmentDAO.upsertAcknowledgment(key2, user1Id);
        assertEquals(1, acknowledgmentDAO.findAcknowledgmentsForUser(user2Id).size());
        assertEquals(user2Id, acknowledgmentDAO.findAcknowledgmentsByKeyForUser(key1, user2Id).getUserId());

        List<Acknowledgment> user1Acknowledgments = acknowledgmentDAO.findAcknowledgmentsForUser(user1Id);
        assertEquals(2, user1Acknowledgments.size());
        user1Acknowledgments.forEach((ack) -> assertEquals(user1Id, ack.getUserId()));

        List<Acknowledgment> user1AcknowledgmentsWithList = acknowledgmentDAO.findAcknowledgmentsForUser(key_list, user1Id);
        assertEquals(2, user1AcknowledgmentsWithList.size());

        List<Acknowledgment> user2AcknowledgmentsWithList = acknowledgmentDAO.findAcknowledgmentsForUser(key_list, user2Id);
        assertEquals(1, user2AcknowledgmentsWithList.size());
    }
}
