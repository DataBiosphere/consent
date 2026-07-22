package org.broadinstitute.consent.http.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotFoundExceptionMapperTest {

  @Mock private UriInfo uriInfo;

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

  @Test
  void testExceptionWithMessage() {
    NotFoundExceptionMapper mapper = new NotFoundExceptionMapper();
    try (Response response = mapper.toResponse(new NotFoundException("Dataset not found"))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      assertTrue(response.getEntity().toString().contains("Dataset not found"));
    }
  }
}
