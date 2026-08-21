package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.service.studytemplate.StudyDatasetTemplateService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 * Study/dataset template validation. Typed rather than generic so this validator does not become
 * the implied one for every draft type a later ticket adds.
 */
@Path("api/draft/v1/study-dataset")
public class StudyDatasetTemplateResource extends Resource {

  private static final String FILE_PART = "file";

  private final StudyDatasetTemplateService templateService;

  @Inject
  public StudyDatasetTemplateResource(StudyDatasetTemplateService templateService) {
    this.templateService = templateService;
  }

  /**
   * A template that fails validation is a completed result rather than a failed request. Only an
   * unusable request — no file, more than one, or one too large to read — is a failure.
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/template-validation")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  @Timed
  public Response validateTemplate(@Auth DuosUser duosUser, FormDataMultiPart multipart) {
    // Streamed rather than buffered here: the size limit has one owner, the validator.
    try (InputStream template = templatePart(multipart)) {
      return Response.ok()
          .entity(templateService.validateAndCreateDraft(template, duosUser.getUser()))
          .build();
    } catch (TemplateTooLargeException e) {
      return tooLarge(e);
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private InputStream templatePart(FormDataMultiPart multipart) {
    List<FormDataBodyPart> parts = multipart == null ? List.of() : multipart.getFields(FILE_PART);
    if (parts == null || parts.isEmpty()) {
      throw new BadRequestException(
          "A template file is required in the '%s' part".formatted(FILE_PART));
    }
    if (parts.size() > 1) {
      throw new BadRequestException("Only one template file may be uploaded at a time");
    }
    // The name is never used, so there is no stored name for a traversal check to protect.
    return parts.getFirst().getValueAs(InputStream.class);
  }

  private static Response tooLarge(TemplateTooLargeException e) {
    return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
        .type(MediaType.APPLICATION_JSON)
        .entity(new Error(e.getMessage(), Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()))
        .build();
  }
}
