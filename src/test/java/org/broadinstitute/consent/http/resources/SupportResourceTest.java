package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
        """.formatted(type.name());
    when(supportRequestService.postTicketToSupport(any())).thenReturn(new Request());
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testPostRequestInvalidName() {
    String body = """
        {
          "email": "test.user@example.com",
          "subject": "Test Subject",
          "description": "Test Description",
          "type": "QUESTION",
          "url": "https://example.com",
          "uploads": [
            "token1",
            "token2",
          ]
        }
        """;
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPostRequestInvalidEmail() {
    String body = """
        {
          "name": "Test User",
          "subject": "Test Subject",
          "description": "Test Description",
          "type": "QUESTION",
          "url": "https://example.com",
          "uploads": [
            "token1",
            "token2",
          ]
        }
        """;
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPostRequestInvalidSubject() {
    String body = """
        {
          "name": "Test User",
          "email": "test.user@example.com",
          "description": "Test Description",
          "type": "QUESTION",
          "url": "https://example.com",
          "uploads": [
            "token1",
            "token2",
          ]
        }
        """;
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPostRequestInvalidDescription() {
    String body = """
        {
          "name": "Test User",
          "email": "test.user@example.com",
          "subject": "Test Subject",
          "type": "QUESTION",
          "url": "https://example.com",
          "uploads": [
            "token1",
            "token2",
          ]
        }
        """;
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPostRequestInvalidType() {
    String body = """
        {
          "name": "Test User",
          "email": "test.user@example.com",
          "subject": "Test Subject",
          "description": "Test Description",
          "type": "type",
          "url": "https://example.com",
          "uploads": [
            "token1",
            "token2",
          ]
        }
        """;
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPostRequestInvalidURL() {
    String body = """
        {
          "name": "Test User",
          "email": "test.user@example.com",
          "subject": "Test Subject",
          "description": "Test Description",
          "type": "QUESTION",
          "uploads": [
            "token1",
            "token2",
          ]
        }
        """;
    try (Response response = supportResource.postRequest(body)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  public void testPostUpload() throws Exception {
    JsonObject obj = new JsonObject();
    obj.add("token", new JsonPrimitive("token value"));
    when(supportRequestService.postAttachmentToSupport(any())).thenReturn(obj);
    try (Response response = supportResource.postUpload("test".getBytes())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
  }

}
