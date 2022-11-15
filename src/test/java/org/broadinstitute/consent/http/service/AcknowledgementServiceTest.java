package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.AcknowledgementDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class AcknowledgementServiceTest {

    @Mock
    private static AcknowledgementDAO acknowledgementDAO;
    private AcknowledgementService acknowledgementService;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initService(){
        acknowledgementService = new AcknowledgementService(acknowledgementDAO);
    }

    @Test
    public void test_noAcknowledgementsForUser(){
        User user = new User(1, "test@domain.com", "Test User", new Date(),
                List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
        when(acknowledgementDAO.getAcknowledgementsForUser(anyInt())).thenReturn(new ArrayList<>());
        when(acknowledgementDAO.getAcknowledgementsByKeyForUser(anyString(), anyInt())).thenReturn(null);
        initService();
        assertTrue(acknowledgementService.getAcknowledgementsForUser(user).isEmpty());
        assertNull(acknowledgementService.getAcknowledgementForUserByKey(user, "key1"));
    }

    @Test
    public void test_makeAcknowledgementForUser(){
        User user = new User(2, "test@domain.com", "Test User", new Date(),
                List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
        String key = "key2";
        List<String> keys = List.of(key);
        Timestamp timestamp = new Timestamp(new Date().getTime());
        Acknowledgement key2Acknowledgement = new Acknowledgement();
        key2Acknowledgement.setUserId(user.getUserId());
        key2Acknowledgement.setAck_key(key);
        key2Acknowledgement.setFirst_acknowledged(timestamp);
        key2Acknowledgement.setLast_acknowledged(timestamp);
        List<Acknowledgement> acknowledgementList = List.of(key2Acknowledgement);
        when(acknowledgementDAO.getAcknowledgementsForUser(any(), any())).thenReturn(acknowledgementList);
        when(acknowledgementDAO.getAcknowledgementsForUser(anyInt())).thenReturn(acknowledgementList);
        when(acknowledgementDAO.getAcknowledgementsByKeyForUser(anyString(), anyInt())).thenReturn(key2Acknowledgement);
        initService();

        Map<String, Acknowledgement> makeResponse = acknowledgementService.makeAcknowledgements(keys,user);
        assertEquals(1, makeResponse.size());
        assertTrue(makeResponse.containsKey(key));
        assertEquals(key2Acknowledgement, makeResponse.get(key));

        Map<String, Acknowledgement> lookupResponse = acknowledgementService.getAcknowledgementsForUser(user);
        assertEquals(1, lookupResponse.size());
        assertTrue(lookupResponse.containsKey(key));
        assertEquals(key2Acknowledgement, lookupResponse.get(key));

        Acknowledgement singleLookupResponse = acknowledgementService.getAcknowledgementForUserByKey(user,key);
        assertEquals(singleLookupResponse, key2Acknowledgement);
    }
}
