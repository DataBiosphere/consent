package org.broadinstitute.consent.http.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.gson.JsonArray;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.List;
import org.broadinstitute.consent.http.MockServerTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.service.ontology.OntologyDAO;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.util.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.Header;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class OntologyServiceTest extends MockServerTestHelper {

  private OntologyService service;
  private TestAppender testAppender;
  @Mock private OntologyDAO ontologyDAO;
  @Mock private OntologyIndexService indexService;

  ServicesConfiguration config() {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setLocalURL("http://localhost:8180/");
    config.setOntologyURL(getRootUrl(CONTAINER));
    return config;
  }

  @BeforeEach
  void setUp() {
    Logger testLogger = (Logger) LoggerFactory.getLogger(OntologyService.class);
    testLogger.setLevel(Level.TRACE);
    testAppender = new TestAppender();
    testAppender.reset();
    testLogger.addAppender(testAppender);
    testAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    testAppender.start();
    Client client = ClientBuilder.newClient();
    service = new OntologyService(client, config(), ontologyDAO, indexService);
  }

  @Test
  void testTranslateDataUseSummary() {
    mockDataUseTranslateSummarySuccess();

    DataUse dataUse =
        new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(List.of("")).build();
    DataUseSummary translation = service.translateDataUseSummary(dataUse);

    assertNotNull(translation);

    assertEquals(2, translation.getPrimary().size());

    assertEquals("HMB", translation.getPrimary().get(0).getCode());
    assertFalse(translation.getPrimary().get(0).getDescription().isEmpty());

    assertEquals("DS", translation.getPrimary().get(1).getCode());
    assertFalse(translation.getPrimary().get(1).getDescription().isEmpty());

    assertEquals(4, translation.getSecondary().size());

    assertEquals("NCU", translation.getSecondary().get(0).getCode());
    assertFalse(translation.getSecondary().get(0).getDescription().isEmpty());

    assertEquals("NMDS", translation.getSecondary().get(1).getCode());
    assertFalse(translation.getSecondary().get(1).getDescription().isEmpty());

    assertEquals("NCTRL", translation.getSecondary().get(2).getCode());
    assertFalse(translation.getSecondary().get(2).getDescription().isEmpty());

    assertEquals("OTHER", translation.getSecondary().get(3).getCode());
    assertFalse(translation.getSecondary().get(3).getDescription().isEmpty());
  }

  @Test
  void testTranslateDataUseSummaryErrorLogging() {
    mockServerClient
        .when(request().withMethod("POST").withPath("/translate/summary"))
        .respond(
            response()
                .withStatusCode(500)
                .withHeaders(new Header("Content-Type", MediaType.TEXT_PLAIN))
                .withBody("Internal Server Error"));

    DataUse dataUse =
        new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(List.of("")).build();

    DataUseSummary summary = service.translateDataUseSummary(dataUse);
    assertNull(summary);
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().getFirst();
    assertThat(
        event.getFormattedMessage(),
        containsString("Error parsing response from Ontology service:"));
  }

  @Test
  void testTranslateDataUse() {
    mockDataUseTranslateSuccess();

    DataUse dataUse =
        new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(List.of("")).build();
    String translation = service.translateDataUse(dataUse, DataUseTranslationType.DATASET);

    assertEquals(
        """
            Samples are restricted for use under the following conditions:
            Data is limited for health/medical/biomedical research. [HMB]
            Data use is limited for studying: cancerophobia [DS]
            Commercial use is not prohibited.
            Data use for methods development research irrespective of the specified data use limitations is not prohibited.
            Restrictions for use as a control set for diseases other than those defined were not specified.
            """,
        translation);
  }

  @Test
  void testTranslateDataUseError() {
    mockServerClient
        .when(request().withMethod("POST").withPath("/translate"))
        .respond(
            response()
                .withStatusCode(500)
                .withHeaders(new Header("Content-Type", MediaType.TEXT_PLAIN))
                .withBody("Internal Server Error"));

    DataUse dataUse =
        new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(List.of("")).build();

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> service.translateDataUse(dataUse, DataUseTranslationType.DATASET));

    String expectedMessage = "Error response from Ontology service: Internal Server Error";
    String actualMessage = exception.getMessage();

    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testDeleteOntologyTerms() {
    doNothing().when(ontologyDAO).deleteByOntology(OntologyType.DUO.name());
    assertDoesNotThrow(() -> service.deleteOntologyTerms(OntologyType.DUO));
  }

  @Test
  void testIndexOntology() throws Exception {
    when(indexService.generateTerms(OntologyType.DUO)).thenReturn(List.of());
    User user = new User();
    user.setUserId(1);
    assertDoesNotThrow(() -> service.indexOntology(user, OntologyType.DUO));
  }

  @Test
  void testFindByTermIds() throws Exception {
    String[] termIds = new String[] {"DUO_0000006", "DUO_0000007"};
    String json =
        """
          [{id:"DUO_0000006"}, {id:"DUO_0000007"}]
        """;
    when(ontologyDAO.findByTermIds(termIds))
        .thenReturn(
            output -> {
              // Mock streaming output that writes an empty JSON array
              output.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            });
    StreamingOutput results = service.findByTermIds(termIds);
    assertNotNull(results);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(results);
    assertEquals(2, jsonArray.size());
  }

  private void mockDataUseTranslateSummarySuccess() {
    mockServerClient
        .when(request().withMethod("POST").withPath("/translate/summary"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody(
                    """
                        {
                          "primary": [
                            {
                              "code": "HMB",
                              "description": "Data is limited for health/medical/biomedical research."
                            },
                            {
                              "code": "DS",
                              "description": "Data use is limited for studying: cancerophobia"
                            }
                          ],
                          "secondary": [
                            {
                              "code": "NCU",
                              "description": "Commercial use is not prohibited."
                            },
                            {
                              "code": "NMDS",
                              "description": "Data use for methods development research irrespective of the specified data use limitations is not prohibited."
                            },
                            {
                              "code": "NCTRL",
                              "description": "Restrictions for use as a control set for diseases other than those defined were not specified."
                            },
                            {
                              "code": "OTHER",
                              "description": "Genomic summary results from this study are available only through controlled-access"
                            }
                          ]
                        }
                        """));
  }

  private void mockDataUseTranslateSuccess() {
    mockServerClient
        .when(request().withMethod("POST").withPath("/translate"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", MediaType.TEXT_PLAIN))
                .withBody(
                    """
                        Samples are restricted for use under the following conditions:
                        Data is limited for health/medical/biomedical research. [HMB]
                        Data use is limited for studying: cancerophobia [DS]
                        Commercial use is not prohibited.
                        Data use for methods development research irrespective of the specified data use limitations is not prohibited.
                        Restrictions for use as a control set for diseases other than those defined were not specified.
                        """));
  }
}
