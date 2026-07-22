package org.broadinstitute.consent.http.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonErrorHandlerTest {

  @Mock private Request request;

  private JsonObject invokeWriteErrorJson(JsonErrorHandler handler, int code, String message) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    handler.writeErrorJson(request, pw, code, message, null);
    pw.flush();
    return JsonParser.parseString(sw.toString()).getAsJsonObject();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"Not Found", " "})
  void testWriteErrorJson_404WithoutMeaningfulMessage_usesRequestPath(String message) {
    String path = "/api/datasets/unknown-dataset";
    when(request.getHttpURI()).thenReturn(HttpURI.from("http://localhost" + path));

    JsonErrorHandler handler = new JsonErrorHandler();
    JsonObject json = invokeWriteErrorJson(handler, 404, message);

    assertEquals(404, json.get("code").getAsInt());
    assertEquals("Unable to find requested path: " + path, json.get("message").getAsString());
  }

  @Test
  void testWriteErrorJson_404WithCustomMessage_preservesMessage() {
    JsonErrorHandler handler = new JsonErrorHandler();
    JsonObject json = invokeWriteErrorJson(handler, 404, "Dataset not found");

    assertEquals(404, json.get("code").getAsInt());
    assertEquals("Dataset not found", json.get("message").getAsString());
  }

  @Test
  void testWriteErrorJson_500WithMessage_usesSuppliedMessage() {
    JsonErrorHandler handler = new JsonErrorHandler();
    JsonObject json = invokeWriteErrorJson(handler, 500, "Internal Server Error");

    assertEquals(500, json.get("code").getAsInt());
    assertEquals("Internal Server Error", json.get("message").getAsString());
  }

  @Test
  void testWriteErrorJson_403WithMessage_usesSuppliedMessage() {
    JsonErrorHandler handler = new JsonErrorHandler();
    JsonObject json = invokeWriteErrorJson(handler, 403, "Forbidden");

    assertEquals(403, json.get("code").getAsInt());
    assertEquals("Forbidden", json.get("message").getAsString());
  }

  @Test
  void testWriteErrorJson_outputIsValidJson() {
    JsonErrorHandler handler = new JsonErrorHandler();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    handler.writeErrorJson(request, pw, 400, "Bad Request", null);
    pw.flush();

    // Must parse without throwing
    JsonObject json = JsonParser.parseString(sw.toString()).getAsJsonObject();
    assertEquals(2, json.size());
  }
}
