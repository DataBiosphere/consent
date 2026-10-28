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

/**
 * The anonymous, read-only Data Showcase catalog lookup (S2.2). It is outside {@code /api} and has
 * no role annotation, as {@link StatusResource} does. {@code PublicRateLimitFilter} limits it by
 * client IP.
 *
 * <p>The resource reads the raw body itself, so that it can refuse a body over {@link
 * ShowcaseLookupLimits#MAX_BODY_BYTES} with a 413 before any of it is parsed: first on a declared
 * {@code Content-Length}, then on the bytes actually sent, which covers a chunked body or a length
 * that is not true. A 500 carries a fixed message, so no exception text reaches a public caller.
 * Every response is no-store.
 */
@Path("showcase")
public class PublicShowcaseCatalogResource extends Resource {

  static final String TOO_LARGE_MESSAGE =
      "Request body is larger than %d bytes".formatted(ShowcaseLookupLimits.MAX_BODY_BYTES);
  static final String SERVER_ERROR_MESSAGE = "The showcase lookup failed";

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
      byte[] bytes = body.readNBytes(ShowcaseLookupLimits.MAX_BODY_BYTES + 1);
      if (bytes.length > ShowcaseLookupLimits.MAX_BODY_BYTES) {
        return error(Response.Status.REQUEST_ENTITY_TOO_LARGE, TOO_LARGE_MESSAGE);
      }
      Object response = showcaseCatalogService.lookup(ShowcaseLookupParser.parse(bytes));
      return Response.ok(NULL_SERIALIZING_GSON.toJson(response), MediaType.APPLICATION_JSON)
          .cacheControl(NO_STORE)
          .build();
    } catch (ShowcaseLookupException e) {
      return error(Response.Status.BAD_REQUEST, e.getMessage());
    } catch (IOException | RuntimeException e) {
      logException(e);
      return error(Response.Status.INTERNAL_SERVER_ERROR, SERVER_ERROR_MESSAGE);
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
