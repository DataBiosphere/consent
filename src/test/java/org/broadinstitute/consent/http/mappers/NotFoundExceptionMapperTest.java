package org.broadinstitute.consent.http.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.broadinstitute.consent.http.models.Error;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotFoundExceptionMapperTest {

  @Mock private UriInfo uriInfo;

  // Covers message.equals(GENERIC_MESSAGE) branch – no-arg constructor synthesises "HTTP 404 Not
  // Found".
  @ParameterizedTest
  @ValueSource(strings = {"/not_found", "/context/¥"})
  void testMessagelessException(String path) {
    NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
    mapper.uriInfo = uriInfo;
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost" + path));
    try (Response response = mapper.toResponse(new NotFoundException())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      assertTrue(response.getEntity().toString().contains(path));
    }
  }

  // Covers message == null branch.
  @Test
  void testNullMessage() {
    String path = "/api/datasets/123";
    NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
    mapper.uriInfo = uriInfo;
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost" + path));
    try (Response response = mapper.toResponse(new NotFoundException((String) null))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      assertEquals(
          "Unable to find requested path: " + path, ((Error) response.getEntity()).message());
    }
  }

  // Covers message.isBlank() branch – empty string and whitespace-only string.
  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void testBlankMessage(String blank) {
    String path = "/api/datasets/123";
    NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
    mapper.uriInfo = uriInfo;
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost" + path));
    try (Response response = mapper.toResponse(new NotFoundException(blank))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      assertEquals(
          "Unable to find requested path: " + path, ((Error) response.getEntity()).message());
    }
  }

  // Covers the else branch – a real message must be passed through unchanged.
  @Test
  void testExceptionWithMessage() {
    NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
    try (Response response = mapper.toResponse(new NotFoundException("Dataset not found"))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      assertTrue(response.getEntity().toString().contains("Dataset not found"));
    }
  }
}
