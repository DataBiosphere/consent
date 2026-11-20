package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.reflect.ClassPath;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.broadinstitute.consent.http.models.Error;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Routes multipart requests through Jersey, so the limit is shown to apply before the body is read
 * into the resource method's parameters: a unit test on the filter can't show that.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class UploadSizeLimitEndpointTest {

  private static final long LIMIT = 4_000;

  @Path("upload")
  public static class UploadResource {

    @POST
    @UploadSizeLimited
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@FormDataParam("file") InputStream file) throws IOException {
      return Response.ok(Integer.toString(file.readAllBytes().length)).build();
    }
  }

  private static final ResourceExtension RESOURCE =
      ResourceExtension.builder()
          .addResource(new UploadResource())
          .addProvider(new UploadSizeLimitFeature(LIMIT))
          .addProvider(MultiPartFeature.class)
          .build();

  @Test
  void testAnUploadUnderTheLimitIsRead() throws IOException {
    try (Response response = post(1_000)) {
      assertEquals(200, response.getStatus());
      assertEquals("1000", response.readEntity(String.class));
    }
  }

  @Test
  void testAnUploadDeclaringMoreThanTheLimitIsRefused() throws IOException {
    try (Response response = post(5_000)) {
      assertEquals(413, response.getStatus());
      assertEquals(
          new Error("Request is too large. Max size is: 0.004 MB", 413),
          response.readEntity(Error.class));
    }
  }

  // Every endpoint that takes an upload is bounded, except the template upload, which has its own
  // guard (TemplateSizeLimitFilter).
  @Test
  void testEveryUploadEndpointIsBounded() throws IOException {
    List<String> unbounded =
        ClassPath.from(getClass().getClassLoader())
            .getTopLevelClasses("org.broadinstitute.consent.http.resources")
            .stream()
            .map(ClassPath.ClassInfo::load)
            .filter(type -> type.isAnnotationPresent(Path.class))
            .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
            .filter(UploadSizeLimitEndpointTest::takesAnUpload)
            .filter(method -> !method.isAnnotationPresent(UploadSizeLimited.class))
            .filter(method -> !method.isAnnotationPresent(TemplateSizeLimited.class))
            .map(method -> method.getDeclaringClass().getSimpleName() + "." + method.getName())
            .toList();
    assertTrue(unbounded.isEmpty(), "Upload endpoints without a size limit: " + unbounded);
  }

  // The guard finds an upload however it's declared: by the method's media type, by the class's,
  // or by a multipart parameter with no media type at all.
  @Test
  void testTheGuardFindsEveryKindOfUpload() throws NoSuchMethodException {
    assertTrue(takesAnUpload(UploadResource.class.getMethod("upload", InputStream.class)));
    assertTrue(takesAnUpload(Undeclared.class.getMethod("byClass", InputStream.class)));
    assertTrue(takesAnUpload(Undeclared.class.getMethod("byParameter", FormDataMultiPart.class)));
    assertFalse(takesAnUpload(Undeclared.class.getMethod("json", String.class)));
    assertFalse(takesAnUpload(Undeclared.class.getMethod("helper", FormDataMultiPart.class)));
  }

  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public static class Undeclared {
    @POST
    public void byClass(@FormDataParam("file") InputStream file) {}

    @POST
    @Consumes(MediaType.WILDCARD)
    public void byParameter(FormDataMultiPart multipart) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void json(String body) {}

    public void helper(FormDataMultiPart multipart) {}
  }

  private static boolean takesAnUpload(Method method) {
    if (Arrays.stream(method.getAnnotations())
        .noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class))) {
      return false;
    }
    Consumes consumes =
        method.isAnnotationPresent(Consumes.class)
            ? method.getAnnotation(Consumes.class)
            : method.getDeclaringClass().getAnnotation(Consumes.class);
    boolean uploadType =
        consumes != null
            && Arrays.stream(consumes.value())
                .anyMatch(
                    type ->
                        type.startsWith(MediaType.MULTIPART_FORM_DATA)
                            || type.equals("application/binary"));
    boolean multipartParameter =
        Arrays.stream(method.getParameters())
            .anyMatch(
                parameter ->
                    parameter.isAnnotationPresent(FormDataParam.class)
                        || MultiPart.class.isAssignableFrom(parameter.getType()));
    return uploadType || multipartParameter;
  }

  private Response post(int fileBytes) throws IOException {
    try (FormDataMultiPart multipart = new FormDataMultiPart()) {
      multipart.bodyPart(
          new FormDataBodyPart(
              FormDataContentDisposition.name("file").fileName("letter.pdf").build(),
              new byte[fileBytes],
              MediaType.APPLICATION_OCTET_STREAM_TYPE));
      return RESOURCE
          .target("upload")
          .register(MultiPartFeature.class)
          .request()
          .post(Entity.entity(multipart, multipart.getMediaType()));
    }
  }
}
