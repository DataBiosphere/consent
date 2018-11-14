package org.broadinstitute.consent.http.service;

import java.util.Date;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.ApprovalExpirationTimeDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.util.DarConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class DatabaseApprovalExpirationTimeAPITest {

    @Mock
    private ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
    @Mock
    private DACUserDAO dacUserDAO;

    DatabaseApprovalExpirationTimeAPI databaseApprovalAPI;

    ApprovalExpirationTime validApproval = new ApprovalExpirationTime(1, 123, new Date(), new Date(), 5, "Testing approval period");
    DACUser validUser = new DACUser();

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        databaseApprovalAPI = new DatabaseApprovalExpirationTimeAPI(approvalExpirationTimeDAO, dacUserDAO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateException() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(validApproval);
        databaseApprovalAPI.create(validApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsMissingAmountOfDays() throws Exception {
        ApprovalExpirationTime missingDaysApproval = new ApprovalExpirationTime(1, 123, new Date(), new Date(), null, "Testing approval period");
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        databaseApprovalAPI.create(missingDaysApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsMissingUserId() throws Exception {
        ApprovalExpirationTime missingUserIdApproval = new ApprovalExpirationTime(1, null, new Date(), new Date(), 5, "Testing approval period");
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        databaseApprovalAPI.create(missingUserIdApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsNoUserForId() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        when(dacUserDAO.findDACUserById(validApproval.getUserId())).thenReturn(null);
        databaseApprovalAPI.create(validApproval);
    }

    @Test
    public void testCreate() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(anyInt())).thenReturn(validApproval);
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        when(dacUserDAO.findDACUserById(anyInt())).thenReturn(validUser);
        databaseApprovalAPI.create(validApproval);
    }

    @Test
    public void testUpdate() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(anyInt())).thenReturn(validApproval);
        when(dacUserDAO.findDACUserById(validApproval.getUserId())).thenReturn(validUser);
        ApprovalExpirationTime response = databaseApprovalAPI.update(validApproval, 1);
        assertTrue("The approval time is equal to the set mocked response: ", response.equals(validApproval));
    }

    @Test
    public void testFindApprovalExpirationTimeDefaultValues() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        ApprovalExpirationTime response = databaseApprovalAPI.findApprovalExpirationTime();
        assertTrue("The amount of days equals the default: ", response.getAmountOfDays().equals(DarConstants.DEFAULT_AMOUNT_OF_DAYS));
        assertTrue("The display name equals the default: ", response.getDisplayName().equals(DarConstants.DUOS_DEFAULT));
    }

    @Test
    public void testFindApprovalExpirationTimeSetValues() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(validApproval);
        ApprovalExpirationTime response = databaseApprovalAPI.findApprovalExpirationTime();
        assertTrue("The approval time is equal to the set mocked response: ", response.equals(validApproval));
    }

    @Test
    public void testFindApprovalExpirationTimeById() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(validApproval.getId())).thenReturn(validApproval);
        ApprovalExpirationTime response = databaseApprovalAPI.findApprovalExpirationTimeById(validApproval.getId());
        assertTrue("The approval time is equal to the set mocked response: ", response.equals(validApproval));
    }

    @Test(expected = NotFoundException.class)
    public void testFindApprovalExpirationTimeByIdException() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(validApproval.getId())).thenReturn(null);
        databaseApprovalAPI.findApprovalExpirationTimeById(validApproval.getId());
    }

}