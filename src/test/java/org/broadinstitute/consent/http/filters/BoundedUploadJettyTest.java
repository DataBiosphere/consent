package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.MultiPartProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Sends oversized bodies chunked to a real server, which the in-memory test container can't do (it
 * always declares the length): only then does the bound on the stream, rather than the check on the
 * declared length, have to stop the read. Checks the answer is a 413 and that nothing the refused
 * read wrote to temporary disk is left behind.
 */
class BoundedUploadJettyTest {

  @Path("/")
  public static class UploadResource {

    @POST
    @Path("template")
    @TemplateSizeLimited
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response template(@FormDataParam("file") InputStream file) throws IOException {
      return Response.ok(Long.toString(file.transferTo(java.io.OutputStream.nullOutputStream())))
          .build();
    }

    // Answers with what is in the temporary directory while the parsed body is still held.
    @POST
    @Path("spilled")
    @TemplateSizeLimited
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response spilled(@FormDataParam("file") InputStream file) {
      return Response.ok(Integer.toString(temporaryFiles().size())).build();
    }
  }

  private static java.nio.file.Path tempDir;
  private static Server server;
  private static String base;

  @BeforeAll
  static void start() throws Exception {
    tempDir = Files.createTempDirectory("bounded-upload");
    ResourceConfig config =
        new ResourceConfig()
            .register(new UploadResource())
            .register(TemplateSizeLimitFilter.class)
            .register(MultiPartFeature.class)
            .register(new MultiPartProperties().tempDir(tempDir.toString()).resolver());
    server = new Server(0);
    ServletContextHandler context = new ServletContextHandler();
    context.addServlet(new ServletHolder(new ServletContainer(config)), "/*");
    server.setHandler(context);
    server.start();
    base = "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort();
  }

  @AfterAll
  static void stop() throws Exception {
    server.stop();
  }

  @Test
  void testATemplateSentChunkedPastTheCapIsRefusedAndLeavesNoTemporaryFile() throws Exception {
    HttpResponse<String> response =
        sendChunked("template", multipart((int) TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1));

    assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.statusCode());
    assertEquals(
        "{\"message\":\"" + StudyTemplateValidationService.TOO_LARGE_MESSAGE + "\",\"code\":413}",
        response.body());
    assertEquals(List.of(), temporaryFiles());
  }

  @Test
  void testATemplateWithinTheCapIsRead() throws Exception {
    HttpResponse<String> response = sendChunked("template", multipart(60_000));

    assertEquals(Response.Status.OK.getStatusCode(), response.statusCode());
    assertEquals("60000", response.body());
  }

  // Shows the checks for leftover files above can fail: a body this size is written to the
  // directory they look in.
  @Test
  void testAParsedBodyIsWrittenToTheTemporaryDirectory() throws Exception {
    HttpResponse<String> response = sendChunked("spilled", multipart(60_000));

    assertEquals(Response.Status.OK.getStatusCode(), response.statusCode());
    assertEquals("1", response.body());
  }

  /** A multipart body with one file part of the given size. */
  static byte[] multipart(int fileBytes) {
    byte[] head =
        ("--b\r\nContent-Disposition: form-data; name=\"file\"; filename=\"f.csv\"\r\n"
                + "Content-Type: text/csv\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII);
    byte[] tail = "\r\n--b--\r\n".getBytes(StandardCharsets.US_ASCII);
    byte[] body = new byte[head.length + fileBytes + tail.length];
    System.arraycopy(head, 0, body, 0, head.length);
    java.util.Arrays.fill(body, head.length, head.length + fileBytes, (byte) 'a');
    System.arraycopy(tail, 0, body, head.length + fileBytes, tail.length);
    return body;
  }

  /** Sends the body with no declared length, as a chunked request. */
  static HttpResponse<String> sendChunked(String path, byte[] body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(base + "/" + path))
            .header("Content-Type", "multipart/form-data; boundary=b")
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body)))
            .build();
    try (HttpClient client = HttpClient.newHttpClient()) {
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }

  static List<String> temporaryFiles() {
    try (Stream<java.nio.file.Path> files = Files.list(tempDir)) {
      return files.map(file -> file.getFileName().toString()).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
