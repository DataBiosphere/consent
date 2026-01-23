package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.OntologyService;

@Path("{api : (api/)?}ontology")
public class OntologyResource extends Resource {

  private final OntologyService ontologyService;

  @Inject
  public OntologyResource(OntologyService ontologyService) {
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

  @GET
  @Path("search")
  public Response searchTerm(@QueryParam("terms") String terms) {
    try {
      List<String> termList = ontologyService.findByTerms(terms);
      if (termList != null && !termList.isEmpty()) {
        return Response.ok(termList).build();
      } else {
        throw new NotFoundException("Ontology term not found for terms: " + terms);
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
