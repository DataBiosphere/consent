package org.broadinstitute.consent.http.resources;

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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationResponse;
import org.broadinstitute.consent.http.service.studytemplate.StudyDatasetTemplateService;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;
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
   * A template that fails validation is a completed result rather than a failed request: it answers
   * 200 with {@code valid: false} and the errors the producer has to fix. Only the request itself
   * being unusable — no file, more than one, or one too large to read — is a failure.
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/template-validation")
  @RolesAllowed({ADMIN, CHAIRPERSON, DATASUBMITTER})
  public Response validateTemplate(@Auth DuosUser duosUser, FormDataMultiPart multipart) {
    try {
      byte[] template = readTemplate(multipart);
      if (template.length > StudyTemplateValidationService.MAX_TEMPLATE_BYTES) {
        return tooLarge();
      }
      TemplateValidationResponse response =
          templateService.validateAndCreateDraft(
              new ByteArrayInputStream(template), duosUser.getUser());
      return Response.ok().entity(response).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /** Reads one byte beyond the limit, which is all it takes to know the file exceeds it. */
  private byte[] readTemplate(FormDataMultiPart multipart) throws IOException {
    List<FormDataBodyPart> parts = multipart == null ? List.of() : multipart.getFields(FILE_PART);
    if (parts == null || parts.isEmpty()) {
      throw new BadRequestException(
          "A template file is required in the '%s' part".formatted(FILE_PART));
    }
    if (parts.size() > 1) {
      throw new BadRequestException("Only one template file may be uploaded at a time");
    }
    FormDataBodyPart part = parts.getFirst();
    validateFileDetails(part.getContentDisposition());
    try (InputStream content = part.getValueAs(InputStream.class)) {
      return content.readNBytes(StudyTemplateValidationService.MAX_TEMPLATE_BYTES + 1);
    }
  }

  private static Response tooLarge() {
    return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            new Error(
                StudyTemplateValidationService.TOO_LARGE_MESSAGE,
                Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()))
        .build();
  }
}
