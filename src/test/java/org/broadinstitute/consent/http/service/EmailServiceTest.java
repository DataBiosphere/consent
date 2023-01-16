package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * This class can be used to functionally test email notifications as well as unit test.
 * To enable functional tests, configure MailService with correct values (i.e. is active, sendgrid key, etc.)
 * Functional test emails will be directed to the private google group:
 *      https://groups.google.com/a/broadinstitute.org/g/duos-dev
 */
public class EmailServiceTest {

    private EmailService service;

    @Mock
    private DarCollectionDAO collectionDAO;

    @Mock
    private ConsentDAO consentDAO;

    @Mock
    private VoteDAO voteDAO;

    @Mock
    private ElectionDAO electionDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private MailMessageDAO emailDAO;

    private final String defaultAccount = "duos-dev@broadinstitute.org";

    @Before
    public void setUp() {
    }

    private void initService() {
        String serverUrl =  "http://localhost:8000/#/";
        boolean serviceActive = false;

        openMocks(this);
        MailConfiguration mConfig = new MailConfiguration();
        mConfig.setActivateEmailNotifications(serviceActive);
        mConfig.setGoogleAccount("");
        mConfig.setSendGridApiKey("");
        SendGridAPI sendGridAPI = new SendGridAPI(mConfig, userDAO);

        FreeMarkerConfiguration fmConfig = new FreeMarkerConfiguration();
        fmConfig.setDefaultEncoding("UTF-8");
        fmConfig.setTemplateDirectory("/freemarker");
        FreeMarkerTemplateHelper helper = new FreeMarkerTemplateHelper(fmConfig);
        service = new EmailService(collectionDAO, consentDAO, voteDAO, electionDAO, userDAO,
                emailDAO, sendGridAPI, helper, serverUrl);
    }

    @Test
    public void testSendDataCustodianApprovalMessage() {
        initService();
        String darCode = "DAR-123456789";
        List<DatasetMailDTO> datasets = new ArrayList<>();
        datasets.add(new DatasetMailDTO("DS-1 Name", "DS-1 Alias"));
        datasets.add(new DatasetMailDTO("DS-2 Name", "DS-2 Alias"));
        datasets.add(new DatasetMailDTO("DS-3 Name", "DS-3 Alias"));
        String dataDepositorName = "Data Depositor Name";
        String researcherEmail = "researcher@test.com";
        User user = new User();
        user.setEmail(defaultAccount);
        user.setUserId(1);
        try {
            service.sendDataCustodianApprovalMessage(user, darCode, datasets,
                    dataDepositorName, researcherEmail);
        } catch (Exception e) {
            fail("Should not fail sending message: " + e);
        }
    }

    @Test
    public void testFetchEmails(){
        List<MailMessage>  mailMessages = generateMailMessageList();
        initService();
        when(emailDAO.fetchMessagesByType(any(), anyInt(), anyInt())).thenReturn(mailMessages);
        assertEquals(2, service.fetchEmailMessagesByType(EmailType.COLLECT, 20, 0).size());
    }

    @Test
    public void testFetchEmailsByCreateDate(){
        List<MailMessage>  mailMessages = generateMailMessageList();
        initService();
        Date startDate = new Date();
        Date endDate = new Date();
        when(emailDAO.fetchMessagesByCreateDate(any(), any(), anyInt(), anyInt())).thenReturn(mailMessages);
        assertEquals(2, service.fetchEmailMessagesByCreateDate(startDate, endDate,20, 0).size());
    }

    private List<MailMessage> generateMailMessageList() {
        return Collections.nCopies(2, generateMailMessage());
    }
    private MailMessage generateMailMessage() {
        return new MailMessage(
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomStringUtils.randomAlphanumeric(10),
                new Date(),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(),
                new Date()
        );
    }
}
