package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/collections")
public class DarCollectionResource extends Resource {
  private final UserService userService;
  private final DarCollectionService darCollectionService;
  private final DatasetService datasetService;

  @Inject
  public DarCollectionResource(
    UserService userService,
    DarCollectionService darCollectionService,
    DatasetService datasetService
  ) {
    this.userService = userService;
    this.darCollectionService = darCollectionService;
    this.datasetService = datasetService;
  }

  @GET
  @Produces("application/json")
  @Path("/{id}")
  @RolesAllowed(RESEARCHER)
  public Response getCollectionsForResearcher(@Auth AuthUser authUser) {
    try{
      User user = userService.findUserByEmail(authUser.getEmail());
      List<DarCollection> collections = darCollectionService.findDarCollectionsByUserId(user.getDacUserId());
      addDatasetsToCollections(collections);
      return Response.ok().entity(collections).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }

  //Wrote this method here since I don't have the actual service class to work with
  //I can move it over there if needed
  private void addDatasetsToCollections(List<DarCollection> collections) {
    for(DarCollection collection : collections) {
      List<Integer> datasetIds = new ArrayList<Integer>();
      for(DataAccessRequest dar : collection.getDars()) {
        datasetIds.add(dar.getData().getDatasetIds().get(0));
      }
      List<Integer> distinctIds = datasetIds.stream()
        .distinct()
        .collect(Collectors.toList());
      //Dataset call is being done seperatly to keep queries and mappers/reducers from getting too messy
      Set<DatasetDTO> datasets = datasetService.getDatasetDTOByIds(distinctIds);
      collection.setDatasets(datasets);
    }
  }
}