package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class FreeMarkerTemplateHelperTest {

    private FreeMarkerTemplateHelper helper;

    @Mock
    private FreeMarkerConfiguration freeMarkerConfig;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        when(freeMarkerConfig.getTemplateDirectory()).thenReturn("/freemarker");
        when(freeMarkerConfig.getDefaultEncoding()).thenReturn("UTF-8");
        helper = new FreeMarkerTemplateHelper(freeMarkerConfig);

    }

    @Test
    public void testGetDisabledDatasetsTemplate() throws Exception {
        Writer template = helper.getDisabledDatasetsTemplate("DatasetTemp User", sampleDatasets().stream().map(ds -> ds.getObjectId()).collect(Collectors.toList()), "entityId", "serverUrl");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Disabled Datasets Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello DatasetTemp User!"));
        assertTrue(templateString.contains("DS-101"));
        assertTrue(templateString.contains("DS-102"));
        assertTrue(templateString.contains("DS-103"));
    }

    @Test
    public void testGetCollectTemplate() throws Exception {
        Writer template = helper.getCollectTemplate("CollectTemplate User", "DARELECTION-1", "DAR-1", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Collect Votes Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello CollectTemplate User!"));
        assertTrue(templateString.contains("DAR-1"));
    }

    @Test
    public void testGetNewCaseTemplate() throws Exception {
        Writer template = helper.getNewCaseTemplate("NewCase User", "DARELECTION-1", "DAR-1", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - New Case Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello NewCase User!"));
    }

    @Test
    public void testGetReminderTemplate() throws Exception {
        Writer template = helper.getReminderTemplate("Reminder User", "DARELECTION-1", "DAR-1", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Vote Reminder"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello Reminder User!"));
    }

    @Test
    public void testGetNewDARRequestTemplate() throws Exception {
        Writer template = helper.getNewDARRequestTemplate("localhost:1234", "Admin", "Entity");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertEquals("Broad Data Use Oversight System - New Data Access Request", parsedTemplate.title());
        Element userNameElement = parsedTemplate.getElementById("userName");
        assertNotNull(userNameElement);
        assertNotNull(userNameElement.text());
        assertEquals("Hello Admin!", userNameElement.text());
    }

    @Test
    public void testGetCancelledDarTemplate() throws Exception {
        Writer template = helper.getCancelledDarTemplate("DARELECTION-1", "DAR-1", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Cancelled Data Access Request"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello Admin!"));
    }

    @Test
    public void testGetAdminApprovedDarTemplate() throws Exception {
        Writer template = helper.getAdminApprovedDarTemplate("AdminApproved User", "DARELECTION-1", getApprovedDarMap(), "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Admin - DAR Approved Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello AdminApproved User!"));
    }

    @Test
    public void testGetClosedDatasetElectionsTemplate() throws Exception {
        Writer template = helper.getClosedDatasetElectionsTemplate(getClosedDsElections(), "DarCode",  "SomeType", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Closed Dataset Elections"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello Admin!"));
    }

    @Test
    public void testGetNewResearcherLibraryRequestTemplate() throws Exception {
        Writer template = helper.getNewResearcherLibraryRequestTemplate("John Doe", "http://localhost:8000/#/");
        String templateString = template.toString();

        assertEquals(
                """
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml" style="font-family: 'Roboto', sans-serif ;">
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Broad Data Use Oversight System - New Researcher</title>
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
                                <p>A researcher from your institution, John Doe, has registered in DUOS and listed you as their Signing Official. In order to request access to data, they will need to be issued a Library Card. Please log in to DUOS <a href="http://localhost:8000/#/" align="center" style="text-decoration: none; font-family: 'Roboto', sans-serif; color: #00609F;">here</a> and review the terms of the Library Card Agreements for this researcher in your Signing Official Console. If you have any questions, please contact <a href="mailto:duos-support@broadinstitute.zendesk.com" style="text-decoration: none; font-family: 'Roboto', sans-serif; color: #00609F;">duos-support@broadinstitute.zendesk.com</a>.</p>
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
                """, templateString);

    }

    /* Helper methods */

    private Document getAsHtmlDoc(String parsedHtml){
        return Jsoup.parse(parsedHtml);
    }

    private Dataset ds1 = new Dataset(1, "DS-101", "Dataset 1", new Date(), true);
    private Dataset ds2 = new Dataset(2, "DS-102", "Dataset 2", new Date(), true);
    private Dataset ds3 = new Dataset(3, "DS-103", "Dataset 3", new Date(), true);
    private User testUser = new User(1, "testuser@email.com", "Test User", new Date(), null);
    private Election e1 = new Election(1, "DataSet", "Closed", new Date(), "DAR-1", null , true, 1);
    private Election e2 = new Election(2, "DataSet", "Closed", new Date(), "DAR-1", null , false, 2);
    private Election e3 = new Election(3, "DataSet", "Closed", new Date(), "DAR-2", null , true, 1);

    private List<Dataset> sampleDatasets(){
        return Arrays.asList(ds1, ds2, ds3);
    }

    private Map<User, List<Dataset>> getApprovedDarMap(){
        Map<User, List<Dataset>> approvedDarMap = new HashMap<>();
        approvedDarMap.put(testUser, sampleDatasets());
        return approvedDarMap;
    }

    private Map<String, List<Election>> getClosedDsElections(){
        Map<String, List<Election>> closedDatasetElections = new HashMap<>();
        closedDatasetElections.put("DAR-1", Arrays.asList(e1, e2));
        closedDatasetElections.put("DAR-2", Arrays.asList(e3));
        return closedDatasetElections;
    }
}
