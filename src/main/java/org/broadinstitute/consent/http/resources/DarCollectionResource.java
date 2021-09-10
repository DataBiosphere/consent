package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/collections")
public class DarCollectionResource extends Resource {
  private final UserService userService;
  private final DarCollectionService darCollectionService;

  @Inject
  public DarCollectionResource(
    UserService userService,
    DarCollectionService darCollectionService,
    DatasetService datasetService
  ) {
    this.userService = userService;
    this.darCollectionService = darCollectionService;
  }

  @GET
  @Produces("application/json")
  @RolesAllowed(RESEARCHER)
  public Response getCollectionsForResearcher(@Auth AuthUser authUser) {
    try{
      User user = userService.findUserByEmail(authUser.getEmail());
      List<DarCollection> collections = darCollectionService.getCollectionsForUser(user);
      darCollectionService.addDatasetsToCollections(collections);
      return Response.ok().entity(collections).build();
    } catch(Exception e) {
      return createExceptionResponse(e);
    }
  }
}