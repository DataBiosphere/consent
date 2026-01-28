package org.broadinstitute.consent.http.resources;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.OntologyService;

/**
 * Resource class to handle ontology-related endpoints. Provides functionality to index ontology
 * terms and search for terms. Indexing (/api/ontology) is restricted to ADMIN users. Searching
 * (/ontology/search or /api/ontology/search) is open to authed or un-authed requests.
 */
@Path("{api : (api/)?}ontology")
public class OntologyResource extends Resource {

  private final OntologyService ontologyService;

  @Inject
  public OntologyResource(OntologyService ontologyService) {
    this.ontologyService = ontologyService;
  }

  @POST
  @RolesAllowed({ADMIN})
  public Response indexOntologyTerms(
      @Auth DuosUser duosUser, @QueryParam("ontologyType") String ontologyType) {
    try {
      OntologyType type = OntologyType.getFromName(ontologyType);
      if (type == null) {
        throw new IllegalArgumentException("Invalid ontology type: " + ontologyType);
      }
      ontologyService.indexOntology(duosUser.getUser(), type.getFileName(), type.name());
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("search")
  public Response searchTerm(@QueryParam("termIDs") String termIDs) {
    try {
      List<JsonObject> termList = ontologyService.findByTermIds(termIDs);
      if (termList != null && !termList.isEmpty()) {
        return Response.ok(termList).build();
      } else {
        throw new NotFoundException("Ontology term not found for term IDs: " + termIDs);
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
