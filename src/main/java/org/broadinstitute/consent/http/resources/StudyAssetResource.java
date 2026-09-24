package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.StudyAssets;
import org.broadinstitute.consent.http.service.StudyAssetService;

@Path("api/dataset/study/{studyId}/assets")
@Produces(MediaType.APPLICATION_JSON)
public class StudyAssetResource extends Resource {
  private final StudyAssetService service;

  @Inject
  public StudyAssetResource(StudyAssetService service) {
    this.service = service;
  }

  @GET
  @Path("/publications")
  @PermitAll
  public Response publications(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    return assets(user, studyId, StudyAssets.PUBLICATIONS);
  }

  @GET
  @Path("/models")
  @PermitAll
  public Response models(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    return assets(user, studyId, StudyAssets.MODELS);
  }

  @GET
  @Path("/workspaces")
  @PermitAll
  public Response workspaces(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    return assets(user, studyId, StudyAssets.WORKSPACES);
  }

  @GET
  @Path("/presentations")
  @PermitAll
  public Response presentations(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    return assets(user, studyId, StudyAssets.PRESENTATIONS);
  }

  @GET
  @Path("/clinicalTrials")
  @PermitAll
  public Response clinicalTrials(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    return assets(user, studyId, StudyAssets.CLINICAL_TRIALS);
  }

  @GET
  @Path("/intellectualProperty")
  @PermitAll
  public Response intellectualProperty(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    return assets(user, studyId, StudyAssets.INTELLECTUAL_PROPERTIES);
  }

  @GET
  @Path("/fundingResources")
  @PermitAll
  public Response fundingResources(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    return assets(user, studyId, StudyAssets.FUNDING);
  }

  private Response assets(DuosUser user, Integer studyId, String key) {
    try {
      return Response.ok(service.getAssetsByType(studyId, user.getUser(), key)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
