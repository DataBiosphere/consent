package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DatasetAssociationService;

@Path("api/datasetAssociation")
public class DatasetAssociationsResource extends Resource {


  private final DatasetAssociationService service;

  @Inject
  public DatasetAssociationsResource(DatasetAssociationService service) {
    this.service = service;
  }


  @POST
  @Path("/{datasetId}")
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response associateDatasetWithUsers(@PathParam("datasetId") Integer datasetId,
      List<Integer> userIdList) {
    try {
      List<DatasetAssociation> associations = service
          .createDatasetUsersAssociation(datasetId, userIdList);
      return Response.status(Response.Status.CREATED).entity(associations).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/{dataSetId}")
  @Consumes("application/json")
  @Produces("application/json")
  @PermitAll
  public Response getDatasetAssociations(@PathParam("dataSetId") Integer datasetId) {
    try {
      Map<String, Collection<User>> userMap = service.findDataOwnersRelationWithDataset(datasetId);
      return Response.ok().entity(userMap).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @PUT
  @Path("/{dataSetId}")
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response updateDatasetAssociations(@PathParam("dataSetId") Integer datasetId,
      List<Integer> userIdList) {
    try {
      List<DatasetAssociation> associations = service
          .updateDatasetAssociations(datasetId, userIdList);
      return Response.ok().entity(associations).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
