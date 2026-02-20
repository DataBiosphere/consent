package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.MockServerTestHelper;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UseRestrictionConverterTest extends MockServerTestHelper {

  @Test
  void testParseDataUsePurposeEmpty() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNull(dataUse.getGeneralUse());
    assertNull(dataUse.getDiseaseRestrictions());
    assertNull(dataUse.getHmbResearch());
    assertNull(dataUse.getPopulationOriginsAncestry());
    assertNull(dataUse.getMethodsResearch());
    assertNull(dataUse.getNonProfitUse());
    assertNull(dataUse.getOther());
    assertNull(dataUse.getSecondaryOther());
    assertNull(dataUse.getEthicsApprovalRequired());
    assertNull(dataUse.getCollaboratorRequired());
    assertNull(dataUse.getGeographicalRestrictions());
    assertNull(dataUse.getGeneticStudiesOnly());
    assertNull(dataUse.getPublicationResults());
    assertNull(dataUse.getPublicationMoratorium());
    assertNull(dataUse.getControls());
    assertNull(dataUse.getGender());
    assertNull(dataUse.getPediatric());
    assertNull(dataUse.getPopulation());
    assertNull(dataUse.getIllegalBehavior());
    assertNull(dataUse.getSexualDiseases());
    assertNull(dataUse.getStigmatizeDiseases());
    assertNull(dataUse.getVulnerablePopulations());
    assertNull(dataUse.getPsychologicalTraits());
    assertNull(dataUse.getNotHealth());
  }

  @Test
  void testParseDataUsePurposeFalseAsNull() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();

    data.setMethods(false);
    data.setPopulation(false);
    data.setControls(false);
    data.setOntologies(List.of());
    data.setForProfit(false);
    data.setGender("");
    data.setPediatric(false);
    data.setIllegalBehavior(false);
    data.setSexualDiseases(false);
    data.setStigmatizedDiseases(false);
    data.setVulnerablePopulation(false);
    data.setPsychiatricTraits(false);
    data.setNotHealth(false);

    dar.setData(data);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNull(dataUse.getGeneralUse());
    assertNull(dataUse.getDiseaseRestrictions());
    assertNull(dataUse.getHmbResearch());
    assertNull(dataUse.getPopulationOriginsAncestry());
    assertNull(dataUse.getMethodsResearch());
    assertNotNull(dataUse.getNonProfitUse());
    assertNull(dataUse.getOther());
    assertNull(dataUse.getSecondaryOther());
    assertNull(dataUse.getEthicsApprovalRequired());
    assertNull(dataUse.getCollaboratorRequired());
    assertNull(dataUse.getGeographicalRestrictions());
    assertNull(dataUse.getGeneticStudiesOnly());
    assertNull(dataUse.getPublicationResults());
    assertNull(dataUse.getPublicationMoratorium());
    assertNull(dataUse.getControls());
    assertNull(dataUse.getGender());
    assertNull(dataUse.getPediatric());
    assertNull(dataUse.getPopulation());
    assertNull(dataUse.getIllegalBehavior());
    assertNull(dataUse.getSexualDiseases());
    assertNull(dataUse.getStigmatizeDiseases());
    assertNull(dataUse.getVulnerablePopulations());
    assertNull(dataUse.getPsychologicalTraits());
    assertNull(dataUse.getNotHealth());
  }

  @Test
  void testParseDataUsePurposeMethods() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setMethods(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getMethodsResearch());
  }

  @Test
  void testParseDataUseAiLlmUse() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setAiLlmUse(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getAiLlmUse());
  }

  @Test
  void testParseDataUsePurposeControls() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setControls(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getControls());
  }

  @Test
  void testParseDataUsePurposeDisease() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    OntologyEntry entry = new OntologyEntry();
    entry.setId("id");
    entry.setDefinition("description");
    entry.setLabel("label");
    dar.getData().setOntologies(List.of(entry));
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNotNull(dataUse.getDiseaseRestrictions());
    assertFalse(dataUse.getDiseaseRestrictions().isEmpty());
  }

  @Test
  void testParseDataUsePurposeNonProfit() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setForProfit(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertFalse(dataUse.getNonProfitUse());
  }

  @Test
  void testParseDataUsePurposeGender() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setOneGender(true);
    dar.getData().setGender("F");
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNotNull(dataUse.getGender());
  }

  @Test
  void testParseDataUsePurposePediatric() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setPediatric(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getPediatric());
  }

  @Test
  void testParseDataUsePurposeHMB() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setHmb(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getHmbResearch());
  }

  @Test
  void testParseDataUsePurposeOther() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setOther(true);
    dar.getData().setOtherText("Other Text");
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertFalse(dataUse.getOther().isEmpty());
  }

  @Test
  void testParseDataUseIllegalBehavior() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setIllegalBehavior(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getIllegalBehavior());
  }

  @Test
  void testParseDataUseSexualDiseases() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setSexualDiseases(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getSexualDiseases());
  }

  @Test
  void testParseDataUseStigmatizeDiseases() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setStigmatizedDiseases(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getStigmatizeDiseases());
  }

  @Test
  void testParseDataUseVulnerablePopulations() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setVulnerablePopulation(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getVulnerablePopulations());
  }

  @Test
  void testParseDataUsePsychologicalTraits() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setPsychiatricTraits(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getPsychologicalTraits());
  }

  @Test
  void testParseDataUseNotHealth() {
    UseRestrictionConverter converter = new UseRestrictionConverter();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setNotHealth(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getNotHealth());
  }

  private DataAccessRequest createDataAccessRequest() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    data.setReferenceId(dar.getReferenceId());
    dar.setData(data);
    return dar;
  }
}
