package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;

public class AcknowledgementDAOTest extends DAOTestHelper {
    @Test
    public void createAndRetrieveAcknowledgement() throws InterruptedException {
        Integer user_id = 1;
        String key = RandomStringUtils.randomAlphabetic(100);
        assertTrue(acknowledgementDAO.getAcknowledgementsForUser(user_id).isEmpty());

        acknowledgementDAO.upsertAcknowledgement(key, user_id);
        Acknowledgement newAcknowledgement = acknowledgementDAO.getAcknowledgementsByKeyForUser(key, user_id);
        assertEquals(newAcknowledgement.getFirst_acknowledged(), newAcknowledgement.getLast_acknowledged());
        assertEquals(key, newAcknowledgement.getAck_key());
        assertEquals(user_id, newAcknowledgement.getUserId());

        assertEquals(1, acknowledgementDAO.getAcknowledgementsForUser(user_id).size());
        assertEquals(newAcknowledgement, acknowledgementDAO.getAcknowledgementsForUser(user_id).get(0));

        //Theoretically possible that on a fast enough system not enough
        //time will have passed to tick a millisecond in the clock.  We should be lucky enough to see that.
        sleep(1);
        acknowledgementDAO.upsertAcknowledgement(key, user_id);
        assertEquals(1, acknowledgementDAO.getAcknowledgementsForUser(user_id).size());
        Acknowledgement upsertResult = acknowledgementDAO.getAcknowledgementsForUser(user_id).get(0);
        assertNotEquals(newAcknowledgement, upsertResult);
        assertEquals(newAcknowledgement.getAck_key(), upsertResult.getAck_key());
        assertEquals(newAcknowledgement.getFirst_acknowledged().getTime(), upsertResult.getFirst_acknowledged().getTime());
        assertEquals(newAcknowledgement.getUserId(), upsertResult.getUserId());
        assertNotEquals(newAcknowledgement.getLast_acknowledged().getTime(), upsertResult.getLast_acknowledged().getTime());
    }

    @Test
    public void ensureMissingAcknowledgementWorks(){
        Integer user1 = 2;
        Integer user2 = 3;
        String key1 = RandomStringUtils.randomAlphabetic(100);
        String key2 = RandomStringUtils.randomAlphabetic(100);
        List<String> key_list = Arrays.asList(key1, key2);
        assertTrue(acknowledgementDAO.getAcknowledgementsForUser(user1).isEmpty());
        assertTrue(acknowledgementDAO.getAcknowledgementsForUser(user2).isEmpty());

        acknowledgementDAO.upsertAcknowledgement(key1, user1);
        assertEquals(1, acknowledgementDAO.getAcknowledgementsForUser(user1).size());
        assertEquals(user1, acknowledgementDAO.getAcknowledgementsByKeyForUser(key1, user1).getUserId());
        acknowledgementDAO.upsertAcknowledgement(key1, user2);
        assertEquals(1, acknowledgementDAO.getAcknowledgementsForUser(user2).size());
        assertEquals(user2, acknowledgementDAO.getAcknowledgementsByKeyForUser(key1, user2).getUserId());

        acknowledgementDAO.upsertAcknowledgement(key2, user1);
        assertEquals(1, acknowledgementDAO.getAcknowledgementsForUser(user2).size());
        assertEquals(user2, acknowledgementDAO.getAcknowledgementsByKeyForUser(key1, user2).getUserId());

        List<Acknowledgement> user1Acknowledgements = acknowledgementDAO.getAcknowledgementsForUser(user1);
        assertEquals(2, user1Acknowledgements.size());
        user1Acknowledgements.forEach((ack) -> assertEquals(user1, ack.getUserId()));

        List<Acknowledgement> user1AcknowledgementsWithList = acknowledgementDAO.getAcknowledgementsForUser(key_list, user1);
        assertEquals(2, user1AcknowledgementsWithList.size());

        List<Acknowledgement> user2AcknowledgementsWithList = acknowledgementDAO.getAcknowledgementsForUser(key_list, user2);
        assertEquals(1, user2AcknowledgementsWithList.size());
    }
}
