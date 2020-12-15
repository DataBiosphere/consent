package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Date;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.ApprovalExpirationTimeDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.DarConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ApprovalExpirationTimeServiceTest {

    @Mock
    private ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
    @Mock
    private UserDAO userDAO;

    private ApprovalExpirationTimeService expirationTimeService;

    ApprovalExpirationTime validApproval = new ApprovalExpirationTime(1, 123, new Date(), new Date(), 5, "Testing approval period");
    User validUser = new User();

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        expirationTimeService = new ApprovalExpirationTimeService(approvalExpirationTimeDAO, userDAO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateException() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(validApproval);
        expirationTimeService.create(validApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsMissingAmountOfDays() throws Exception {
        ApprovalExpirationTime missingDaysApproval = new ApprovalExpirationTime(1, 123, new Date(), new Date(), null, "Testing approval period");
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        expirationTimeService.create(missingDaysApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsMissingUserId() throws Exception {
        ApprovalExpirationTime missingUserIdApproval = new ApprovalExpirationTime(1, null, new Date(), new Date(), 5, "Testing approval period");
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        expirationTimeService.create(missingUserIdApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsNoUserForId() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        when(userDAO.findUserById(validApproval.getUserId())).thenReturn(null);
        expirationTimeService.create(validApproval);
    }

    @Test
    public void testCreate() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(anyInt())).thenReturn(validApproval);
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        when(userDAO.findUserById(anyInt())).thenReturn(validUser);
        expirationTimeService.create(validApproval);
    }

    @Test
    public void testUpdate() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(anyInt())).thenReturn(validApproval);
        when(userDAO.findUserById(validApproval.getUserId())).thenReturn(validUser);
        ApprovalExpirationTime response = expirationTimeService.update(validApproval, 1);
        assertTrue("The approval time is equal to the set mocked response: ", response.equals(validApproval));
    }

    @Test
    public void testFindApprovalExpirationTimeDefaultValues() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        ApprovalExpirationTime response = expirationTimeService.findApprovalExpirationTime();
        assertTrue("The amount of days equals the default: ", response.getAmountOfDays().equals(DarConstants.DEFAULT_AMOUNT_OF_DAYS));
        assertTrue("The display name equals the default: ", response.getDisplayName().equals(DarConstants.DUOS_DEFAULT));
    }

    @Test
    public void testFindApprovalExpirationTimeSetValues() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(validApproval);
        ApprovalExpirationTime response = expirationTimeService.findApprovalExpirationTime();
        assertTrue("The approval time is equal to the set mocked response: ", response.equals(validApproval));
    }

    @Test
    public void testFindApprovalExpirationTimeById() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(validApproval.getId())).thenReturn(validApproval);
        ApprovalExpirationTime response = expirationTimeService.findApprovalExpirationTimeById(validApproval.getId());
        assertTrue("The approval time is equal to the set mocked response: ", response.equals(validApproval));
    }

    @Test(expected = NotFoundException.class)
    public void testFindApprovalExpirationTimeByIdException() throws Exception {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(validApproval.getId())).thenReturn(null);
        expirationTimeService.findApprovalExpirationTimeById(validApproval.getId());
    }

}