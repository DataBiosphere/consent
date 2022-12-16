package org.broadinstitute.consent.http.service;

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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.mail.MessagingException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

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

    @Mock
    private SendGridAPI sendGridAPI;


    private final String defaultAccount = "duos-dev@broadinstitute.org";

    @Before
    public void setUp() {
    }

    private void initRealService() {
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

    private void initFakeService() {
        String serverUrl =  "http://localhost:8000/#/";

        openMocks(this);

        FreeMarkerConfiguration fmConfig = new FreeMarkerConfiguration();
        fmConfig.setDefaultEncoding("UTF-8");
        fmConfig.setTemplateDirectory("/freemarker");
        FreeMarkerTemplateHelper helper = new FreeMarkerTemplateHelper(fmConfig);
        service = new EmailService(collectionDAO, consentDAO, voteDAO, electionDAO, userDAO,
                emailDAO, sendGridAPI, helper, serverUrl);
    }

    @Test
    public void testSendDataCustodianApprovalMessage() {
        initRealService();
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
    public void testSendNewResearcherEmail() throws MessagingException {
        initFakeService();
        User user = new User();
        user.setUserId(1234);
        user.setDisplayName("John Doe");

        User so = new User();
        user.setEmail("fake_email@asdf.com");
        try {
            service.sendNewResearcherMessage(user, so);
        } catch (Exception e) {
            fail("Should not fail sending message: " + e);
        }

        verify(sendGridAPI, times(1)).sendDatasetDeniedMessage(any(), any());

        verify(emailDAO, times(1)).insert(
                eq("1234"),
                eq(null),
                eq(1234),
                eq(EmailType.NEW_RESEARCHER.getTypeInt()),
                any(),
                eq("""
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml" style="font-family: 'Roboto', sans-serif ;">
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Broad Data Use Oversight System - Admin - Dataset Approved Notification</title>
                </head>

                <body style="font-family: 'Roboto', sans-serif ; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100% ; height: 100%; color: #777777; margin: 0;">
                <center style="font-family: 'Roboto', sans-serif ;">
                    <table width="600" bgcolor="#eeeeee" style="border-collapse: collapse ; font-family: 'Roboto', sans-serif ; box-shadow: 3px 3px 0 #cccccc ; border-radius: 5px ; -moz-border-radius: 5px ; margin-top: 20px; background-color: #eeeeee;">
                        <tr width="600" style="font-family: 'Roboto', sans-serif;">
                            <td align="left" style="font-family: 'Roboto', sans-serif ; font-size: 14px; color: #777777; text-align: left; line-height: 21px; padding: 20px 30px 25px 30px;">
                                <img src="http://imageshack.com/a/img905/7554/gjprR0.png" alt="Broad Institute Logo" style=" margin-top: 10px; display: inline-block; font-family: 'Roboto', sans-serif ;">
                            </td>
                        </tr>
                        <tr width="600" align="center" bgcolor="#dedede" style="font-family: 'Roboto', sans-serif; background-color: #dedede; display: inline-table; text-align: center; margin: 0;">
                            <td width="600" align="center" style="border-collapse: collapse; font-family: 'Roboto', sans-serif; font-size: 26px; color: #777777; text-align: center; line-height: 21px; font-weight: bold; padding: 30px;">Broad Data Use Oversight System</td>
                        </tr>
                        <tr align="center" style="font-family: 'Roboto', sans-serif ; padding-top: 20px; margin-top: 20px;">
                            <td id="userName" align="left" style="border-collapse: collapse; font-family: 'Roboto', sans-serif ; font-size: 22px; color: #777777; text-align: left; line-height: 21px; display: block; font-weight: 500; padding: 25px 30px 20px 30px;">Hello,</td>
                        </tr>
                        <tr width="600" style="font-family: 'Roboto', sans-serif ;">
                            <td align="left" style="border-collapse: collapse; font-family: 'Roboto', sans-serif ; font-size: 16px; color: #777777; text-align: left; line-height: 25px; padding: 0px 30px 20px 30px;">
                                <p>A researcher from your institution, John Doe, has registered in DUOS and listed you as their Signing Official. In order to request access to data, they will need to be issued a Library Card. Please log in to DUOS here: Broad Data Use Oversight System and review the terms of the Library Card Agreements for this researcher in your Signing Official Console. If you have any questions, please contact <a href="mailto:duos-support@broadinstitute.zendesk.com">duos-support@broadinstitute.zendesk.com</a>.</p>
                                <p style="line-height: 10px !important; margin-bottom: 5px;">Kind regards,</p>
                                <p style="margin-top: 0;">The DUOS team</p>
                            </td>
                        </tr>
                    </table>
                    <table width ="600" style="font-family: 'Roboto', sans-serif ; margin-top: 15px;">
                        <tr align="center" style="font-family: 'Roboto', sans-serif ; color: #999999;">
                            <td align="center" valign="middle" style="padding: 0 10px 0 10px; border-collapse: collapse; font-family: 'Roboto', sans-serif; font-size: 14px; color: #999999; text-align: center; line-height: 21px; display: inline-block; vertical-align: middle;">	&#169; Broad Institute </td>
                        </tr>
                    </table>
                </center>
                </body>
                </html>
                """),
                any(),
                any(),
                any()
        );

    }

}
