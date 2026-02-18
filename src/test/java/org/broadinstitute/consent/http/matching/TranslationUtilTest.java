package org.broadinstitute.consent.http.matching;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
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
    String translation = service.translate(dataUse, DataUseTranslationType.DATASET);
    assertNotNull(translation);
    assertTrue(translation.contains(term.label()));
    assertTrue(translation.contains("[DS]"));
  }

  @Test
  void testTranslateDataset() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    String translation = service.translate(dataUse, DataUseTranslationType.DATASET);
    assertNotNull(translation);
    assertTrue(translation.contains(TranslationUtil.DATASET_HEADER));
    assertTrue(translation.contains("[GRU]"));
  }

  @Test
  void testTranslatePurpose() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    String translation = service.translate(dataUse, DataUseTranslationType.PURPOSE);
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
    String translation = service.translate(dataUse, DataUseTranslationType.PURPOSE);
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
    String translation = service.translate(dataUse, DataUseTranslationType.PURPOSE);
    assertNotNull(translation);
    assertFalse(translation.contains("[GRU]"));
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

  @Test
  void testTranslateNullType() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(false).build();
    assertThrows(IllegalArgumentException.class, () -> service.translate(dataUse, null));
  }

  @Test
  void testTranslateSummaryNull() {
    DataUseSummary summary = service.translateSummary(null);
    assertNotNull(summary);
    assertNull(summary.getPrimary());
    assertNull(summary.getSecondary());
  }

  @Test
  void testTranslateSummaryGRU() {
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertFalse(summary.getPrimary().isEmpty());
    assertTrue(summary.getPrimary().getFirst().getCode().equalsIgnoreCase("GRU"));
    assertTrue(
        summary.getPrimary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.GRU));
  }

  @Test
  void testTranslateSummaryDS() {
    String cancer = "Cancer";
    DataUse dataUse = new DataUseBuilder().setDiseaseRestrictions(List.of(cancer)).build();
    OntologyTerm term = new OntologyTerm("DOID_1", "v1", "term definition");
    term.setLabel(cancer);
    when(ontologyDAO.findByTermIds(new String[] {cancer}))
        .thenReturn(
            output -> output.write(gson.toJson(List.of(term)).getBytes(StandardCharsets.UTF_8)));
    DataUseSummary summary = service.translateSummary(dataUse);
    assertFalse(summary.getPrimary().isEmpty());
    assertTrue(summary.getPrimary().getFirst().getCode().equalsIgnoreCase("DS"));
    assertTrue(
        summary
            .getPrimary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.DS.formatted(cancer)));
  }

  @Test
  void testTranslateSummaryHMB() {
    DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertFalse(summary.getPrimary().isEmpty());
    assertTrue(summary.getPrimary().getFirst().getCode().equalsIgnoreCase("HMB"));
    assertTrue(
        summary.getPrimary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.HMB));
  }

  @Test
  void testTranslateSummaryPOA() {
    DataUse dataUse = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertFalse(summary.getPrimary().isEmpty());
    assertTrue(summary.getPrimary().getFirst().getCode().equalsIgnoreCase("NPOA"));
    assertTrue(
        summary.getPrimary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.POA));
  }

  @Test
  void testTranslateSummaryOther() {
    DataUse dataUse = new DataUseBuilder().setOther("Other").build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertFalse(summary.getPrimary().isEmpty());
    assertTrue(summary.getPrimary().getFirst().getCode().equalsIgnoreCase("OTHER"));
    assertTrue(
        summary
            .getPrimary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.OTHER.formatted("Other")));
  }

  @Test
  void testTranslateSummaryNMDS() {
    DataUse dataUse = new DataUseBuilder().setMethodsResearch(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("NMDS"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.NMDS));
  }

  @Test
  void testTranslateSummaryNCU() {
    DataUse dataUse = new DataUseBuilder().setNonProfitUse(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("NCU"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.NCU));
  }

  @Test
  void testTranslateSummarySecondaryOther() {
    DataUse dataUse = new DataUseBuilder().setSecondaryOther("Secondary other").build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("OTHER"));
    assertTrue(
        summary
            .getSecondary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.SECONDARY_OTHER.formatted("Secondary other")));
  }

  @Test
  void testTranslateSummaryIRB() {
    DataUse dataUse = new DataUseBuilder().setEthicsApprovalRequired(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("IRB"));
    assertTrue(
        summary
            .getSecondary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.ETHICS_APPROVAL));
  }

  @Test
  void testTranslateSummaryCollaborationRequired() {
    DataUse dataUse = new DataUseBuilder().setCollaboratorRequired(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("COL"));
    assertTrue(
        summary
            .getSecondary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.COLLABORATION_REQUIRED));
  }

  @Test
  void testTranslateSummaryGeoGraphicalRestrictions() {
    DataUse dataUse = new DataUseBuilder().setGeographicalRestrictions("BOSTON").build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("GS"));
    assertTrue(
        summary
            .getSecondary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.GEO_RESTRICTION.formatted("BOSTON")));
  }

  @Test
  void testTranslateSummaryGSO() {
    DataUse dataUse = new DataUseBuilder().setGeneticStudiesOnly(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("GSO"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.GSO));
  }

  @Test
  void testTranslateSummaryPUB() {
    DataUse dataUse = new DataUseBuilder().setPublicationResults(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("PUB"));
    assertTrue(
        summary
            .getSecondary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.PUB_REQUIRED));
  }

  @Test
  void testTranslateSummaryPubMoratorium() {
    DataUse dataUse = new DataUseBuilder().setPublicationMoratorium("Moratorium").build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("MOR"));
    assertTrue(
        summary
            .getSecondary()
            .getFirst()
            .getDescription()
            .equalsIgnoreCase(TranslationUtil.PUB_MORATORIUM.formatted("Moratorium")));
  }

  @Test
  void testTranslateSummaryNCTRL() {
    DataUse dataUse = new DataUseBuilder().setControl(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("NCTRL"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.NCTRL));
  }

  @Test
  void testTranslateSummaryPOP_M() {
    DataUse dataUse = new DataUseBuilder().setGender(TranslationUtil.MALE).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("POP-M"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.RS_M));
  }

  @Test
  void testTranslateSummaryPOP_F() {
    DataUse dataUse = new DataUseBuilder().setGender(TranslationUtil.FEMALE).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("POP-F"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.RS_FM));
  }

  @Test
  void testTranslateSummaryPOP_PD() {
    DataUse dataUse = new DataUseBuilder().setPediatric(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("POP-PD"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.RS_PD));
  }

  @Test
  void testTranslateSummaryPOP() {
    DataUse dataUse = new DataUseBuilder().setPopulation(true).build();
    DataUseSummary summary = service.translateSummary(dataUse);
    assertTrue(summary.getPrimary().isEmpty());
    assertFalse(summary.getSecondary().isEmpty());
    assertTrue(summary.getSecondary().getFirst().getCode().equalsIgnoreCase("POP"));
    assertTrue(
        summary.getSecondary().getFirst().getDescription().equalsIgnoreCase(TranslationUtil.POP));
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
