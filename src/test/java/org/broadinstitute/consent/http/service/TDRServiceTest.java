package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TDRServiceTest {

  @Mock
  private DataAccessRequestService darService;

  @Mock
  private DatasetDAO datasetDAO;

  @Mock
  private UserDAO userDAO;

  @Mock SamDAO samDAO;

  @Mock AuthUser authUser;
  private TDRService service;

  private void initService() {
    service = new TDRService(darService, datasetDAO, samDAO, userDAO);
  }

  @Test
  void testGetApprovedUsersForDataset() {
    Dataset dataset = new Dataset();

    User user1 = new User();
    user1.setUserId(1);
    user1.setEmail("asdf1@gmail.com");
    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(user1.getUserId());
    DataAccessRequestData data = new DataAccessRequestData();
    Collaborator lab = new Collaborator();
    lab.setEmail("lab@gmail.com");
    data.setLabCollaborators(List.of(lab));
    dar1.setData(data);
    User user2 = new User();
    user2.setUserId(2);
    user2.setEmail("asdf2@gmail.com");
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(user2.getUserId());

    when(darService.getApprovedDARsForDataset(dataset)).thenReturn(List.of(dar1, dar2));
    when(userDAO.findUsers(any())).thenReturn(List.of(user1, user2));
    initService();

    ApprovedUsers approvedUsers = service.getApprovedUsersForDataset(authUser, dataset);
    List<String> approvedUsersEmails = approvedUsers.approvedUsers().stream()
        .map(ApprovedUser::email)
        .toList();

    assertTrue(
        approvedUsersEmails.containsAll(List.of(user1.getEmail(), user2.getEmail(), lab.getEmail())));
  }

  @Test
  void testGetApprovedUsersForDatasetEmptyEmails() {
    Dataset dataset = new Dataset();
    User user1 = new User();
    user1.setUserId(1);
    user1.setEmail(" ");
    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(user1.getUserId());
    DataAccessRequestData data = new DataAccessRequestData();
    Collaborator lab = new Collaborator();
    lab.setEmail(" ");
    data.setLabCollaborators(List.of(lab));
    dar1.setData(data);
    User user2 = new User();
    user2.setUserId(2);
    user2.setEmail(" ");
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(user2.getUserId());

    when(darService.getApprovedDARsForDataset(dataset)).thenReturn(List.of(dar1, dar2));
    when(userDAO.findUsers(any())).thenReturn(List.of(user1, user2));

    initService();
    ApprovedUsers approvedUsers = service.getApprovedUsersForDataset(authUser, dataset);
    assertTrue(approvedUsers.approvedUsers().isEmpty());
  }

  @Test
  void testGetApprovedUsersForDatasetNoUsers() {
    Dataset dataset = new Dataset();
    when(darService.getApprovedDARsForDataset(any())).thenReturn(List.of());

    initService();
    ApprovedUsers approvedUsers = service.getApprovedUsersForDataset(authUser, dataset);
    assertTrue(approvedUsers.approvedUsers().isEmpty());
    verify(userDAO, never()).findUsers(any());
  }

  @Test
  void testGetDatasetsByIdentifier() {
    String identifiers = "DUOS-00001, DUOS-00002";
    List<Integer> identifierList = Arrays.stream(identifiers.split(","))
        .map(String::trim)
        .filter(identifier -> !identifier.isBlank())
        .map(Dataset::parseIdentifierToAlias)
        .toList();

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setAlias(00001);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setAlias(00002);

    when(datasetDAO.findDatasetsByAlias(identifierList)).thenReturn(List.of(dataset1, dataset2));

    initService();
    List<Dataset> datasetIds = service.getDatasetsByIdentifier(identifierList);

    assertEquals(datasetIds.size(), identifierList.size());
    assertTrue(datasetIds.containsAll(List.of(dataset1, dataset2)));
  }

  @Test
  void testPopulateDraftDarStubFromDatasetIdentifiers() {
    String identifiers = "DUOS-00001, DUOS-00002";
    String title = "New Project";
    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setAlias(00001);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setAlias(00002);

    when(datasetDAO.findDatasetsByAlias(any())).thenReturn(List.of(dataset1, dataset2));

    initService();
    assertDoesNotThrow(() -> {
      DataAccessRequest dar = service.populateDraftDarStubFromDatasetIdentifiers(identifiers, "New Project");
      assertNotNull(dar);
      assertTrue(dar.getDatasetIds().contains(dataset1.getDatasetId()));
      assertTrue(dar.getDatasetIds().contains(dataset2.getDatasetId()));
      assertEquals(title, dar.getData().getProjectTitle());
      assertNotNull(dar.getReferenceId());
      assertNotNull(dar.getCreateDate());
    });
  }

  @Test
  void testPopulateDraftDarStubFromDatasetIdentifiersNotFound() {
    String identifiers = "DUOS-00001, DUOS-00002";
    initService();
    assertThrows(NotFoundException.class, () -> service.populateDraftDarStubFromDatasetIdentifiers(identifiers, "New Project"));
  }

}
