package org.broadinstitute.consent.http.mappers;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.Callback;

/**
 * A request only reaches this Jetty-level handler when it never entered Jersey's JAX-RS dispatch at
 * all - most notably, a path that matches no {@code @Path} template. Jersey's own {@link
 * jakarta.ws.rs.ext.ExceptionMapper}s (see {@link NotFoundExceptionMapper}, {@link
 * ForbiddenExceptionMapper}) cannot intercept that case, since it never becomes a JAX-RS exception;
 * Jetty resolves it directly via {@code HttpServletResponse.sendError}. This handler is the
 * last-resort net that keeps that response in the app's standard {@link Error} JSON shape instead
 * of Jetty's own error page format, ignoring the client's Accept header since this is a JSON API
 * with no HTML or plain-text representation to negotiate to.
 */
public class JsonErrorHandler extends ErrorHandler {

  @Override
  protected void generateResponse(
      Request request,
      Response response,
      int code,
      String message,
      Throwable cause,
      Callback callback)
      throws IOException {
    generateAcceptableResponse(
        request,
        response,
        callback,
        "application/json",
        List.of(StandardCharsets.UTF_8),
        code,
        message,
        cause);
  }

  @Override
  protected void writeErrorJson(
      Request request, PrintWriter writer, int code, String message, Throwable cause) {
    boolean isGenericMessage =
        message == null || message.isBlank() || message.equals(HttpStatus.getMessage(code));
    String finalMessage;
    if (code == 404 && isGenericMessage) {
      finalMessage = "Unable to find requested path: " + request.getHttpURI().getPath();
    } else if (isGenericMessage) {
      finalMessage = HttpStatus.getMessage(code);
    } else {
      finalMessage = message;
    }
    writer.write(GsonUtil.getInstance().toJson(new Error(finalMessage, code)));
  }
}
