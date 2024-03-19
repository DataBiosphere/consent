package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("api/datasetAssociation")
public class DatasetAssociationsResource extends Resource {


  @Inject
  public DatasetAssociationsResource() {
  }


  @Deprecated
  @POST
  @Path("/{datasetId}")
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response associateDatasetWithUsers() {
    return Response.noContent().build();
  }

  @Deprecated
  @GET
  @Path("/{dataSetId}")
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  public Response getDatasetAssociations() {
    return Response.noContent().build();
  }

  @Deprecated
  @PUT
  @Path("/{dataSetId}")
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response updateDatasetAssociations() {
    return Response.noContent().build();
  }
}
