package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        expirationTimeService = new ApprovalExpirationTimeService(approvalExpirationTimeDAO, userDAO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateException() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(validApproval);
        expirationTimeService.create(validApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsMissingAmountOfDays() {
        ApprovalExpirationTime missingDaysApproval = new ApprovalExpirationTime(1, 123, new Date(), new Date(), null, "Testing approval period");
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        expirationTimeService.create(missingDaysApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsMissingUserId() {
        ApprovalExpirationTime missingUserIdApproval = new ApprovalExpirationTime(1, null, new Date(), new Date(), 5, "Testing approval period");
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        expirationTimeService.create(missingUserIdApproval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateFieldsNoUserForId() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        when(userDAO.findUserById(validApproval.getUserId())).thenReturn(null);
        expirationTimeService.create(validApproval);
    }

    @Test
    public void testCreate() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(anyInt())).thenReturn(validApproval);
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        when(userDAO.findUserById(anyInt())).thenReturn(validUser);
        ApprovalExpirationTime aet = expirationTimeService.create(validApproval);
        assertNotNull(aet);
    }

    @Test
    public void testUpdate() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(anyInt())).thenReturn(validApproval);
        when(userDAO.findUserById(validApproval.getUserId())).thenReturn(validUser);
        ApprovalExpirationTime response = expirationTimeService.update(validApproval, 1);
        assertEquals("The approval time is equal to the set mocked response: ", response, validApproval);
    }

    @Test
    public void testFindApprovalExpirationTimeDefaultValues() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(null);
        ApprovalExpirationTime response = expirationTimeService.findApprovalExpirationTime();
        assertEquals("The amount of days equals the default: ", response.getAmountOfDays(), DarConstants.DEFAULT_AMOUNT_OF_DAYS);
        assertEquals("The display name equals the default: ", response.getDisplayName(), DarConstants.DUOS_DEFAULT);
    }

    @Test
    public void testFindApprovalExpirationTimeSetValues() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTime()).thenReturn(validApproval);
        ApprovalExpirationTime response = expirationTimeService.findApprovalExpirationTime();
        assertEquals("The approval time is equal to the set mocked response: ", response, validApproval);
    }

    @Test
    public void testFindApprovalExpirationTimeById() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(validApproval.getId())).thenReturn(validApproval);
        ApprovalExpirationTime response = expirationTimeService.findApprovalExpirationTimeById(validApproval.getId());
        assertEquals("The approval time is equal to the set mocked response: ", response, validApproval);
    }

    @Test(expected = NotFoundException.class)
    public void testFindApprovalExpirationTimeByIdException() {
        when(approvalExpirationTimeDAO.findApprovalExpirationTimeById(validApproval.getId())).thenReturn(null);
        expirationTimeService.findApprovalExpirationTimeById(validApproval.getId());
    }

}