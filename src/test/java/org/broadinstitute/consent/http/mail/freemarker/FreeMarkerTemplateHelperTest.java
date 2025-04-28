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

//  @Test
//  void testGetDaaRequestTemplate() throws Exception {
//    String signingOfficialUserName = RandomStringUtils.randomAlphabetic(10);
//    String userName = RandomStringUtils.randomAlphabetic(10);
//    String daaName = RandomStringUtils.randomAlphabetic(10);
//    String serverUrl = RandomStringUtils.randomAlphabetic(10);
//    Writer template = helper.getDaaRequestTemplate(signingOfficialUserName, userName,
//        daaName,
//        serverUrl);
//    String templateString = template.toString();
//    final Document parsedTemplate = getAsHtmlDoc(templateString);
//    assertEquals(
//        "Broad Data Use Oversight System - New Data Access Agreement-Library Card Relationship Request for your Institution",
//        parsedTemplate.title());
//    assertTrue(parsedTemplate
//        .getElementById("userName")
//        .text()
//        .contains(
//            "Hello " + signingOfficialUserName + ","));
//    assertTrue(parsedTemplate
//        .getElementById("content")
//        .text()
//        .contains(
//            userName + " has registered with your institution and is requesting you approve them under the " + daaName + " data access agreement, so that they can request access to data."));
//    assertTrue(parsedTemplate
//        .getElementById("link")
//        .text()
//        .contains(
//            "Please login to review " + userName + "'s Data Access Agreements."));
//    // no unspecified values
//    assertFalse(templateString.contains("${"));
//  }
//
//  @Test
//  void testGetNewDaaUploadSOTemplate() throws Exception {
//    String signingOfficialUserName = RandomStringUtils.randomAlphabetic(10);
//    String dacName = RandomStringUtils.randomAlphabetic(10);
//    String newDaaName = RandomStringUtils.randomAlphabetic(10);
//    String previousDaaName = RandomStringUtils.randomAlphabetic(10);
//    String serverUrl = RandomStringUtils.randomAlphabetic(10);
//    Writer template = helper.getNewDaaUploadSOTemplate(signingOfficialUserName, dacName,
//        newDaaName, previousDaaName, serverUrl);
//    String templateString = template.toString();
//    final Document parsedTemplate = getAsHtmlDoc(templateString);
//    assertEquals(
//        "Broad Data Use Oversight System - New Data Access Agreement Upload",
//        parsedTemplate.title());
//    assertTrue(parsedTemplate
//        .getElementById("userName")
//        .text()
//        .contains(
//            "Dear " + signingOfficialUserName + ","));
//    assertTrue(parsedTemplate
//        .getElementById("content")
//        .text()
//        .contains(
//            "You previously pre-authorized researchers under the " + previousDaaName + " which was in use by the " + dacName + "."));
//    assertTrue(parsedTemplate
//        .getElementById("content")
//        .text()
//        .contains(
//            "The " + dacName + " has recently transitioned to using the " + newDaaName + " which will apply for all future requests to this DAC."));
//    // no unspecified values
//    assertFalse(templateString.contains("${"));
//  }
//
//  @Test
//  void testGetNewDaaUploadResearcherTemplate() throws Exception {
//    String researcherUserName = RandomStringUtils.randomAlphabetic(10);
//    String dacName = RandomStringUtils.randomAlphabetic(10);
//    String newDaaName = RandomStringUtils.randomAlphabetic(10);
//    String previousDaaName = RandomStringUtils.randomAlphabetic(10);
//    String serverUrl = RandomStringUtils.randomAlphabetic(10);
//    Writer template = helper.getNewDaaUploadResearcherTemplate(researcherUserName, dacName,
//        newDaaName, previousDaaName, serverUrl);
//    String templateString = template.toString();
//    final Document parsedTemplate = getAsHtmlDoc(templateString);
//    assertEquals(
//        "Broad Data Use Oversight System - New Data Access Agreement Upload",
//        parsedTemplate.title());
//    assertTrue(parsedTemplate
//        .getElementById("userName")
//        .text()
//        .contains(
//            "Dear " + researcherUserName + ","));
//    assertTrue(parsedTemplate
//        .getElementById("content")
//        .text()
//        .contains(
//            "You were previously pre-authorized to request data from the " + dacName + " under the " + previousDaaName + "."));
//    assertTrue(parsedTemplate
//        .getElementById("content")
//        .text()
//        .contains(
//            "The " + dacName + " has recently transitioned to using the " + newDaaName + " which will apply for all future requests to this DAC."));
//    // no unspecified values
//    assertFalse(templateString.contains("${"));
//  }

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
