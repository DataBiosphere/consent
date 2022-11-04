package org.broadinstitute.consent.http.service.mail;

import com.google.api.client.http.HttpStatusCodes;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.models.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SendGridAPITest {

    private static final String TO = "to@broadinstitute.org";
    private static final String ID = "DUL-123";
    private static final String TYPE = "Data Use Limitations";
    private SendGridAPI sendGridAPI;

    @Mock
    private SendGrid sendGrid;

    @Mock
    private Writer template;

    @Mock
    private UserDAO userDAO;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        // For most tests, we don't want to actually make an external call to SendGrid.
        configureApi(false);
        doNothing().when(template).write(anyString());
    }

    private void configureApi(boolean active) {
        MailConfiguration config = new MailConfiguration();
        config.setSendGridApiKey("test");
        config.setGoogleAccount("from@broadinstitute.org");
        config.setActivateEmailNotifications(active);
        sendGridAPI = new SendGridAPI(config, userDAO);
        sendGridAPI.setSendGrid(sendGrid);
    }

    @Test
    public void testSuccessfulAPICall() throws Exception {
        configureApi(true);
        when(sendGrid.getHost()).thenReturn("host");
        when(sendGrid.getVersion()).thenReturn("version");
        when(sendGrid.getRequestHeaders()).thenReturn(Map.of());
        Response sendGridResponse = mock(Response.class);
        when(sendGrid.makeCall(any())).thenReturn(sendGridResponse);
        User user = mock(User.class);
        when(user.getEmailPreference()).thenReturn(true);
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        Optional<Response> response = sendGridAPI.sendCollectMessage(TO, ID, TYPE, template);
        assertNotNull(response);
        assertTrue(response.isPresent());
        assertNotEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.get().getStatusCode());
    }

    @Test
    public void testFailedAPICall() throws Exception {
        configureApi(true);
        when(sendGrid.getHost()).thenReturn("host");
        when(sendGrid.getVersion()).thenReturn("version");
        when(sendGrid.getRequestHeaders()).thenReturn(Map.of());
        when(sendGrid.makeCall(any())).thenThrow(new IOException("Mock Failure"));
        User user = mock(User.class);
        when(user.getEmailPreference()).thenReturn(true);
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        Optional<Response> response = sendGridAPI.sendCollectMessage(TO, ID, TYPE, template);
        assertNotNull(response);
        assertTrue(response.isPresent());
        assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.get().getStatusCode());
    }

    @Test
    public void testUserDisabled() throws Exception {
        configureApi(true);
        when(sendGrid.getHost()).thenReturn("host");
        when(sendGrid.getVersion()).thenReturn("version");
        when(sendGrid.getRequestHeaders()).thenReturn(Map.of());
        when(sendGrid.makeCall(any())).thenThrow(new IOException("Mock Failure"));
        User user = mock(User.class);
        when(user.getEmailPreference()).thenReturn(false);
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        Optional<Response> response = sendGridAPI.sendCollectMessage(TO, ID, TYPE, template);
        assertNotNull(response);
        assertFalse(response.isPresent());
    }

    @Test
    public void testUserEmailNotFound() throws Exception {
        configureApi(true);
        when(sendGrid.getHost()).thenReturn("host");
        when(sendGrid.getVersion()).thenReturn("version");
        when(sendGrid.getRequestHeaders()).thenReturn(Map.of());
        when(sendGrid.makeCall(any())).thenThrow(new IOException("Mock Failure"));
        when(userDAO.findUserByEmail(any())).thenReturn(null);
        Optional<Response> response = sendGridAPI.sendCollectMessage(TO, ID, TYPE, template);
        assertNotNull(response);
        assertFalse(response.isPresent());
    }

    @Test
    public void testCollectMessage() {
        try {
            sendGridAPI.sendCollectMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewCaseMessage() {
        try {
            sendGridAPI.sendNewCaseMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testReminderMessage() {
        try {
            sendGridAPI.sendReminderMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDisabledDatasetMessage() {
        try {
            sendGridAPI.sendDisabledDatasetMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewDARRequests() {
        try {
            sendGridAPI.sendNewDARRequests(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testCancelDARRequestMessage() {
        try {
            sendGridAPI.sendCancelDARRequestMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testFlaggedDarAdminApprovedMessage() {
        try {
            sendGridAPI.sendFlaggedDarAdminApprovedMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testClosedDatasetElectionsMessage() {
        try {
            sendGridAPI.sendClosedDatasetElectionsMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDelegateResponsibilitiesMessage() {
        try {
            sendGridAPI.sendDelegateResponsibilitiesMessage(TO, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherApprovedMessage() {
        try {
            sendGridAPI.sendNewResearcherApprovedMessage(TO, template, "Test");
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDataCustodianApprovalMessage() {
        try {
            sendGridAPI.sendDataCustodianApprovalMessage(TO, "Test", template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

}
