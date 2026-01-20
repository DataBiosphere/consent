package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.OntologyService;

@Path("api/ontology")
public class OntologyAdminResource extends Resource {

  private final OntologyService ontologyService;

  @Inject
  public OntologyAdminResource(OntologyService ontologyService) {
    this.ontologyService = ontologyService;
  }

  @POST
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @RolesAllowed({ADMIN})
  public Response indexOntologyTerms(
      @Auth DuosUser duosUser,
      @QueryParam("fileName") String fileName,
      @QueryParam("fileType") String fileType) {
    try {
      ontologyService.indexOntology(duosUser.getUser(), fileName, fileType);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
