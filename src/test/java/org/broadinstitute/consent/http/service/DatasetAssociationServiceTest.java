package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.sql.BatchUpdateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetAssociationServiceTest {

  @Mock
  private DatasetAssociationDAO dsAssociationDAO;
  @Mock
  private UserDAO userDAO;
  @Mock
  private DatasetDAO dsDAO;
  @Mock
  private UserRoleDAO userRoleDAO;

  private DatasetAssociationService service;

  @BeforeEach
  public void setUp() {
    service = new DatasetAssociationService(dsAssociationDAO, userDAO, dsDAO, userRoleDAO);
  }

  @Test
  void testGetAndVerifyUsersUserNotDataOwner() {
    when(dsDAO.findDatasetById(any())).thenReturn(ds1);
    when(userDAO.findUsersWithRoles(notNull())).thenReturn(
        new HashSet<>(Arrays.asList(member, chairperson)));
    doNothing().when(userRoleDAO).insertSingleUserRole(any(), any());
    assertDoesNotThrow(() -> {
      service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    });
  }

  @Test
  void testGetAndVerifyUsersInvalidUsersList() {
    when(userDAO.findUsersWithRoles(notNull())).thenReturn(
        new HashSet<>(Arrays.asList(member, chairperson)));
    List<Integer> userIds = Arrays.asList(1, 2, 3, 4);
    assertThrows(BadRequestException.class, () -> {
      service.createDatasetUsersAssociation(1, userIds);
    });
  }

  @Test
  void testCreateDatasetUsersAssociation() {
    when(userDAO.findUsersWithRoles(notNull())).thenReturn(
        new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
    when(dsDAO.findDatasetById(1)).thenReturn(ds1);
    when(dsAssociationDAO.getDatasetAssociation(1)).thenReturn(
        Arrays.asList(dsAssociation1, dsAssociation2));
    assertDoesNotThrow(() -> {
      service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    });
  }

  @Test
  void testCreateDatasetUsersAssociationNotFoundException() {
    when(userDAO.findUsersWithRoles(notNull())).thenReturn(
        new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
    when(dsDAO.findDatasetById(1)).thenReturn(null);
    List<Integer> userIds = Arrays.asList(1, 2);
    assertThrows(NotFoundException.class, () -> {
      service.createDatasetUsersAssociation(1, userIds);
    });
  }

  @Test
  void testCreateDatasetUsersAssociationBadRequestException() {
    when(userDAO.findUsersWithRoles(notNull())).thenReturn(
        new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
    when(dsDAO.findDatasetById(1)).thenReturn(ds1);

    doAnswer(invocationOnMock -> {
      throw new BatchUpdateException();
    }).when(dsAssociationDAO).insertDatasetUserAssociation(any());
    assertThrows(BatchUpdateException.class, () -> {
      service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    });
  }


  /**
   * Private data methods
   **/
  private static final String CHAIRPERSON = "Chairperson";
  private static final String DACMEMBER = "Member";
  private static final String DATAOWNER = "DataOwner";

  private final DatasetAssociation dsAssociation1 = new DatasetAssociation(1, 3);
  private final DatasetAssociation dsAssociation2 = new DatasetAssociation(1, 4);

  private final Dataset ds1 = new Dataset(1, "DS-001", "DS-001", new Date());
  private final User chairperson = new User(1, "originalchair@broad.com", "Original Chairperson", new Date(),
      chairpersonList());
  private final User member = new User(2, "originalchair@broad.com", "Original Chairperson", new Date(),
      memberList());
  private final User dataOwner1 = new User(3, "originalchair@broad.com", "Original Chairperson", new Date(),
      dataownerList());
  private final User dataOwner2 = new User(4, "originalchair@broad.com", "Original Chairperson", new Date(),
      dataownerList());

  private List<UserRole> chairpersonList() {
    return List.of(getChairpersonRole());
  }

  private List<UserRole> memberList() {
    return List.of(getMemberRole());
  }

  private List<UserRole> dataownerList() {
    return List.of(getDataOwnerRole());
  }

  private UserRole getMemberRole() {
    return new UserRole(1, DACMEMBER);
  }

  private UserRole getChairpersonRole() {
    return new UserRole(2, CHAIRPERSON);
  }

  private UserRole getDataOwnerRole() {
    return new UserRole(6, DATAOWNER);
  }

}