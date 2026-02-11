package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
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
      ontologyService.indexOntology(duosUser.getUser(), type);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @RolesAllowed({ADMIN})
  public Response deleteOntologyTerms(
      @Auth DuosUser duosUser, @QueryParam("ontologyType") String ontologyType) {
    try {
      OntologyType type = OntologyType.getFromName(ontologyType);
      if (type == null) {
        throw new IllegalArgumentException("Invalid ontology type: " + ontologyType);
      }
      ontologyService.deleteOntologyTerms(type);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("search")
  @Produces({MediaType.APPLICATION_JSON})
  public Response searchByTermIds(@QueryParam("ids") String ids) {
    try {
      StreamingOutput stream = ontologyService.findByTermIds(ids.split(","));
      return Response.ok(stream).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("autocomplete")
  @Produces({MediaType.APPLICATION_JSON})
  public Response autocomplete(
      @QueryParam("q") String q,
      @QueryParam("type") String type,
      @QueryParam("count") Integer count) {
    try {
      OntologyType ontologyType = null;
      if (type != null && !type.isBlank()) {
        ontologyType = OntologyType.getFromName(type);
      }
      StreamingOutput stream = ontologyService.findByQuery(q, ontologyType, count);
      return Response.ok(stream).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
