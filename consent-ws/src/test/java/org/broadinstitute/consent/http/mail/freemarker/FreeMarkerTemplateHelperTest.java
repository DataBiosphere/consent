package org.broadinstitute.consent.http.mail.freemarker;

import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.darsummary.SummaryItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class FreeMarkerTemplateHelperTest {

    private FreeMarkerTemplateHelper helper;

    @Mock
    private FreeMarkerConfiguration freeMarkerConfig;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
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
        Writer template = helper.getNewDARRequestTemplate("localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - New Data Access Request"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello Admin!"));
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
    public void testGetApprovedDarTemplate() throws Exception {
        Writer template = helper.getApprovedDarTemplate("ApprovedDar User", new Date().toString(), "DAR-1", "SomeInvestigator", "SomeInstitution",
                "SomePurpose", Arrays.asList(item1, item2, item3), "SomeDiseaseArea",
                checkedSentences(), "SomeTranslatedUseRestriction", Arrays.asList(piModel1, piModel2, piModel3),
                "4", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Dataset Owner - DAR Approved Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello ApprovedDar User!"));
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
    public void testGetUserDelegateResponsibilitiesTemplate() throws Exception {
        Writer template = helper.getUserDelegateResponsibilitiesTemplate("DelegateUser", Arrays.asList(vae1, vae2, vae3), "DataOwner", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Delegated Responsibilities Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello DelegateUser!"));
    }

    @Test
    public void testGetNewResearcherCreatedTemplate() throws Exception {
        Writer template = helper.getNewResearcherCreatedTemplate("Administrator", "Researcher Name", "localhost:1234", "registered");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - New Researcher Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello Administrator!"));
        assertTrue(parsedTemplate.getElementById("researcherName").text().equals("A Researcher, Researcher Name, has been registered in DUOS. Please click on the following link to review the user profile and classify him as Bonafide"));
    }

    /* Helper methods */

    private Document getAsHtmlDoc(String parsedHtml){
        return Jsoup.parse(parsedHtml);
    }

    private DataSet ds1 = new DataSet(1, "DS-101", "Dataset 1", new Date(), true);
    private DataSet ds2 = new DataSet(2, "DS-102", "Dataset 2", new Date(), true);
    private DataSet ds3 = new DataSet(3, "DS-103", "Dataset 3", new Date(), true);
    private DACUser testDacUser = new DACUser(1, "testuser@email.com", "Test User", new Date());
    private Election e1 = new Election(1, "DataSet", "Closed", new Date(), "DAR-1", null , true, 1);
    private Election e2 = new Election(2, "DataSet", "Closed", new Date(), "DAR-1", null , false, 2);
    private Election e3 = new Election(3, "DataSet", "Closed", new Date(), "DAR-2", null , true, 1);

    private VoteAndElectionModel vae1 = new VoteAndElectionModel("Identifier1", "DAR-1", "DataSet", "DataOwner");
    private VoteAndElectionModel vae2 = new VoteAndElectionModel("Identifier2", "DAR-2", "DataSet", "DataOwner");
    private VoteAndElectionModel vae3 = new VoteAndElectionModel("Identifier3", "DAR-3", "DataSet", "DataOwner");

    private SummaryItem item1 = new SummaryItem("A sample item 1", "Sample item 1");
    private SummaryItem item2 = new SummaryItem("A sample item 2", "Sample item 2");
    private SummaryItem item3 = new SummaryItem("A sample item 3", "Sample item 3");

    private DataSetPIMailModel piModel1 = new DataSetPIMailModel("DS-101", "Dataset 1");
    private DataSetPIMailModel piModel2 = new DataSetPIMailModel("DS-102", "Dataset 2");
    private DataSetPIMailModel piModel3 = new DataSetPIMailModel("DS-102", "Dataset 3");

    private List<DataSet> sampleDatasets(){
        return Arrays.asList(ds1, ds2, ds3);
    }

    private Map<DACUser, List<DataSet>> getApprovedDarMap(){
        Map<DACUser, List<DataSet>> approvedDarMap = new HashMap<>();
        approvedDarMap.put(testDacUser, sampleDatasets());
        return approvedDarMap;
    }

    private Map<String, List<Election>> getClosedDsElections(){
        Map<String, List<Election>> closedDatasetElections = new HashMap<>();
        closedDatasetElections.put("DAR-1", Arrays.asList(e1, e2));
        closedDatasetElections.put("DAR-2", Arrays.asList(e3));
        return closedDatasetElections;
    }

    private List<String> checkedSentences(){
        return Arrays.asList("I checked this sentence", "Also this other", "And another one");
    }
}