package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.ShowcaseLookupResponse;
import org.broadinstitute.consent.http.models.ShowcasePageResponse;
import org.broadinstitute.consent.http.models.ShowcaseStatsResponse;
import org.broadinstitute.consent.http.models.ShowcaseUnavailableResult;
import org.broadinstitute.consent.http.service.showcase.ShowcaseAllLookup;
import org.broadinstitute.consent.http.service.showcase.ShowcaseCatalogService;
import org.broadinstitute.consent.http.service.showcase.ShowcaseIdentifiersLookup;
import org.broadinstitute.consent.http.service.showcase.ShowcaseLookupLimits;
import org.broadinstitute.consent.http.service.showcase.ShowcaseScope;
import org.broadinstitute.consent.http.service.showcase.ShowcaseSection;
import org.broadinstitute.consent.http.service.showcase.ShowcaseStatsLookup;
import org.broadinstitute.consent.http.util.gson.JerseyGsonProvider;
import org.eclipse.jetty.http.BadMessageException;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Sends requests through Jersey's own routing with no authentication registered, so a pass here
 * also shows that the route needs no user. Synthetic data only.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class PublicShowcaseCatalogResourceTest {

  private static final String PATH = "/showcase/datasets";
  private static final String REQUEST =
      """
      { "mode": "identifiers", "section": "open-access-datasets",
        "identifiers": ["DUOS-000123", "DUOS-000999"] }""";
  private static final ShowcaseIdentifiersLookup LOOKUP =
      new ShowcaseIdentifiersLookup(
          ShowcaseSection.OPEN_ACCESS_DATASETS, List.of("DUOS-000123", "DUOS-000999"));

  private final ShowcaseCatalogService service = mock(ShowcaseCatalogService.class);

  private final ResourceExtension resource =
      ResourceExtension.builder()
          .addResource(new PublicShowcaseCatalogResource(service))
          .addProvider(JerseyGsonProvider.class)
          .build();

  @Test
  void testAnonymousLookup() {
    when(service.lookup(LOOKUP))
        .thenReturn(
            new ShowcaseLookupResponse(
                List.of(
                    new ShowcaseDatasetSummary(
                        "DUOS-000123",
                        "Example Open Cohort",
                        "open",
                        null,
                        List.of("GRU"),
                        1200,
                        "2026-10-02",
                        "https://example.org/open-cohort",
                        null),
                    new ShowcaseUnavailableResult("DUOS-000999"))));

    Response response = post(REQUEST);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNoStore(response);
    JsonObject body = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
    // The summary keys are exactly the public allowlist, and an unknown value is null.
    assertEquals(
        Set.copyOf(ShowcasePublicField.jsonNames()),
        body.getAsJsonArray("datasets").get(0).getAsJsonObject().keySet());
    assertEquals(
        JsonParser.parseString(
            """
            { "datasets": [
              { "identifier": "DUOS-000123", "title": "Example Open Cohort",
                "accessManagement": "open", "dacName": null, "consentCodes": ["GRU"],
                "participantCount": 1200, "availableDate": "2026-10-02",
                "accessUrl": "https://example.org/open-cohort", "species": null },
              { "identifier": "DUOS-000999", "status": "unavailable" } ] }
            """),
        body);
  }

  /** The last page still has the key, as null. */
  @Test
  void testLastPageHasNullCursor() {
    when(service.lookup(new ShowcaseAllLookup(ShowcaseLookupLimits.DEFAULT_PAGE_SIZE, null)))
        .thenReturn(new ShowcasePageResponse(List.of(), null));

    Response response = post("{ \"mode\": \"all\" }");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNoStore(response);
    assertEquals(
        JsonParser.parseString("{ \"datasets\": [], \"nextCursor\": null }"),
        JsonParser.parseString(response.readEntity(String.class)));
  }

  /** The duos-ui {@code lookupExamples} partner hero example. */
  @Test
  void testStats() {
    when(service.lookup(new ShowcaseStatsLookup(new ShowcaseScope(List.of("Example DAC")))))
        .thenReturn(new ShowcaseStatsResponse(14, 37));

    Response response = post("{ \"mode\": \"stats\", \"scope\": { \"dac\": [\"Example DAC\"] } }");

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNoStore(response);
    assertEquals(
        JsonParser.parseString("{ \"studyCount\": 14, \"datasetCount\": 37 }"),
        JsonParser.parseString(response.readEntity(String.class)));
  }

  @Test
  void testRejectedRequestReadsNothing() {
    Response response =
        post(
            """
            { "mode": "identifiers", "section": "open-access-datasets",
              "identifiers": ["DUOS-123"] }""");

    assertError(
        response,
        Response.Status.BAD_REQUEST,
        "Identifier \"DUOS-123\" is not canonical; use \"DUOS-000123\"");
    verifyNoInteractions(service);
  }

  /** Past the cap, and past an int, as the header of a multi-gigabyte body would be. */
  @Test
  void testDeclaredLengthOverTheCapIsRefused() {
    Response response =
        resource
            .target(PATH)
            .request()
            .header(HttpHeaders.CONTENT_LENGTH, "5000000000")
            .post(Entity.json(REQUEST));

    assertError(
        response,
        Response.Status.REQUEST_ENTITY_TOO_LARGE,
        PublicShowcaseCatalogResource.TOO_LARGE_MESSAGE);
    verifyNoInteractions(service);
  }

  @Test
  void testBodyAtTheCapIsRead() {
    when(service.lookup(LOOKUP)).thenReturn(new ShowcaseLookupResponse(List.of()));

    Response response = post(padded(REQUEST, ShowcaseLookupLimits.MAX_BODY_BYTES));

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testBodyOverTheCapIsRefused() {
    Response response = post(padded(REQUEST, ShowcaseLookupLimits.MAX_BODY_BYTES + 1));

    assertError(
        response,
        Response.Status.REQUEST_ENTITY_TOO_LARGE,
        PublicShowcaseCatalogResource.TOO_LARGE_MESSAGE);
    verifyNoInteractions(service);
  }

  /** A body that declares a small length, or none, is still bounded by the bytes actually read. */
  @ParameterizedTest
  @ValueSource(strings = {"10", "", "not-a-number"})
  void testUntrueLengthIsBoundedByTheRead(String declaredLength) {
    Response response =
        resource
            .target(PATH)
            .request()
            .header(HttpHeaders.CONTENT_LENGTH, declaredLength)
            .post(Entity.json(padded(REQUEST, ShowcaseLookupLimits.MAX_BODY_BYTES + 1)));

    assertError(
        response,
        Response.Status.REQUEST_ENTITY_TOO_LARGE,
        PublicShowcaseCatalogResource.TOO_LARGE_MESSAGE);
    verifyNoInteractions(service);
  }

  /**
   * Calls the method directly, because the test client can set its own {@code Content-Length}: this
   * pins the bounded read on its own, with no header at all.
   */
  @Test
  void testBodyWithNoDeclaredLengthIsBoundedByTheRead() {
    byte[] body =
        padded(REQUEST, ShowcaseLookupLimits.MAX_BODY_BYTES + 1).getBytes(StandardCharsets.UTF_8);

    Response response =
        new PublicShowcaseCatalogResource(service)
            .lookupDatasets(null, new ByteArrayInputStream(body));

    assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
    verifyNoInteractions(service);
  }

  /** An aborted upload or a broken chunked body is the client's fault: a 400, not a 500. */
  @ParameterizedTest
  @ValueSource(classes = {EofException.class, BadMessageException.class})
  void testClientReadErrorIsABadRequest(Class<? extends Throwable> failure) throws Exception {
    InputStream body = mock(InputStream.class);
    when(body.readNBytes(anyInt())).thenThrow(failure);

    Response response = new PublicShowcaseCatalogResource(service).lookupDatasets(null, body);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(
        PublicShowcaseCatalogResource.UNREADABLE_BODY_MESSAGE,
        ((Error) response.getEntity()).message());
    verifyNoInteractions(service);
  }

  @Test
  void testServerErrorHidesTheExceptionText() {
    when(service.lookup(any()))
        .thenThrow(new IllegalStateException("pi@example.org gs://bucket/location"));

    Response response = post(REQUEST);

    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    String body = response.readEntity(String.class);
    assertFalse(body.contains("pi@example.org"), body);
    assertFalse(body.contains("gs://"), body);
    assertEquals(
        PublicShowcaseCatalogResource.SERVER_ERROR_MESSAGE,
        JsonParser.parseString(body).getAsJsonObject().get("message").getAsString());
  }

  private Response post(String body) {
    return resource.target(PATH).request().post(Entity.json(body));
  }

  private static void assertError(Response response, Response.Status status, String message) {
    assertEquals(status.getStatusCode(), response.getStatus());
    assertNoStore(response);
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    assertEquals(
        message,
        JsonParser.parseString(response.readEntity(String.class))
            .getAsJsonObject()
            .get("message")
            .getAsString());
  }

  private static void assertNoStore(Response response) {
    assertEquals("no-store", response.getHeaderString(HttpHeaders.CACHE_CONTROL));
  }

  /** The JSON request padded with trailing spaces to exactly {@code length} bytes. */
  private static String padded(String json, int length) {
    return json + " ".repeat(length - json.length());
  }
}
