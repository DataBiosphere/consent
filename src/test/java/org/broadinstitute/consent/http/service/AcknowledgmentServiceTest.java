package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.AcknowledgmentDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgment;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class AcknowledgmentServiceTest {

    @Mock
    private static AcknowledgmentDAO acknowledgmentDAO;
    private AcknowledgmentService acknowledgmentService;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initService(){
        acknowledgmentService = new AcknowledgmentService(acknowledgmentDAO);
    }

    @Test
    public void test_noAcknowledgmentsForUser(){
        User user = new User(1, "test@domain.com", "Test User", new Date(),
                List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
        when(acknowledgmentDAO.findAcknowledgmentsForUser(anyInt())).thenReturn(new ArrayList<>());
        when(acknowledgmentDAO.findAcknowledgmentsByKeyForUser(anyString(), anyInt())).thenReturn(null);
        initService();
        assertTrue(acknowledgmentService.findAcknowledgmentsForUser(user).isEmpty());
        assertNull(acknowledgmentService.findAcknowledgmentForUserByKey(user, "key1"));
    }

    @Test
    public void test_makeAndDeleteAcknowledgmentForUser(){
        User user = new User(2, "test@domain.com", "Test User", new Date(),
                List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
        String key = "key2";
        List<String> keys = List.of(key);
        Timestamp timestamp = new Timestamp(new Date().getTime());
        Acknowledgment key2Acknowledgment = new Acknowledgment();
        key2Acknowledgment.setUserId(user.getUserId());
        key2Acknowledgment.setAckKey(key);
        key2Acknowledgment.setFirstAcknowledged(timestamp);
        key2Acknowledgment.setLastAcknowledged(timestamp);
        List<Acknowledgment> acknowledgmentList = List.of(key2Acknowledgment);
        when(acknowledgmentDAO.findAcknowledgmentsForUser(any(), any())).thenReturn(acknowledgmentList);
        when(acknowledgmentDAO.findAcknowledgmentsForUser(anyInt())).thenReturn(acknowledgmentList);
        when(acknowledgmentDAO.findAcknowledgmentsByKeyForUser(anyString(), anyInt())).thenReturn(key2Acknowledgment);
        doNothing().when(acknowledgmentDAO).deleteAcknowledgment(anyString(), anyInt());
        initService();

        Map<String, Acknowledgment> makeResponse = acknowledgmentService.makeAcknowledgments(keys,user);
        assertEquals(1, makeResponse.size());
        assertTrue(makeResponse.containsKey(key));
        assertEquals(key2Acknowledgment, makeResponse.get(key));

        Map<String, Acknowledgment> lookupResponse = acknowledgmentService.findAcknowledgmentsForUser(user);
        assertEquals(1, lookupResponse.size());
        assertTrue(lookupResponse.containsKey(key));
        assertEquals(key2Acknowledgment, lookupResponse.get(key));

        Acknowledgment singleLookupResponse = acknowledgmentService.findAcknowledgmentForUserByKey(user,key);
        assertEquals(singleLookupResponse, key2Acknowledgment);

        acknowledgmentService.deleteAcknowledgmentForUserByKey(user, key);
    }
}
