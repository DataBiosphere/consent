package org.broadinstitute.consent.http.resources;

import java.net.URI;
import java.util.Map;

import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DraftSubmission;
import org.broadinstitute.consent.http.models.DraftSubmissionInterface;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.inject.Inject;

import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

public class DraftSubmissionResource extends Resource {
    private UserService userService;

    @Inject
    public DraftSubmissionResource(UserService userService) {
        this.userService = userService;
    }

  @POST
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON})
  @Path("/draft/v1")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response createDraftRegistration(
      @Auth AuthUser authUser,
      FormDataMultiPart multipart,
      @FormDataParam("dataset") String json) {
        User user = userService.findUserByEmail(authUser.getEmail());
        Map<String, FormDataBodyPart> files = extractFilesFromMultiPart(multipart);
        DraftSubmissionInterface draft = new DraftSubmission(json, files, user);
        URI uri = UriBuilder.fromPath(String.format("/api/draftsubmission/v1/%s", draft.getUUID().toString()))
        .build();  
        return Response.created(uri).entity(json).build();
      }



}
