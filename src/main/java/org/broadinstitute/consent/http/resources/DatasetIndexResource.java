package org.broadinstitute.consent.http.resources;


import io.dropwizard.auth.Auth;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/dataset/index")
public class DatasetIndexResource extends Resource {

  private final DatasetService datasetService;

  private final ElasticSearchService esService;

  private UserService userService;


  public DatasetIndexResource(DatasetService datasetService, ElasticSearchService esService,
      UserService userService) {
    this.datasetService = datasetService;
    this.esService = esService;
    this.userService = userService;
  }

  @POST
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  public Response indexAll(@Auth AuthUser authUser) {
    try {
      User user = userService.findUserByEmail(authUser.getEmail());
      List<Dataset> allDatasets = datasetService.findAllDatasetsByUser(user);
      List<DatasetTerm> allTerms = allDatasets.stream().map(esService::toDatasetTerm).toList();
      esService.indexDatasets(allTerms);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Produces("application/json")
  @RolesAllowed({ADMIN})
  @Path("{datasetId}")
  public Response indexDataset(@Auth AuthUser authUser, @PathParam("datasetId") Integer datasetId) {
    try {
      Dataset dataset = datasetService.findDatasetById(datasetId);
      DatasetTerm term = esService.toDatasetTerm(dataset);
      esService.indexDatasets(List.of(term));
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

}
