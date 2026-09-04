package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.StudyComment;
import org.broadinstitute.consent.http.models.StudyCommentsSummary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.StudyCommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyCommentResourceTest extends AbstractTestHelper {

  @Mock private StudyCommentService service;
  @Mock private DuosUser duosUser;

  private final User user = new User();

  private StudyCommentResource resource;

  @BeforeEach
  void setUp() {
    resource = new StudyCommentResource(service);
  }

  @Test
  void testList() {
    when(duosUser.getUser()).thenReturn(user);
    when(service.list(1, user)).thenReturn(new StudyCommentsSummary(List.of(), null));

    Response response = resource.list(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testListNotFound() {
    when(duosUser.getUser()).thenReturn(user);
    when(service.list(1, user)).thenThrow(new NotFoundException());

    Response response = resource.list(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testPost() {
    when(duosUser.getUser()).thenReturn(user);
    StudyComment comment = new StudyComment(7, 1, 10, 5, "Great", null, null, "Name", "Inst");
    when(service.post(eq(1), eq(user), eq(5), eq("Great"))).thenReturn(comment);

    Response response = resource.post(duosUser, 1, "{\"rating\": 5, \"commentText\": \"Great\"}");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(comment, response.getEntity());
  }

  @Test
  void testPostForbidden() {
    when(duosUser.getUser()).thenReturn(user);
    when(service.post(any(), any(), any(), any())).thenThrow(new ForbiddenException());

    Response response = resource.post(duosUser, 1, "{\"rating\": 5}");
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void testDelete() {
    when(duosUser.getUser()).thenReturn(user);

    Response response = resource.delete(duosUser, 1, 7);
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
  }

  /** An empty or literal-null body is a client error, not a server error. */
  @Test
  void testPostWithEmptyBodyIsABadRequest() {
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST, resource.post(duosUser, 1, "").getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST, resource.post(duosUser, 1, "null").getStatus());
  }

  /**
   * The rating is an Integer on the payload, so by the time the service sees it there is no way to
   * tell a number the client sent from one Gson coerced out of some other JSON type. Assert the
   * type at the edge instead: anything that is not a JSON number is a 400, and nothing is written.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"rating\": \"4\"}", // a string that looks like a number
        "{\"rating\": \"four\"}",
        "{\"rating\": true}",
        "{\"rating\": [4]}",
        "{\"rating\": {\"value\": 4}}",
        "{\"rating\": 4.5}", // truncating to 4 would store a rating nobody sent
        "{\"rating\": 4.0000001}",
        "{\"rating\": 99999999999999999999}" // valid JSON number, too large for an int
      })
  void testPostWithNonNumericRatingIsABadRequest(String body) {
    Response response = resource.post(duosUser, 1, body);

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    Error error = assertInstanceOf(Error.class, response.getEntity());
    assertEquals("Rating must be a whole number between 1 and 5.", error.message());
    verify(service, never()).post(any(), any(), any(), any());
  }

  /** A whole number sent as a JSON number is the only accepted form, including 4.0. */
  @ParameterizedTest
  @ValueSource(strings = {"{\"rating\": 4}", "{\"rating\": 4.0}"})
  void testPostAcceptsWholeJsonNumberRatings(String body) {
    when(duosUser.getUser()).thenReturn(user);
    StudyComment comment = new StudyComment(7, 1, 10, 4, null, null, null, "Name", "Inst");
    when(service.post(eq(1), eq(user), eq(4), eq(null))).thenReturn(comment);

    Response response = resource.post(duosUser, 1, body);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    verify(service).post(1, user, 4, null);
  }

  /**
   * An out-of-range rating is still the service's call to make, so a numeric rating has to reach it
   * even when it is obviously invalid - otherwise the two error messages would swap.
   */
  @Test
  void testPostPassesOutOfRangeNumericRatingToTheService() {
    when(duosUser.getUser()).thenReturn(user);
    when(service.post(eq(1), eq(user), eq(9), any()))
        .thenThrow(new BadRequestException("Rating must be between 1 and 5."));

    Response response = resource.post(duosUser, 1, "{\"rating\": 9}");

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    verify(service).post(1, user, 9, null);
  }

  /** An absent or explicitly null rating is the service's range error, not a type error. */
  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"rating\": null}", "{\"commentText\": \"no rating\"}"})
  void testPostWithMissingRatingReachesTheServiceAsNull(String body) {
    when(duosUser.getUser()).thenReturn(user);
    when(service.post(eq(1), eq(user), eq(null), any()))
        .thenThrow(new BadRequestException("Rating must be between 1 and 5."));

    Response response = resource.post(duosUser, 1, body);

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    Error error = assertInstanceOf(Error.class, response.getEntity());
    assertEquals("Rating must be between 1 and 5.", error.message());
  }

  /**
   * A body that parses but is not a JSON object gets the payload message, never a Gson stacktrace.
   */
  @ParameterizedTest
  @ValueSource(strings = {"\"hello\"", "[]", "4", "true", "", "null", "{not json}"})
  void testPostWithNonObjectBodyIsABadRequest(String body) {
    Response response = resource.post(duosUser, 1, body);

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    Error error = assertInstanceOf(Error.class, response.getEntity());
    assertEquals("Comment payload must be a JSON object", error.message());
    verify(service, never()).post(any(), any(), any(), any());
  }

  @Test
  void testPostWithNonStringCommentTextIsABadRequest() {
    Response response = resource.post(duosUser, 1, "{\"rating\": 4, \"commentText\": {\"a\": 1}}");

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    verify(service, never()).post(any(), any(), any(), any());
  }

  @Test
  void testDeleteNotFound() {
    when(duosUser.getUser()).thenReturn(user);
    doThrow(new NotFoundException()).when(service).delete(1, 7, user);

    Response response = resource.delete(duosUser, 1, 7);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }
}
