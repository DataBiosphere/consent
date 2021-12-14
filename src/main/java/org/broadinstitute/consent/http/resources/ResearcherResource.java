package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.ResearcherService;
import org.broadinstitute.consent.http.service.UserService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;


@Path("api/researcher")
public class ResearcherResource extends Resource {

    private final ResearcherService researcherService;
    private final UserService userService;
    private final LibraryCardService libraryCardService;

    @Inject
    public ResearcherResource(ResearcherService researcherService, UserService userService, LibraryCardService libraryCardService) {
        this.researcherService = researcherService;
        this.userService = userService;
        this.libraryCardService = libraryCardService;
    }

    @DELETE
    @Path("{userId}")
    @Produces("application/json")
    @RolesAllowed(ADMIN)
    public Response deleteAllProperties(@PathParam("userId") Integer userId) {
        try {
            researcherService.deleteResearcherProperties(userId);
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
