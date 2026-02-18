package org.broadinstitute.consent.http.matching;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.service.ontology.OntologyDAO;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranslationUtilTest extends AbstractTestHelper {

  private TranslationUtil service;
  private final Gson gson = GsonUtil.getInstance();

  @Mock private OntologyDAO ontologyDAO;

  @BeforeEach
  void setUpClass() {
    service = new TranslationUtil(ontologyDAO);
  }

  @Test
  void testDiseaseLookup() {
    OntologyTerm term = initializeDiseaseTerm();
    DataUse dataUse = new DataUseBuilder().setDiseaseRestrictions(List.of(term.id())).build();
    String dataUseJson = gson.toJson(dataUse);
    String translation = service.translateDataset(dataUseJson);
    assertNotNull(translation);
    assertTrue(translation.contains(term.label()));
    assertTrue(translation.contains("[DS]"));
  }

  @Test
  void testTranslateDataset() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    String datasetString = gson.toJson(dataUse);
    String translation = service.translateDataset(datasetString);
    assertNotNull(translation);
    assertTrue(translation.contains(TranslationUtil.DATASET_HEADER));
    assertTrue(translation.contains("[GRU]"));
  }

  @Test
  void testTranslatePurpose() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    String datasetString = gson.toJson(dataUse);
    String translation = service.translatePurpose(datasetString);
    assertNotNull(translation);
    assertTrue(translation.contains(TranslationUtil.PURPOSE_HEADER));
    assertTrue(translation.contains("[GRU]"));
  }

  @Test
  void testTranslateCoverageTrue() {
    OntologyTerm term = initializeDiseaseTerm();
    DataUse dataUse =
        new DataUseBuilder()
            .setGeneralUse(true)
            .setHmbResearch(true)
            .setDiseaseRestrictions(List.of(term.id()))
            .setMethodsResearch(true)
            .setControl(true)
            .setNonProfitUse(true)
            .setGender(TranslationUtil.MALE)
            .setPediatric(true)
            .setEthicsApprovalRequired(true)
            .setCollaboratorRequired(true)
            .setIllegalBehavior(true)
            .setNotHealth(true)
            .setOther("Other")
            .setSecondaryOther("Secondary other")
            .setGeographicalRestrictions("Geographical restriction")
            .setGeneticStudiesOnly(true)
            .setPublicationResults(true)
            .setPublicationMoratorium("2025-01-01")
            .setPopulationOriginsAncestry(true)
            .setPopulation(true)
            .setPsychologicalTraits(true)
            .setSexualDiseases(true)
            .setStigmatizeDiseases(true)
            .setVulnerablePopulations(true)
            .build();
    String datasetString = gson.toJson(dataUse);
    String translation = service.translatePurpose(datasetString);
    assertNotNull(translation);
    assertTrue(translation.contains("[GRU]"));
  }

  @Test
  void testTranslateCoverageFalse() {
    OntologyTerm term = initializeDiseaseTerm();
    DataUse dataUse =
        new DataUseBuilder()
            .setGeneralUse(false)
            .setHmbResearch(false)
            .setDiseaseRestrictions(List.of(term.id()))
            .setMethodsResearch(false)
            .setControl(true)
            .setNonProfitUse(false)
            .setGender(TranslationUtil.FEMALE)
            .setPediatric(false)
            .setEthicsApprovalRequired(false)
            .setCollaboratorRequired(false)
            .setIllegalBehavior(false)
            .setNotHealth(false)
            .setOther("Other")
            .setSecondaryOther("Secondary other")
            .setGeographicalRestrictions("Geographical restriction")
            .setGeneticStudiesOnly(false)
            .setPublicationResults(false)
            .setPublicationMoratorium("2025-01-01")
            .setPopulationOriginsAncestry(false)
            .setPopulation(false)
            .setPsychologicalTraits(false)
            .setSexualDiseases(false)
            .setStigmatizeDiseases(false)
            .setVulnerablePopulations(false)
            .build();
    String datasetString = gson.toJson(dataUse);
    String translation = service.translatePurpose(datasetString);
    assertNotNull(translation);
    assertFalse(translation.contains("[GRU]"));
  }

  @Test
  void tesTranslateDatasetInvalidJson() {
    String invalidJson = "{invalid json}";
    assertThrows(IllegalArgumentException.class, () -> service.translateDataset(invalidJson));
  }

  @Test
  void tesTranslatePurposeInvalidJson() {
    String invalidJson = "{invalid json}";
    assertThrows(IllegalArgumentException.class, () -> service.translatePurpose(invalidJson));
  }

  @ParameterizedTest
  @EnumSource(DataUseTranslationType.class)
  void testTranslateNullDataUse(DataUseTranslationType type) {
    String translation = service.translate(null, type);
    assertNotNull(translation);
  }

  @ParameterizedTest
  @EnumSource(DataUseTranslationType.class)
  void testTranslateNullDiseases(DataUseTranslationType type) {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(false).build();
    dataUse.setDiseaseRestrictions(null);
    String translation = service.translate(dataUse, type);
    assertNotNull(translation);
  }

  @ParameterizedTest
  @EnumSource(DataUseTranslationType.class)
  void testTranslateEmptyDiseases(DataUseTranslationType type) {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(false).build();
    dataUse.setDiseaseRestrictions(List.of());
    String translation = service.translate(dataUse, type);
    assertNotNull(translation);
  }

  @Test
  void testFindTermsByIdsError() {
    when(ontologyDAO.findByTermIds(any()))
        .thenThrow(new RuntimeException("Ontology service error"));
    List<OntologyTerm> terms = service.findTermsByIds(List.of("DOID_1"));
    assertNotNull(terms);
    assertTrue(terms.isEmpty());
  }

  private OntologyTerm initializeDiseaseTerm() {
    OntologyTerm term =
        new OntologyTerm("DOID_" + randomInt(1, 100), "term label", "term definition");
    // Label is the value we use for a disease translation.
    term.setLabel(term.id());
    String json = GsonUtil.getInstance().toJson(List.of(term));
    when(ontologyDAO.findByTermIds(new String[] {term.id()}))
        .thenReturn(output -> output.write(json.getBytes(StandardCharsets.UTF_8)));
    return term;
  }
}
