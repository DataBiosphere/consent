package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class DataAccessRequestDataTest {

  @Test
  void serialization() {
    String exampleDarData =
        """
        {
          "institution": "The Broad Institute of MIT and Harvard",
          "projectTitle": "title",
          "checkCollaborator": false,
          "checkNihDataOnly": false
        }""";
    DataAccessRequestData resultingDarData = DataAccessRequestData.fromString(exampleDarData);
    String expectedDarData =
        """
        {"projectTitle":"title","checkNihDataOnly":false}""";
    // does not include fields removed from the object (ex. checkCollaborator, institution)
    assertEquals(expectedDarData, resultingDarData.toString());
    JSONAssert.assertEquals(expectedDarData, resultingDarData.toString(), false);
  }

  @Test
  void testGetSetItDirectorEmail() {
    DataAccessRequestData data = new DataAccessRequestData();

    assertNull(data.getItDirectorEmail());

    String testEmail = "it@example.broadinstitute.org";
    data.setItDirectorEmail(testEmail);

    assertEquals(testEmail, data.getItDirectorEmail());
  }

  @Test
  void testGetSetPiEmail() {
    DataAccessRequestData data = new DataAccessRequestData();

    assertNull(data.getPiEmail());

    String testEmail = "pi@example.broadinstitute.org";
    data.setPiEmail(testEmail);

    assertEquals(testEmail, data.getPiEmail());
  }

  @Test
  void testGetSetSigningOfficialEmail() {
    DataAccessRequestData data = new DataAccessRequestData();

    assertNull(data.getSigningOfficialEmail());

    String testEmail = "so@example.broadinstitute.org";
    data.setSigningOfficialEmail(testEmail);

    assertEquals(testEmail, data.getSigningOfficialEmail());
  }

  @Test
  void testPopulateDARDataWithValidJson() {
    String validJson =
        """
            {
                "projectTitle": "Test Project",
                "checkNihDataOnly": true
            }
        """;
    DataAccessRequestData result = DataAccessRequestData.populateDARData(validJson);
    assertNotNull(result);
    assertEquals("Test Project", result.getProjectTitle());
    assertTrue(result.getCheckNihDataOnly());
  }

  @Test
  void testPopulateDARDataWithInvalidJson() {
    String invalidJson =
        """
            {
                "projectTitle": "Test Project",
                "checkNihDataOnly": true,
        """;
    assertThrows(
        BadRequestException.class, () -> DataAccessRequestData.populateDARData(invalidJson));
  }

  @Test
  void testPopulateDARDataWithNullJson() {
    DataAccessRequestData result = DataAccessRequestData.populateDARData(null);
    assertNotNull(result);
    assertNull(result.getProjectTitle());
    assertNull(result.getCheckNihDataOnly());
  }

  // String fields

  @Test
  void testGetSetRus() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getRus());
    data.setRus("rus text");
    assertEquals("rus text", data.getRus());
  }

  @Test
  void testGetSetNonTechRus() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getNonTechRus());
    data.setNonTechRus("non-tech rus text");
    assertEquals("non-tech rus text", data.getNonTechRus());
  }

  @Test
  void testGetSetOtherText() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getOtherText());
    data.setOtherText("other");
    assertEquals("other", data.getOtherText());
  }

  @Test
  void testGetSetGender() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getGender());
    data.setGender("F");
    assertEquals("F", data.getGender());
  }

  @Test
  void testGetSetStatus() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getStatus());
    data.setStatus("Approved");
    assertEquals("Approved", data.getStatus());
  }

  @Test
  void testGetSetCloudProvider() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCloudProvider());
    data.setCloudProvider("AWS");
    assertEquals("AWS", data.getCloudProvider());
  }

  @Test
  void testGetSetCloudProviderType() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCloudProviderType());
    data.setCloudProviderType("Public");
    assertEquals("Public", data.getCloudProviderType());
  }

  @Test
  void testGetSetCloudProviderDescription() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCloudProviderDescription());
    data.setCloudProviderDescription("S3 bucket");
    assertEquals("S3 bucket", data.getCloudProviderDescription());
  }

  @Test
  void testGetSetIrbDocumentLocation() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getIrbDocumentLocation());
    data.setIrbDocumentLocation("gs://bucket/irb.pdf");
    assertEquals("gs://bucket/irb.pdf", data.getIrbDocumentLocation());
  }

  @Test
  void testGetSetIrbDocumentName() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getIrbDocumentName());
    data.setIrbDocumentName("irb.pdf");
    assertEquals("irb.pdf", data.getIrbDocumentName());
  }

  @Test
  void testGetSetIrbProtocolExpiration() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getIrbProtocolExpiration());
    data.setIrbProtocolExpiration("2027-01-01");
    assertEquals("2027-01-01", data.getIrbProtocolExpiration());
  }

  @Test
  void testGetSetItDirector() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getItDirector());
    data.setItDirector("IT Director Name");
    assertEquals("IT Director Name", data.getItDirector());
  }

  @Test
  void testGetSetSigningOfficial() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getSigningOfficial());
    data.setSigningOfficial("Signing Official Name");
    assertEquals("Signing Official Name", data.getSigningOfficial());
  }

  @Test
  void testGetSetCollaborationLetterLocation() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCollaborationLetterLocation());
    data.setCollaborationLetterLocation("gs://bucket/letter.pdf");
    assertEquals("gs://bucket/letter.pdf", data.getCollaborationLetterLocation());
  }

  @Test
  void testGetSetCollaborationLetterName() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCollaborationLetterName());
    data.setCollaborationLetterName("letter.pdf");
    assertEquals("letter.pdf", data.getCollaborationLetterName());
  }

  @Test
  void testGetSetPiName() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPiName());
    data.setPiName("PI Name");
    assertEquals("PI Name", data.getPiName());
  }

  @Test
  void testGetSetPiCountryOfOperation() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPiCountryOfOperation());
    data.setPiCountryOfOperation("United States");
    assertEquals("United States", data.getPiCountryOfOperation());
  }

  @Test
  void testGetSetProgressReportSummary() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getProgressReportSummary());
    data.setProgressReportSummary("Summary text");
    assertEquals("Summary text", data.getProgressReportSummary());
  }

  @Test
  void testGetSetResearchPlans() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getResearchPlans());
    data.setResearchPlans("Research plan text");
    assertEquals("Research plan text", data.getResearchPlans());
  }

  // Boolean fields

  @Test
  void testGetSetDiseases() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getDiseases());
    data.setDiseases(true);
    assertTrue(data.getDiseases());
  }

  @Test
  void testGetSetMethods() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getMethods());
    data.setMethods(true);
    assertTrue(data.getMethods());
  }

  @Test
  void testGetSetAiLlmUse() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getAiLlmUse());
    data.setAiLlmUse(true);
    assertTrue(data.getAiLlmUse());
  }

  @Test
  void testGetSetControls() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getControls());
    data.setControls(true);
    assertTrue(data.getControls());
  }

  @Test
  void testGetSetPopulation() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPopulation());
    data.setPopulation(true);
    assertTrue(data.getPopulation());
  }

  @Test
  void testGetSetOther() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getOther());
    data.setOther(true);
    assertTrue(data.getOther());
  }

  @Test
  void testGetSetForProfit() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getForProfit());
    data.setForProfit(true);
    assertTrue(data.getForProfit());
  }

  @Test
  void testGetSetOneGender() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getOneGender());
    data.setOneGender(true);
    assertTrue(data.getOneGender());
  }

  @Test
  void testGetSetPediatric() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPediatric());
    data.setPediatric(true);
    assertTrue(data.getPediatric());
  }

  @Test
  void testGetSetIllegalBehavior() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getIllegalBehavior());
    data.setIllegalBehavior(true);
    assertTrue(data.getIllegalBehavior());
  }

  @Test
  void testGetSetAddiction() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getAddiction());
    data.setAddiction(true);
    assertTrue(data.getAddiction());
  }

  @Test
  void testGetSetSexualDiseases() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getSexualDiseases());
    data.setSexualDiseases(true);
    assertTrue(data.getSexualDiseases());
  }

  @Test
  void testGetSetStigmatizedDiseases() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getStigmatizedDiseases());
    data.setStigmatizedDiseases(true);
    assertTrue(data.getStigmatizedDiseases());
  }

  @Test
  void testGetSetVulnerablePopulation() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getVulnerablePopulation());
    data.setVulnerablePopulation(true);
    assertTrue(data.getVulnerablePopulation());
  }

  @Test
  void testGetSetPopulationMigration() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPopulationMigration());
    data.setPopulationMigration(true);
    assertTrue(data.getPopulationMigration());
  }

  @Test
  void testGetSetPsychiatricTraits() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPsychiatricTraits());
    data.setPsychiatricTraits(true);
    assertTrue(data.getPsychiatricTraits());
  }

  @Test
  void testGetSetNotHealth() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getNotHealth());
    data.setNotHealth(true);
    assertTrue(data.getNotHealth());
  }

  @Test
  void testGetSetHmb() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getHmb());
    data.setHmb(true);
    assertTrue(data.getHmb());
  }

  @Test
  void testGetSetPoa() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPoa());
    data.setPoa(true);
    assertTrue(data.getPoa());
  }

  @Test
  void testGetSetAnvilUse() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getAnvilUse());
    data.setAnvilUse(true);
    assertTrue(data.getAnvilUse());
  }

  @Test
  void testGetSetCloudUse() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCloudUse());
    data.setCloudUse(true);
    assertTrue(data.getCloudUse());
  }

  @Test
  void testGetSetLocalUse() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getLocalUse());
    data.setLocalUse(true);
    assertTrue(data.getLocalUse());
  }

  @Test
  void testGetSetGeneticStudiesOnly() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getGeneticStudiesOnly());
    data.setGeneticStudiesOnly(true);
    assertTrue(data.getGeneticStudiesOnly());
  }

  @Test
  void testGetSetIrb() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getIrb());
    data.setIrb(true);
    assertTrue(data.getIrb());
  }

  @Test
  void testGetSetPublication() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPublication());
    data.setPublication(true);
    assertTrue(data.getPublication());
  }

  @Test
  void testGetSetCollaboration() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCollaboration());
    data.setCollaboration(true);
    assertTrue(data.getCollaboration());
  }

  @Test
  void testGetSetForensicActivities() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getForensicActivities());
    data.setForensicActivities(true);
    assertTrue(data.getForensicActivities());
  }

  @Test
  void testGetSetSharingDistribution() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getSharingDistribution());
    data.setSharingDistribution(true);
    assertTrue(data.getSharingDistribution());
  }

  @Test
  void testGetSetDsAcknowledgement() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getDSAcknowledgement());
    data.setDSAcknowledgement(true);
    assertTrue(data.getDSAcknowledgement());
  }

  @Test
  void testGetSetGsoAcknowledgement() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getGSOAcknowledgement());
    data.setGSOAcknowledgement(true);
    assertTrue(data.getGSOAcknowledgement());
  }

  @Test
  void testGetSetPubAcknowledgement() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPubAcknowledgement());
    data.setPubAcknowledgement(true);
    assertTrue(data.getPubAcknowledgement());
  }

  // List and complex object fields

  @Test
  void testGetOntologies_returnsEmptyListWhenNull() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertTrue(data.getOntologies().isEmpty());
  }

  @Test
  void testGetSetOntologies() {
    DataAccessRequestData data = new DataAccessRequestData();
    List<OntologyEntry> ontologies = List.of(new OntologyEntry());
    data.setOntologies(ontologies);
    assertEquals(ontologies, data.getOntologies());
  }

  @Test
  void testGetLabCollaborators_returnsEmptyListWhenNull() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertTrue(data.getLabCollaborators().isEmpty());
  }

  @Test
  void testGetSetLabCollaborators() {
    DataAccessRequestData data = new DataAccessRequestData();
    List<Collaborator> collaborators =
        List.of(new Collaborator(null, "lab@example.com", null, null, null, null, null));
    data.setLabCollaborators(collaborators);
    assertEquals(collaborators, data.getLabCollaborators());
  }

  @Test
  void testGetInternalCollaborators_returnsEmptyListWhenNull() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertTrue(data.getInternalCollaborators().isEmpty());
  }

  @Test
  void testGetSetInternalCollaborators() {
    DataAccessRequestData data = new DataAccessRequestData();
    List<Collaborator> collaborators =
        List.of(new Collaborator(null, "internal@example.com", null, null, null, null, null));
    data.setInternalCollaborators(collaborators);
    assertEquals(collaborators, data.getInternalCollaborators());
  }

  @Test
  void testGetExternalCollaborators_returnsEmptyListWhenNull() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertTrue(data.getExternalCollaborators().isEmpty());
  }

  @Test
  void testGetSetExternalCollaborators() {
    DataAccessRequestData data = new DataAccessRequestData();
    List<Collaborator> collaborators =
        List.of(new Collaborator(null, "external@example.com", null, null, null, null, null));
    data.setExternalCollaborators(collaborators);
    assertEquals(collaborators, data.getExternalCollaborators());
  }

  @Test
  void testGetLabAndInternalCollaborators() {
    DataAccessRequestData data = new DataAccessRequestData();
    Collaborator lab = new Collaborator(null, "lab@example.com", null, null, null, null, null);
    Collaborator internal =
        new Collaborator(null, "internal@example.com", null, null, null, null, null);
    data.setLabCollaborators(List.of(lab));
    data.setInternalCollaborators(List.of(internal));
    List<Collaborator> combined = data.getLabAndInternalCollaborators();
    assertEquals(2, combined.size());
    assertTrue(combined.contains(lab));
    assertTrue(combined.contains(internal));
  }

  @Test
  void testGetSetIntellectualProperties() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getIntellectualProperties());
    List<IntellectualProperty> ips =
        List.of(
            new IntellectualProperty(
                null, null, null, null, null, null, null, null, null, null, null));
    data.setIntellectualProperties(ips);
    assertEquals(ips, data.getIntellectualProperties());
  }

  @Test
  void testGetSetPublications() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPublications());
    List<Publication> publications =
        List.of(
            new Publication(
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null));
    data.setPublications(publications);
    assertEquals(publications, data.getPublications());
  }

  @Test
  void testGetSetPresentations() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getPresentations());
    List<Presentation> presentations =
        List.of(
            new Presentation(
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null));
    data.setPresentations(presentations);
    assertEquals(presentations, data.getPresentations());
  }

  @Test
  void testGetSetDmi() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getDmi());
    DataManagementIncident dmi = new DataManagementIncident(null, "incident description");
    data.setDmi(dmi);
    assertEquals(dmi, data.getDmi());
  }

  @Test
  void testGetSetCloseoutSupplement() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getCloseoutSupplement());
    CloseoutSupplement supplement = new CloseoutSupplement(null, "other text", 1);
    data.setCloseoutSupplement(supplement);
    assertEquals(supplement, data.getCloseoutSupplement());
  }

  @Test
  void testGetSetDaaIds() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertNull(data.getDaaIds());
    List<Integer> daaIds = List.of(1, 2, 3);
    data.setDaaIds(daaIds);
    assertEquals(daaIds, data.getDaaIds());
  }

  @Test
  void testGetDatasetIds_returnsEmptyListWhenNull() {
    DataAccessRequestData data = new DataAccessRequestData();
    assertTrue(data.getDatasetIds().isEmpty());
  }
}
