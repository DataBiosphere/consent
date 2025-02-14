package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zendesk.client.v2.model.Request;

@ExtendWith(MockitoExtension.class)
public class SupportResourceTest extends ResourceTest {

  @Mock
  private SupportRequestService supportRequestService;

  private SupportResource supportResource;

  @BeforeEach
  public void setUp() {
    supportResource = new SupportResource(supportRequestService);
  }

  @ParameterizedTest
  @EnumSource(SupportRequestType.class)
  void testPostRequestSuccess(SupportRequestType type) throws Exception {
    String body = """
        {
          "name": "Test User",
          "email": "test.user@example.com",
          "subject": "Test Subject",
          "description": "Test Description",
          "type": "%s",
          "url": "https://example.com",
          "uploads": [
            "token1",
            "token2",
          ]
        }
        """.formatted(type);
    when(supportRequestService.postTicketToSupport(any())).thenReturn(new Request());
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
  }

  private static Stream<Arguments> testPostRequestInvalidFields() {
    return Stream.of(
        Arguments.of("""
            {
              "email": "test.user@example.com",
              "subject": "Test Subject",
              "description": "Test Description",
              "type": "QUESTION",
              "url": "https://example.com",
              "uploads": ["token1", "token2"]
            }
            """),
        Arguments.of("""
            {
              "name": "Test User",
              "subject": "Test Subject",
              "description": "Test Description",
              "type": "QUESTION",
              "url": "https://example.com",
              "uploads": ["token1", "token2"]
            }
            """),
        Arguments.of("""
            {
              "name": "Test User",
              "email": "test.user@example.com",
              "description": "Test Description",
              "type": "QUESTION",
              "url": "https://example.com",
              "uploads": ["token1", "token2"]
            }
            """),
        Arguments.of("""
            {
              "name": "Test User",
              "email": "test.user@example.com",
              "subject": "Test Subject",
              "type": "QUESTION",
              "url": "https://example.com",
              "uploads": ["token1", "token2"]
            }
            """),
        Arguments.of("""
            {
              "name": "Test User",
              "email": "test.user@example.com",
              "subject": "Test Subject",
              "description": "Test Description",
              "url": "https://example.com",
              "uploads": ["token1", "token2"]
            }
            """),
        Arguments.of("""
            {
              "name": "Test User",
              "email": "test.user@example.com",
              "subject": "Test Subject",
              "description": "Test Description",
              "type": "QUESTION",
              "uploads": ["token1", "token2"]
            }
            """)
    );
  }

  @ParameterizedTest
  @MethodSource
  void testPostRequestInvalidFields(String body) {
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUnprocessableTicket() throws Exception {
    doThrow(new UnprocessableEntityException("Unprocessable")).when(supportRequestService)
        .postTicketToSupport(any());
    try (Response response = supportResource.postRequest("""
        {
          "name": "Test User",
          "email": "test.user@example.com",
          "subject": "Test Subject",
          "description": "Test Description",
          "type": "QUESTION",
          "url": "https://example.com",
          "uploads": ["token1", "token2"]
        }
        """)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY, response.getStatus());
    }
  }

  @Test
  void testPostUpload() throws Exception {
    JsonObject obj = new JsonObject();
    obj.add("token", new JsonPrimitive("token value"));
    when(supportRequestService.postAttachmentToSupport(any())).thenReturn(obj);
    try (Response response = supportResource.postUpload("test".getBytes())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
  }

  @Test
  void testUnprocessableUpload() throws Exception {
    doThrow(new UnprocessableEntityException("Unprocessable")).when(supportRequestService)
        .postAttachmentToSupport(any());
    try (Response response = supportResource.postUpload("test".getBytes())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY, response.getStatus());
    }
  }

}
