package org.broadinstitute.consent.http.service.ontology;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import org.broadinstitute.consent.http.db.mapper.JsonMapper;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface OntologyDAO extends Transactional<OntologyDAO> {

  @SqlBatch(
      """
            INSERT INTO ontology_index (id, version, ontology, synonyms, label, definition, usable, obo_id, json_document, create_user_id)
            VALUES (:id, :version, :ontology, :synonyms, :label, :definition, :usable, :oboId, :jsonDocument::jsonb, :userId)
            ON CONFLICT (id) DO NOTHING
          """)
  @BatchChunkSize(100)
  void batchInsertTerms(
      @BindMethods Collection<OntologyTerm> terms, @Bind("userId") Integer userId);

  @SqlQuery("SELECT COUNT(*) FROM ontology_index")
  int countTerms();

  @Json
  default StreamingOutput findByTermIds(@Bind("ids") String[] ids) {
    String query =
        """
            SELECT json_document
            FROM ontology_index
            WHERE LOWER(id) = ANY (:ids)
               OR LOWER(obo_id) = ANY (:ids)
            """;
    String[] lowerIds = Arrays.stream(ids).map(String::toLowerCase).toArray(String[]::new);
    return withHandle(
        handle -> {
          ResultIterable<JsonObject> results =
              handle
                  .createQuery(query)
                  .bindArray("ids", lowerIds)
                  .map(new JsonMapper("json_document"));
          Iterator<JsonObject> terms = results.stream().iterator();
          return output -> {
            try (JsonWriter writer =
                new JsonWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
              writer.beginArray();
              terms.forEachRemaining(
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
            } finally {
              handle.close();
            }
          };
        });
  }
}
