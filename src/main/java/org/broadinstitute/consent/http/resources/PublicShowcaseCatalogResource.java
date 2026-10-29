package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.service.showcase.ShowcaseCatalogService;
import org.broadinstitute.consent.http.service.showcase.ShowcaseLookupException;
import org.broadinstitute.consent.http.service.showcase.ShowcaseLookupLimits;
import org.broadinstitute.consent.http.service.showcase.ShowcaseLookupParser;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.eclipse.jetty.http.BadMessageException;

/**
 * The anonymous, read-only Data Showcase catalog lookup (S2.2). It is outside {@code /api} and has
 * no role annotation, as {@link StatusResource} does. {@code PublicRateLimitFilter} limits it by
 * client IP.
 *
 * <p>The resource reads the raw body itself, so that it can refuse a body over {@link
 * ShowcaseLookupLimits#MAX_BODY_BYTES} with a 413 before any of it is parsed: first on a declared
 * {@code Content-Length}, then on the bytes actually sent, which covers a chunked body or a length
 * that is not true. A body that the client stops or breaks while it is read gets a 400, and no
 * Sentry event, because the fault is on the client side. A 500 carries a fixed message, so no
 * exception text reaches a public caller.
 *
 * <p>Every response that this resource builds is no-store. Responses that Jersey or a filter builds
 * before the resource runs, such as a 404, 415 or 429, do not carry that header, and none of them
 * carries dataset data.
 */
@Path("showcase")
public class PublicShowcaseCatalogResource extends Resource {

  static final String TOO_LARGE_MESSAGE =
      "Request body is larger than %d bytes".formatted(ShowcaseLookupLimits.MAX_BODY_BYTES);
  static final String SERVER_ERROR_MESSAGE = "The showcase lookup failed";
  static final String UNREADABLE_BODY_MESSAGE = "Request body could not be read";

  private static final Gson NULL_SERIALIZING_GSON = GsonUtil.getNullSerializingInstance();
  private static final CacheControl NO_STORE = noStore();

  private final ShowcaseCatalogService showcaseCatalogService;

  @Inject
  public PublicShowcaseCatalogResource(ShowcaseCatalogService showcaseCatalogService) {
    this.showcaseCatalogService = showcaseCatalogService;
  }

  @POST
  @Path("datasets")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response lookupDatasets(
      @HeaderParam(HttpHeaders.CONTENT_LENGTH) String contentLength, InputStream body) {
    try {
      if (declaredLength(contentLength) > ShowcaseLookupLimits.MAX_BODY_BYTES) {
        return error(Response.Status.REQUEST_ENTITY_TOO_LARGE, TOO_LARGE_MESSAGE);
      }
      byte[] bytes = readBody(body);
      if (bytes.length > ShowcaseLookupLimits.MAX_BODY_BYTES) {
        return error(Response.Status.REQUEST_ENTITY_TOO_LARGE, TOO_LARGE_MESSAGE);
      }
      Object response = showcaseCatalogService.lookup(ShowcaseLookupParser.parse(bytes));
      return Response.ok(NULL_SERIALIZING_GSON.toJson(response), MediaType.APPLICATION_JSON)
          .cacheControl(NO_STORE)
          .build();
    } catch (ShowcaseLookupException e) {
      return error(Response.Status.BAD_REQUEST, e.getMessage());
    } catch (RuntimeException e) {
      logException(e);
      return error(Response.Status.INTERNAL_SERVER_ERROR, SERVER_ERROR_MESSAGE);
    }
  }

  /**
   * Reads one byte past the cap, so the caller can tell a body at the cap from a larger one. An
   * aborted upload, an idle timeout or a malformed chunked body fails here as an {@link
   * IOException} or a Jetty {@link BadMessageException}: the client caused it, so it is a 400.
   */
  private static byte[] readBody(InputStream body) {
    try {
      return body.readNBytes(ShowcaseLookupLimits.MAX_BODY_BYTES + 1);
    } catch (IOException | BadMessageException e) {
      throw new ShowcaseLookupException(UNREADABLE_BODY_MESSAGE);
    }
  }

  /**
   * Read as a long, so that the header of a multi-gigabyte body is not lost to an int overflow. A
   * missing or malformed header counts as unknown, and the bounded read decides.
   */
  private static long declaredLength(String header) {
    if (header == null || header.isBlank()) {
      return -1;
    }
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static Response error(Response.Status status, String message) {
    return Response.status(status)
        .type(MediaType.APPLICATION_JSON)
        .entity(new Error(message, status.getStatusCode()))
        .cacheControl(NO_STORE)
        .build();
  }

  private static CacheControl noStore() {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoStore(true);
    cacheControl.setNoTransform(false);
    return cacheControl;
  }
}
