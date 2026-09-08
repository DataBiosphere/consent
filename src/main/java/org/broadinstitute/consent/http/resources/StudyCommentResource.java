package org.broadinstitute.consent.http.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.StudyCommentService;

@Path("api/dataset/study/{studyId}/comments")
@Produces(MediaType.APPLICATION_JSON)
public class StudyCommentResource extends Resource {
  private static final String PAYLOAD_ERROR = "Comment payload must be a JSON object";
  private static final String RATING_TYPE_ERROR = "Rating must be a whole number between 1 and 5.";
  private static final String COMMENT_TYPE_ERROR = "Comment text must be a string.";

  private final StudyCommentService service;

  private record CommentPayload(Integer rating, String commentText) {}

  @Inject
  public StudyCommentResource(StudyCommentService service) {
    this.service = service;
  }

  /**
   * A page of the study's comments, newest first by creation.
   *
   * <p>Bounded by default: a study's comment count grows with its readers, so an unpaged list would
   * grow without limit. The average and total are computed across every comment, not the page, so
   * neither moves as a caller pages through.
   */
  @GET
  @PermitAll
  public Response list(
      @Auth DuosUser user,
      @PathParam("studyId") Integer studyId,
      @QueryParam("limit") Integer limit,
      @QueryParam("offset") Integer offset) {
    try {
      return Response.ok(service.list(studyId, user.getUser(), pageSize(limit), startingAt(offset)))
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private int pageSize(Integer limit) {
    if (limit == null) {
      return StudyCommentService.DEFAULT_PAGE_SIZE;
    }
    if (limit < 1 || limit > StudyCommentService.MAX_PAGE_SIZE) {
      throw new BadRequestException(
          "limit must be between 1 and %d".formatted(StudyCommentService.MAX_PAGE_SIZE));
    }
    return limit;
  }

  private int startingAt(Integer offset) {
    if (offset == null) {
      return 0;
    }
    if (offset < 0) {
      throw new BadRequestException("offset must not be negative");
    }
    return offset;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed({RESEARCHER})
  public Response post(@Auth DuosUser user, @PathParam("studyId") Integer studyId, String json) {
    try {
      CommentPayload payload = parsePayload(json);
      return Response.ok(
              service.post(studyId, user.getUser(), payload.rating(), payload.commentText()))
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Reads the body as a JSON object. Deserializing straight into the record would let Gson coerce
   * whatever it was given - a string "4" becomes the integer 4, a fraction is truncated - so the
   * rating is read out by hand and its JSON type asserted. Everything downstream sees an Integer,
   * and cannot tell a coerced value from one the client actually sent as a number.
   */
  private CommentPayload parsePayload(String json) {
    JsonElement element;
    try {
      element = JsonParser.parseString(json == null ? "" : json);
    } catch (JsonSyntaxException e) {
      throw new BadRequestException(PAYLOAD_ERROR);
    }
    if (element == null || !element.isJsonObject()) {
      throw new BadRequestException(PAYLOAD_ERROR);
    }
    JsonObject object = element.getAsJsonObject();
    return new CommentPayload(rating(object), commentText(object));
  }

  /**
   * A rating must be sent as a JSON number with no fractional part. An absent or null rating is
   * passed through as null so the service produces its own range message.
   */
  private Integer rating(JsonObject object) {
    JsonElement element = object.get("rating");
    if (element == null || element.isJsonNull()) {
      return null;
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      throw new BadRequestException(RATING_TYPE_ERROR);
    }
    try {
      return element.getAsBigDecimal().intValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      // Fractional, or too large to be an int.
      throw new BadRequestException(RATING_TYPE_ERROR);
    }
  }

  /**
   * Comment text must be sent as a JSON string. isJsonPrimitive alone is also true of numbers and
   * booleans, whose getAsString stored 123 as "123" and false as "false", so the type is asserted
   * the same way the rating's is.
   */
  private String commentText(JsonObject object) {
    JsonElement element = object.get("commentText");
    if (element == null || element.isJsonNull()) {
      return null;
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      throw new BadRequestException(COMMENT_TYPE_ERROR);
    }
    return element.getAsString();
  }

  /**
   * Removes a comment: the caller's own, or anyone's when the caller is an admin. Admins are the
   * moderation path over comments that every authenticated user can read. A user who has lost the
   * Researcher role can no longer delete, their own comment included.
   */
  @DELETE
  @Path("/{commentId}")
  @RolesAllowed({RESEARCHER, ADMIN})
  public Response delete(
      @Auth DuosUser user,
      @PathParam("studyId") Integer studyId,
      @PathParam("commentId") Integer commentId) {
    try {
      service.delete(studyId, commentId, user.getUser());
      return Response.noContent().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
