package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.matching.TranslationUtil.DS;
import static org.broadinstitute.consent.http.matching.TranslationUtil.HMB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import jakarta.ws.rs.core.StreamingOutput;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.matching.TranslationUtil;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.service.ontology.OntologyDAO;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.util.TestAppender;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class OntologyServiceTest extends AbstractTestHelper {

  private OntologyService service;
  private ExecutorService executorService;
  private TestAppender testAppender;
  private final Gson gson = GsonUtil.getInstance();
  @Mock private OntologyDAO ontologyDAO;
  @Mock private OntologyIndexService indexService;

  @BeforeEach
  void setUp() {
    Logger testLogger = (Logger) LoggerFactory.getLogger(OntologyService.class);
    testLogger.setLevel(Level.TRACE);
    testAppender = new TestAppender();
    testAppender.reset();
    testLogger.addAppender(testAppender);
    testAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    testAppender.start();
    // A same-thread executor makes indexOntology's async task and callback run synchronously,
    // so tests can assert on their effects without sleeps or teardown races.
    executorService = MoreExecutors.newDirectExecutorService();
    service = new OntologyService(ontologyDAO, indexService, executorService);
  }

  @AfterEach
  void tearDown() {
    executorService.shutdown();
  }

  @Test
  void testTranslateDataUseSummary() {
    // Translations are more fully tested in TranslationUtilTest, so here we just verify that the
    // summary is populated with expected values based on the input data use.
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    DataUseSummary summary = service.translateDataUseSummary(dataUse);
    assertNotNull(summary);
    assertFalse(summary.getPrimary().isEmpty());
    assertTrue(summary.getPrimary().getFirst().getCode().equalsIgnoreCase("GRU"));
    assertTrue(
        summary.getPrimary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.GRU));
  }

  @ParameterizedTest
  @EnumSource(DataUseTranslationType.class)
  void testTranslateDataUse(DataUseTranslationType type) {
    OntologyTerm term = new OntologyTerm("DOID_1", "v1", "DOID");
    term.setLabel("cancerophobia");
    DataUse dataUse =
        new DataUseBuilder()
            .setHmbResearch(true)
            .setDiseaseRestrictions(List.of(term.id()))
            .build();
    String responseJson = gson.toJson(List.of(term));
    when(ontologyDAO.findByTermIds(new String[] {term.id()}))
        .thenReturn(output -> output.write(responseJson.getBytes(StandardCharsets.UTF_8)));

    String translation = service.translateDataUse(dataUse, type);
    assertNotNull(translation);
    switch (type) {
      case DATASET -> assertTrue(translation.contains(TranslationUtil.DATASET_HEADER));
      case PURPOSE -> assertTrue(translation.contains(TranslationUtil.PURPOSE_HEADER));
    }
    assertTrue(translation.contains(HMB));
    assertTrue(translation.contains(DS.formatted(term.label())));
  }

  @ParameterizedTest
  @EnumSource(DataUseTranslationType.class)
  void testTranslateDataUseOntologyError(DataUseTranslationType type) {
    DataUse dataUse =
        new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(List.of("")).build();
    when(ontologyDAO.findByTermIds(any()))
        .thenThrow(new RuntimeException("Ontology service error"));

    String translation = service.translateDataUse(dataUse, type);
    assertNotNull(translation);
    switch (type) {
      case DATASET -> assertTrue(translation.contains(TranslationUtil.DATASET_HEADER));
      case PURPOSE -> assertTrue(translation.contains(TranslationUtil.PURPOSE_HEADER));
    }
    assertTrue(translation.contains(HMB));
    assertFalse(translation.contains(DS.formatted("")));
  }

  @Test
  void testTranslateNullType() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(false).build();
    assertThrows(IllegalArgumentException.class, () -> service.translateDataUse(dataUse, null));
  }

  @Test
  void testDeleteOntologyTerms() {
    doNothing().when(ontologyDAO).deleteByOntology(OntologyType.DUO.name());
    assertDoesNotThrow(() -> service.deleteOntologyTerms(OntologyType.DUO));
  }

  @Test
  void testIndexOntology() {
    User user = new User();
    user.setUserId(1);
    assertDoesNotThrow(() -> service.indexOntology(user, OntologyType.DUO));
  }

  @Test
  void testIndexOntologyFailure() throws OWLOntologyCreationException {
    User user = new User();
    user.setUserId(1);
    String message = "Failed to generate terms";
    when(indexService.generateTerms(OntologyType.DUO)).thenThrow(new RuntimeException(message));

    service.indexOntology(user, OntologyType.DUO);
    // Verify that the failure was logged
    assertEquals(1, testAppender.getSize());
    ILoggingEvent event = testAppender.getLoggedEvents().getFirst();
    assertThat(event.getFormattedMessage(), containsString(message));
  }

  @Test
  void testFindByTermIds() throws Exception {
    String[] termIds = new String[] {"DUO_0000006", "DUO_0000007"};
    String json =
        """
          [{"id":"DUO_0000006"}, {"id":"DUO_0000007"}]
        """;
    when(ontologyDAO.findByTermIds(termIds))
        .thenReturn(output -> output.write(json.getBytes(StandardCharsets.UTF_8)));
    StreamingOutput results = service.findByTermIds(termIds);
    assertNotNull(results);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(results);
    assertEquals(2, jsonArray.size());
  }

  @Test
  void testFindByQuery() throws Exception {
    String term = "data use modifier";
    OntologyType type = OntologyType.DUO;
    Integer count = 5;
    String json =
        """
          [{"id":"DUO_0000006"}, {"id":"DUO_0000007"}, {"id":"DUO_0000008"}, {"id":"DUO_0000009"}, {"id":"DUO_0000010"}]
        """;
    when(ontologyDAO.findByQuery(term, type, count))
        .thenReturn(output -> output.write(json.getBytes(StandardCharsets.UTF_8)));
    StreamingOutput results = service.findByQuery(term, type, count);
    assertNotNull(results);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(results);
    assertEquals(5, jsonArray.size());
  }
}
