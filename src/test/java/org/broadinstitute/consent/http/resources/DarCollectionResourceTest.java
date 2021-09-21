package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DarCollectionResourceTest {
  private final AuthUser authUser = new AuthUser("test@test.com");
  private final List<UserRole> researcherRole = Collections.singletonList(
    new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())
  );
  private final User researcher = new User(1, authUser.getEmail(), "Display Name", new Date(), researcherRole, authUser.getEmail());

  private DarCollectionResource resource;

  @Mock private UserService userService;
  @Mock private DatasetService datasetService;
  @Mock private DarCollectionService darCollectionService;

  private void initResource() {
    resource = new DarCollectionResource(userService, darCollectionService);
  }

  private DataAccessRequest mockDataAccessRequestWithDatasetIds() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setDatasetIds(Collections.singletonList(RandomUtils.nextInt(1, 100)));
    dar.setData(data);
    return dar;
  }

  private DarCollection mockDarCollection() {
    DarCollection collection = new DarCollection();
    collection.setDars(new ArrayList<>());
    for(int i = 0; i < 3; i++) {
      collection.getDars().add(mockDataAccessRequestWithDatasetIds());
    }
    return collection;
  }

  private Set<DataSet> mockDatasetsForResearcherCollection() {
    Set<DataSet> datasets = new HashSet<>();
    for(int i = 1; i < 3; i++) {
      DataSet newDataset = new DataSet();
      newDataset.setDataSetId(i);
      datasets.add(newDataset);
    }
    return datasets;
  }

  @Before
  public void setUp() {
    openMocks(this);
  }

  @Test
  public void testGetCollectionsForResearcher() {
    List<DarCollection> mockCollectionsList = new ArrayList<>();
    mockCollectionsList.add(mockDarCollection());
    mockCollectionsList.add(mockDarCollection());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getCollectionsForUser(any(User.class))).thenReturn(mockCollectionsList);
    when(datasetService.getDatasetWithDataUseByIds(any())).thenReturn(mockDatasetsForResearcherCollection());
    initResource();

    Response response = resource.getCollectionsForResearcher(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetCollectionById() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionById(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetCollectionByIdNotFound() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId() + 1);
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByCollectionId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionById(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetCollectionByReferenceId() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionByReferenceId(authUser, "1");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetCollectionByReferenceIdNotFound() {
    DarCollection collection = mockDarCollection();
    collection.setCreateUserId(researcher.getDacUserId() + 1);
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getByReferenceId(any())).thenReturn(collection);
    initResource();

    Response response = resource.getCollectionByReferenceId(authUser, "1");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetCollectionsByInitialQuery_BadSortField() {
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    initResource();

    Response response = resource.getCollectionsByInitialQuery(authUser, "filterTerm", "badSortFieldName", "ASC", 10);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetCollectionsByInitialQuery_BadSortOrder() {
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    initResource();

    Response response = resource.getCollectionsByInitialQuery(authUser, "filterTerm", "projectTitle", "badSortOrder", 10);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetCollectionsByInitialQuery() {
    PaginationResponse<DarCollection> paginationResponse = new PaginationResponse<>();
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.getCollectionsWithFilters(any(PaginationToken.class), any(User.class)))
      .thenReturn(paginationResponse);
    initResource();
    
    Response response = resource.getCollectionsByInitialQuery(authUser, "filterTerm", "projectTitle", "asc", 10);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}