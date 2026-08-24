package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.broadinstitute.consent.http.filters.TemplateSizeLimitFilter;
import org.broadinstitute.consent.http.mappers.TemplateTooLargeExceptionMapper;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.service.studytemplate.StudyDatasetTemplateService;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins the size limit to the endpoint through a request Jersey routes itself. The filter is
 * name-bound rather than called by the resource, and a binding that does not take effect fails
 * silently -- a unit test on the filter cannot tell that apart from one that works.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class TemplateSizeLimitEndpointTest {

  private static final String PATH = "/api/draft/v1/study-dataset/template-validation";

  /** Past the cap, and past an int, as the header of a multi-gigabyte upload would be. */
  private static final String OVERSIZE_LENGTH = "5000000000";

  private static final StudyDatasetTemplateService TEMPLATE_SERVICE =
      mock(StudyDatasetTemplateService.class);

  private static final ResourceExtension RESOURCE =
      ResourceExtension.builder()
          .addResource(new StudyDatasetTemplateResource(TEMPLATE_SERVICE))
          .addProvider(TemplateSizeLimitFilter.class)
          .addProvider(TemplateTooLargeExceptionMapper.class)
          .addProvider(MultiPartFeature.class)
          .addProvider(new AuthValueFactoryProvider.Binder<>(DuosUser.class))
          .build();

  @Test
  void testAnUploadDeclaringMoreThanTheCapIsRefusedOnItsHeaderAlone() throws IOException {
    Response response = post(OVERSIZE_LENGTH);

    assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
    assertEquals(
        new Error(
            StudyTemplateValidationService.TOO_LARGE_MESSAGE,
            Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()),
        response.readEntity(Error.class));
    // Nothing downstream ran: not the authentication the endpoint requires, not the service.
    verifyNoInteractions(TEMPLATE_SERVICE);
  }

  @Test
  void testAnUploadWithinTheCapIsNotRefusedByTheGuard() throws IOException {
    // Nothing authenticates this request, so it fails further along; what this pins is that the
    // guard let it through rather than refusing every upload that declares a length.
    Response response = post("64");

    assertNotEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
  }

  private Response post(String declaredLength) throws IOException {
    try (FormDataMultiPart multipart = new FormDataMultiPart()) {
      multipart.bodyPart(
          new FormDataBodyPart(
              FormDataContentDisposition.name("file").fileName("template.csv").build(),
              "1,study".getBytes(),
              MediaType.APPLICATION_OCTET_STREAM_TYPE));
      return RESOURCE
          .target(PATH)
          .register(MultiPartFeature.class)
          .request()
          .header(HttpHeaders.CONTENT_LENGTH, declaredLength)
          .post(Entity.entity(multipart, multipart.getMediaType()));
    }
  }
}
