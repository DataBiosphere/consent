package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TDRServiceTest extends AbstractTestHelper {

  @Mock
  private DataAccessRequestService darService;

  @Mock
  private DatasetDAO datasetDAO;

  @Mock
  private LibraryCardDAO libraryCardDAO;

  @Mock
  private UserDAO userDAO;

  @Mock
  SamDAO samDAO;

  @Mock
  AuthUser authUser;
  private TDRService service;

  private void initService() {
    service = new TDRService(darService, datasetDAO, libraryCardDAO, samDAO, userDAO);
  }

  @Test
  void testGetApprovedUsersForDataset() {
    Dataset dataset = new Dataset();

    User user1 = new User();
    user1.setUserId(1);
    user1.setEmail("asdf1@gmail.com");
    LibraryCard libraryCard1 = new LibraryCard();
    libraryCard1.setUserEmail(user1.getEmail());
    user1.setLibraryCard(libraryCard1);
    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(user1.getUserId());
    DataAccessRequestData data = new DataAccessRequestData();
    Collaborator lab =
        new Collaborator(null, "lab@gmail.com", null, null, null, null, null);
    data.setLabCollaborators(List.of(lab));
    Collaborator internal =
        new Collaborator(null, "internal@gmail.com", null, null, null, null, null);
    LibraryCard libraryCard3 = new LibraryCard();
    libraryCard3.setUserEmail(internal.email());
    data.setInternalCollaborators(List.of(internal));
    dar1.setData(data);
    User user2 = new User();
    user2.setUserId(2);
    user2.setEmail("asdf2@gmail.com");
    LibraryCard libraryCard2 = new LibraryCard();
    libraryCard2.setUserEmail(user2.getEmail());
    user2.setLibraryCard(libraryCard2);
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(user2.getUserId());

    when(darService.getApprovedDARsForDataset(dataset)).thenReturn(List.of(dar1, dar2));
    when(userDAO.findUsers(any())).thenReturn(List.of(user1, user2));
    // Mock that the library cards exist for user 1, 2, and internal collaborator, but not the internal lab staff.
    when(libraryCardDAO.findByUserEmails(anyList()))
        .thenReturn(List.of(libraryCard1, libraryCard2, libraryCard3));
    initService();

    ApprovedUsers approvedUsers = service.getApprovedUsersForDataset(authUser, dataset);
    List<String> approvedUsersEmails = approvedUsers.approvedUsers().stream()
        .map(ApprovedUser::email)
        .toList();

    assertTrue(
        approvedUsersEmails.containsAll(
            List.of(user1.getEmail(), user2.getEmail(), internal.email())),
        "Approved users should include user1, user2, and internal collaborator");
    assertFalse(
        approvedUsersEmails.contains(lab.email()),
        "Lab collaborator should not be included as they do not have a library card"
    );
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
    Collaborator lab = new Collaborator(null, " ", null, null, null, null, null);
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
  void testGetApprovedUsersForDataset_logsInfo() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setAlias(00001);
    User user1 = new User();
    user1.setUserId(1);
    user1.setEmail("user1@example.com");
    LibraryCard libraryCard1 = new LibraryCard();
    libraryCard1.setUserEmail(user1.getEmail());
    user1.setLibraryCard(libraryCard1);
    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(user1.getUserId());
    dar1.setData(new DataAccessRequestData());

    when(darService.getApprovedDARsForDataset(dataset)).thenReturn(List.of(dar1));
    when(userDAO.findUsers(any())).thenReturn(List.of(user1));
    when(libraryCardDAO.findByUserEmails(anyList())).thenReturn(List.of(libraryCard1));

    // Spy the service to intercept logInfo
    initService();
    TDRService spyService = spy(service);
    doNothing().when(spyService).logInfo(any());

    spyService.getApprovedUsersForDataset(authUser, dataset);

    verify(spyService).logInfo(org.mockito.ArgumentMatchers.contains("Approved users requested. Requesting user:"));
    verify(spyService).logInfo(org.mockito.ArgumentMatchers.contains("user1@example.com"));
    verify(spyService).logInfo(org.mockito.ArgumentMatchers.contains("DUOS-000001"));  }
}
