package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Date;
import java.util.HashSet;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
  @Mock private UriInfo info;
  @Mock private UriBuilder builder;

  private void initResource() {
    resource = new DarCollectionResource(userService, darCollectionService, datasetService);
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
    collection.setDars(new ArrayList<DataAccessRequest>());
    for(int i = 0; i < 3; i++) {
      collection.getDars().add(mockDataAccessRequestWithDatasetIds());
    }
    return collection;
  }

  private Set<DatasetDTO> mockDatasetsForResearcherCollection() {
    Set<DatasetDTO> datasets = new HashSet<DatasetDTO>();
    for(int i = 1; i < 3; i++) {
      DatasetDTO newDataset = new DatasetDTO();
      newDataset.setDataSetId(i);
      datasets.add(newDataset);
    }
    return datasets;
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetCollectionsForResearcher() {
    List<DarCollection> mockCollectionsList = new ArrayList<DarCollection>();
    mockCollectionsList.add(mockDarCollection());
    mockCollectionsList.add(mockDarCollection());
    when(userService.findUserByEmail(anyString())).thenReturn(researcher);
    when(darCollectionService.findDarCollectionsByUserId(anyInt())).thenReturn(mockCollectionsList);
    when(datasetService.getDatasetDTOByIds(any())).thenReturn(mockDatasetsForResearcherCollection());
    initResource();

    Response response = resource.getCollectionsForResearcher(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  
}
