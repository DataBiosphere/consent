package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.service.sam.SamService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("api/sam")
public class SamResource extends Resource {

  private final SamService samService;

  @Inject
  public SamResource(SamService samService) {
    this.samService = samService;
  }

  @Path("resource-types")
  @GET
  @Produces("application/json")
  public Response getResourceTypes(@Auth AuthUser authUser) {
      try {
          List<ResourceType> types = samService.getResourceTypes(authUser);
          //  Jackson is the default - use Gson to serialize the entity
          return Response.ok().entity(new Gson().toJson(types)).build();
      } catch (Exception e) {
          return createExceptionResponse(e);
      }
  }
}