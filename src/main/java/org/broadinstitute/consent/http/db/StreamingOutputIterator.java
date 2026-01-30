package org.broadinstitute.consent.http.db;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterable;

public class StreamingOutputIterator {

  /**
   * Streams the results from a ResultIterable as a JSON array.
   *
   * @param resultIterable the iterable result set to stream. Requires mapping to JsonObject.
   * @param handle the JDBI handle to close after streaming.
   * @return a StreamingOutput that writes the JSON array to the output stream.
   */
  public StreamingOutput streamResults(ResultIterable<JsonObject> resultIterable, Handle handle) {
    try (handle) {
      Iterator<JsonObject> resultIterator = resultIterable.stream().iterator();
      return output -> {
        try (JsonWriter writer =
            new JsonWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
          writer.beginArray();
          resultIterator.forEachRemaining(
              term -> {
                try {
                  writer.jsonValue(term.toString());
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
          writer.endArray();
        } catch (Exception e) {
          throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
      };
    } catch (Exception e) {
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }
}
