package org.broadinstitute.consent.http.mail.freemarker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreeMarkerTemplateHelperTest {

  private FreeMarkerTemplateHelper helper;

  @Mock
  private FreeMarkerConfiguration freeMarkerConfig;

  @BeforeEach
  public void setUp() throws IOException {
    when(freeMarkerConfig.getTemplateDirectory()).thenReturn("/freemarker");
    when(freeMarkerConfig.getDefaultEncoding()).thenReturn("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);

  }

  @Test
  void testGetNewCaseTemplate() throws Exception {
    Writer template = helper.getNewCaseTemplate("NewCase User", "DARELECTION-1", "DAR-1",
        "localhost:1234");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals("Broad Data Use Oversight System - New DAR ready for your vote",
        parsedTemplate.title());
    assertEquals("Hello NewCase User,", parsedTemplate.getElementById("userName").text());
  }

  @Test
  void testGetReminderTemplate() throws Exception {
    Writer template = helper.getReminderTemplate("Reminder User", "DAR-1", "localhost:1234");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals("Broad Data Use Oversight System - Your vote was requested for a Data Access Request",
        parsedTemplate.title());
    assertEquals("Hello Reminder User,", parsedTemplate.getElementById("userName").text());
  }

  @Test
  void testGetNewDARRequestTemplate() throws Exception {
    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-01");

    Dataset d1 = new Dataset();
    d1.setDacId(1);
    d1.setDatasetName("Dataset-01");
    d1.setDatasetId(1);
    d1.setAlias(1);
    d1.setDatasetIdentifier();

    Map<String, List<String>> dacDatasetGroups = new HashMap<>();
    dacDatasetGroups.put(dac.getName(), List.of(d1.getDatasetIdentifier()));

    Writer template = helper.getNewDARRequestTemplate(
        "localhost:1234",
        "Admin",
        dacDatasetGroups,
        "ResearcherName",
        "DAR-01"
    );
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals("Broad Data Use Oversight System - New DAR submitted to your DAC",
        parsedTemplate.title());
    Element userNameElement = parsedTemplate.getElementById("userName");
    assertNotNull(userNameElement);
    assertNotNull(userNameElement.text());
    assertEquals("Hello Admin,", userNameElement.text());
  }

  @Test
  void testGetNewResearcherLibraryRequestTemplate() throws Exception {
    Writer template = helper.getNewResearcherLibraryRequestTemplate("John Doe",
        "http://localhost:8000/#/");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Request from your researcher for Library Card permissions",
        parsedTemplate.title());

    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains("A researcher from your institution, John Doe, has registered in DUOS"));

    assertEquals("http://localhost:8000/#/", parsedTemplate
        .getElementById("serverUrl")
        .attr("href"));

    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetDataCustodianApprovalTemplate() throws Exception {
    List<DatasetMailDTO> datasetMailDTOs = List.of();
    Writer template = helper.getDataCustodianApprovalTemplate(datasetMailDTOs, "Depositor",
        "Dar Code", "researcher@email.com");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - Researcher - A researcher was approved for your dataset",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains("researcher@email.com was approved by the DAC for the following datasets"));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetDatasetSubmittedTemplate() throws Exception {
    Writer template = helper.getDatasetSubmittedTemplate("dacChairName", "dataSubmitterName",
        "testDataset",
        "dacName");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - Signing Official - Dataset Submitted Notification",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            "A new dataset, testDataset, has been submitted to your DAC, dacName by dataSubmitterName. Please log in to DUOS to review and accept or reject management of this dataset."));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetDaaRequestTemplate() throws Exception {
    String signingOfficialUserName = RandomStringUtils.randomAlphabetic(10);
    String userName = RandomStringUtils.randomAlphabetic(10);
    String daaName = RandomStringUtils.randomAlphabetic(10);
    String serverUrl = RandomStringUtils.randomAlphabetic(10);
    Writer template = helper.getDaaRequestTemplate(signingOfficialUserName, userName,
        daaName,
        serverUrl);
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement-Library Card Relationship Request for your Institution",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("userName")
        .text()
        .contains(
            "Hello " + signingOfficialUserName + ","));
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            userName + " has registered with your institution and is requesting you approve them under the " + daaName + " data access agreement, so that they can request access to data."));
    assertTrue(parsedTemplate
        .getElementById("link")
        .text()
        .contains(
            "Please login to review " + userName + "'s Data Access Agreements."));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetNewDaaUploadSOTemplate() throws Exception {
    String signingOfficialUserName = RandomStringUtils.randomAlphabetic(10);
    String dacName = RandomStringUtils.randomAlphabetic(10);
    String newDaaName = RandomStringUtils.randomAlphabetic(10);
    String previousDaaName = RandomStringUtils.randomAlphabetic(10);
    String serverUrl = RandomStringUtils.randomAlphabetic(10);
    Writer template = helper.getNewDaaUploadSOTemplate(signingOfficialUserName, dacName,
        newDaaName, previousDaaName, serverUrl);
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement Upload",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("userName")
        .text()
        .contains(
            "Dear " + signingOfficialUserName + ","));
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            "You previously pre-authorized researchers under the " + previousDaaName + " which was in use by the " + dacName + "."));
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            "The " + dacName + " has recently transitioned to using the " + newDaaName + " which will apply for all future requests to this DAC."));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetNewDaaUploadResearcherTemplate() throws Exception {
    String researcherUserName = RandomStringUtils.randomAlphabetic(10);
    String dacName = RandomStringUtils.randomAlphabetic(10);
    String newDaaName = RandomStringUtils.randomAlphabetic(10);
    String previousDaaName = RandomStringUtils.randomAlphabetic(10);
    String serverUrl = RandomStringUtils.randomAlphabetic(10);
    Writer template = helper.getNewDaaUploadResearcherTemplate(researcherUserName, dacName,
        newDaaName, previousDaaName, serverUrl);
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement Upload",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("userName")
        .text()
        .contains(
            "Dear " + researcherUserName + ","));
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            "You were previously pre-authorized to request data from the " + dacName + " under the " + previousDaaName + "."));
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            "The " + dacName + " has recently transitioned to using the " + newDaaName + " which will apply for all future requests to this DAC."));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  /* Helper methods */

  private Document getAsHtmlDoc(String parsedHtml) {
    return Jsoup.parse(parsedHtml);
  }

  private final Dataset ds1 = new Dataset(1, "DS-101", "Dataset 1", new Date());
  private final Dataset ds2 = new Dataset(2, "DS-102", "Dataset 2", new Date());
  private final Dataset ds3 = new Dataset(3, "DS-103", "Dataset 3", new Date());

  private List<Dataset> sampleDatasets() {
    return Arrays.asList(ds1, ds2, ds3);
  }

}
