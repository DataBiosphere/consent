package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.testcontainers.containers.MockServerContainer;

@ExtendWith(MockitoExtension.class)
class OntologyServiceTest implements WithMockServer {

  private MockServerClient client;

  private static final MockServerContainer container = new MockServerContainer(IMAGE);

  private OntologyService service;

  @BeforeAll
  static void setUp() {
    container.start();
  }

  @AfterAll
  static void tearDown() {
    container.stop();
  }

  @BeforeEach
  void startUp() {
    client = new MockServerClient(container.getHost(), container.getServerPort());
    client.reset();
  }

  ServicesConfiguration config() {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setLocalURL("http://localhost:8180/");
    config.setOntologyURL(getRootUrl(container));
    return config;
  }

  private void initService() {
    Client client = ClientBuilder.newClient();
    service = new OntologyService(client, config());
  }

  @Test
  void testTranslateDataUseSummary() {
    mockDataUseTranslateSummarySuccess();
    initService();

    DataUse dataUse = new DataUseBuilder()
        .setHmbResearch(true)
        .setDiseaseRestrictions(List.of(""))
        .build();
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
  void testTranslateDataUse() {
    mockDataUseTranslateSuccess();
    initService();

    DataUse dataUse = new DataUseBuilder()
        .setHmbResearch(true)
        .setDiseaseRestrictions(List.of(""))
        .build();
    String translation = service.translateDataUse(dataUse, DataUseTranslationType.DATASET);

    assertEquals("""
        Samples are restricted for use under the following conditions:
        Data is limited for health/medical/biomedical research. [HMB]
        Data use is limited for studying: cancerophobia [DS]
        Commercial use is not prohibited.
        Data use for methods development research irrespective of the specified data use limitations is not prohibited.
        Restrictions for use as a control set for diseases other than those defined were not specified.
        """, translation);

  }

  private void mockDataUseTranslateSummarySuccess() {
    client
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
                        """
                )
        );
  }

  private void mockDataUseTranslateSuccess() {
    client
        .when(request().withMethod("POST").withPath("/translate"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", MediaType.TEXT_PLAIN))
                .withBody("""
                    Samples are restricted for use under the following conditions:
                    Data is limited for health/medical/biomedical research. [HMB]
                    Data use is limited for studying: cancerophobia [DS]
                    Commercial use is not prohibited.
                    Data use for methods development research irrespective of the specified data use limitations is not prohibited.
                    Restrictions for use as a control set for diseases other than those defined were not specified.
                    """)
        );
  }

}
