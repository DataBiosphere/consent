package org.broadinstitute.consent.http.models;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.BadRequestException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataAccessRequestTest {

  @Test
  void populateProgressReportFromJsonString() {
    DataAccessRequest parentDar = new DataAccessRequest();
    parentDar.setId(1);
    parentDar.setReferenceId("parent-reference-id");
    parentDar.setCollectionId(100);
    DataAccessRequestData parentData = new DataAccessRequestData();
    parentData.setProjectTitle("Parent Project Title");
    Collaborator collaborator = new Collaborator();
    collaborator.setName("Parent Collaborator");
    parentData.setInternalCollaborators(List.of(collaborator));
    parentData.setExternalCollaborators(List.of(collaborator));
    parentData.setLabCollaborators(List.of(collaborator));
    parentDar.setDatasetIds(List.of(1, 2, 3));
    parentData.setCollaborationLetterName("collaboration_letter.txt");
    parentData.setIrbDocumentName("irb_document.txt");
    parentData.setCollaborationLetterLocation("collaboration_letter_location");
    parentData.setIrbDocumentLocation("irb_document_location");
    parentData.setDSAcknowledgement(false);
    parentData.setGSOAcknowledgement(false);
    parentData.setPubAcknowledgement(false);
    parentDar.setData(parentData);

    String json = """
            {
                "projectTitle": "New Project Title",
                "internalCollaborators": [],
                "externalCollaborators": [],
                "labCollaborators": [],
                "progressReportSummary": "New Summary",
                "datasetIds": [1, 2],
                "collaborationLetterName": "new_collaboration_letter.txt",
                "irbDocumentName": "new_irb_document.txt",
                "collaborationLetterLocation": "new_collaboration_letter_location",
                "irbDocumentLocation": "new_irb_document_location",
                "dsAcknowledgement": true,
                "gsoAcknowledgement": true
            }
        """;

    DataAccessRequest newDar = DataAccessRequest.populateProgressReportFromJsonString(json, parentDar);
    DataAccessRequestData newData = newDar.getData();

    assertNotNull(newDar);
    assertNotEquals(parentDar.getReferenceId(), newDar.getReferenceId());
    assertEquals(parentDar.getCollectionId(), newDar.getCollectionId());
    assertEquals("Parent Project Title", newData.getProjectTitle());
    assertEquals(List.of(), newData.getInternalCollaborators());
    assertEquals(List.of(), newData.getExternalCollaborators());
    assertEquals(List.of(), newData.getLabCollaborators());
    assertEquals("New Summary", newData.getProgressReportSummary());
    assertEquals(List.of(1, 2), newDar.getDatasetIds());
    assertNull(newData.getCollaborationLetterName());
    assertNull(newData.getIrbDocumentName());
    assertNull(newData.getCollaborationLetterLocation());
    assertNull(newData.getIrbDocumentLocation());
    assertEquals(List.of(collaborator),
        parentDar.getData().getInternalCollaborators()); // Ensure parent is unchanged
    assertEquals("collaboration_letter.txt", parentDar.getData().getCollaborationLetterName());
    assertTrue(newData.getDSAcknowledgement());
    assertTrue(newData.getGSOAcknowledgement());
    assertNull(newData.getPubAcknowledgement());
  }

  @Test
  void testIsCloseoutProgressReport_False() {
    DataAccessRequest dar = new DataAccessRequest();
    assertFalse(dar.getIsCloseoutProgressReport());
  }

  @Test
  void testIsCloseoutProgressReport_FalseWithoutData() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setParentId(1);
    assertFalse(dar.getIsCloseoutProgressReport());
  }

  @Test
  void testIsCloseoutProgressReport_True() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    CloseoutSupplement supplement = new CloseoutSupplement(List.of("yes"), "", 1);
    darData.setCloseoutSupplement(supplement);
    dar.setData(darData);
    dar.setParentId(1);
    assertTrue(dar.getIsCloseoutProgressReport());
  }

  @Test
  void testGetHasCloseoutApproval_False() {
    DataAccessRequest dar = new DataAccessRequest();
    assertFalse(dar.getHasSOCloseoutApproval());
  }

  @Test
  void testGetHasCloseoutApproval_True() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setCloseoutSigningOfficialApprovedUserId(1);
    dar.setCloseoutSigningOfficialApprovedDate(Timestamp.from(Instant.now()));
    assertTrue(dar.getHasSOCloseoutApproval());
  }

  @Test
  void testValidateCloseoutSupplement_Unset() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setCloseoutSupplement(null);
    dar.setData(darData);
    assertDoesNotThrow(() -> DataAccessRequest.validateCloseoutSupplement(dar.getData().getCloseoutSupplement()));
  }

  @Test
  void testValidateCloseoutApprovalThrowsExceptionWithEmptyReasonsOtherTextSigningOfficial() {
    CloseoutSupplement supplement = new CloseoutSupplement(List.of(), "", null);
    BadRequestException exception = assertThrows(BadRequestException.class, () -> DataAccessRequest.validateCloseoutSupplement(supplement));
    assertThat(exception.getMessage(), containsString("A closeout supplement must have values provided."));
  }

  @Test
  void testValidateCloseoutApprovalThrowsExceptionWithEmptyReasons() {
    CloseoutSupplement supplement = new CloseoutSupplement(List.of(), "", 1);
    BadRequestException exception = assertThrows(BadRequestException.class, () -> DataAccessRequest.validateCloseoutSupplement(supplement));
    assertThat(exception.getMessage(), containsString("A closeout supplement must have reasons provided."));
  }

  @Test
  void testValidateCloseoutApprovalThrowsExceptionWithNullReasons() {
    CloseoutSupplement supplement = new CloseoutSupplement(null, "", 1);
    BadRequestException exception = assertThrows(BadRequestException.class, () -> DataAccessRequest.validateCloseoutSupplement(supplement));
    assertThat(exception.getMessage(), containsString("A closeout supplement must have reasons provided."));
  }

  @Test
  void testValidateCloseoutApprovalThrowsExceptionWithEmptySigningOfficial() {
    CloseoutSupplement supplement = new CloseoutSupplement(List.of("test"), "", null);
    BadRequestException exception = assertThrows(BadRequestException.class, () -> DataAccessRequest.validateCloseoutSupplement(supplement));
    assertThat(exception.getMessage(), containsString("A closeout supplement must have a signing official id provided."));
  }
}