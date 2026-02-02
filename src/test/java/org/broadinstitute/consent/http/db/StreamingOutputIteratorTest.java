package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamingOutputIteratorTest extends AbstractTestHelper {

  private StreamingOutputIterator<JsonObject> streamingOutputIterator;

  @Mock private ResultIterable<JsonObject> resultIterable;

  @Mock private Handle handle;

  @BeforeEach
  void setUp() {
    streamingOutputIterator = new StreamingOutputIterator<>();
  }

  @Test
  void testStreamResults_EmptyResults() throws Exception {
    when(resultIterable.stream()).thenReturn(Stream.empty());

    StreamingOutput output = streamingOutputIterator.streamResults(resultIterable, handle);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String jsonString = baos.toString(StandardCharsets.UTF_8);

    JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
    assertEquals(0, jsonArray.size());
    verify(handle).close();
  }

  @Test
  void testStreamResults_SingleResult() throws Exception {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("id", "test-id");
    jsonObject.addProperty("label", "test-label");

    when(resultIterable.stream()).thenReturn(Stream.of(jsonObject));

    StreamingOutput output = streamingOutputIterator.streamResults(resultIterable, handle);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String jsonString = baos.toString(StandardCharsets.UTF_8);

    JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
    assertEquals(1, jsonArray.size());
    assertEquals("test-id", jsonArray.get(0).getAsJsonObject().get("id").getAsString());
    verify(handle).close();
  }

  @Test
  void testStreamResults_MultipleResults() throws Exception {
    JsonObject jsonObject1 = new JsonObject();
    jsonObject1.addProperty("id", "id-1");

    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.addProperty("id", "id-2");

    JsonObject jsonObject3 = new JsonObject();
    jsonObject3.addProperty("id", "id-3");

    when(resultIterable.stream()).thenReturn(Stream.of(jsonObject1, jsonObject2, jsonObject3));

    StreamingOutput output = streamingOutputIterator.streamResults(resultIterable, handle);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    output.write(baos);
    String jsonString = baos.toString(StandardCharsets.UTF_8);

    JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
    assertEquals(3, jsonArray.size());
    assertEquals("id-1", jsonArray.get(0).getAsJsonObject().get("id").getAsString());
    assertEquals("id-2", jsonArray.get(1).getAsJsonObject().get("id").getAsString());
    assertEquals("id-3", jsonArray.get(2).getAsJsonObject().get("id").getAsString());
    verify(handle).close();
  }

  @Test
  void testStreamResults_HandlesIterableException() {
    String errorMessage = "Test exception";
    when(resultIterable.stream()).thenThrow(new RuntimeException(errorMessage));
    try {
      streamingOutputIterator.streamResults(resultIterable, handle);
      fail("Expected ServerErrorException was not thrown");
    } catch (ServerErrorException e) {
      assertEquals(errorMessage, e.getMessage());
    }
    verify(handle).close();
  }

  @Test
  void testStreamResults_HandlesWriterException() throws Exception {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("id", "test-id");

    when(resultIterable.stream()).thenReturn(Stream.of(jsonObject));

    StreamingOutput output = streamingOutputIterator.streamResults(resultIterable, handle);

    java.io.OutputStream baos = mock(java.io.OutputStream.class);
    doThrow(new IOException("Write error")).when(baos).write(any(byte[].class), anyInt(), anyInt());

    assertThrows(ServerErrorException.class, () -> output.write(baos));
    verify(handle).close();
  }
}
