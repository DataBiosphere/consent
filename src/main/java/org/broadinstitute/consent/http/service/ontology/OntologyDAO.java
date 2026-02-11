package org.broadinstitute.consent.http.service.ontology;

import com.google.gson.JsonObject;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import org.broadinstitute.consent.http.db.StreamingOutputIterator;
import org.broadinstitute.consent.http.db.mapper.JsonMapper;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
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

  @SqlUpdate("DELETE FROM ontology_index where ontology = :ontology")
  void deleteByOntology(@Bind("ontology") String ontology);

  @Json
  default StreamingOutput findByTermIds(@Bind("ids") String[] ids) {
    String query =
        """
            SELECT json_document
            FROM ontology_index
            WHERE LOWER(id) = ANY (:ids)
               OR LOWER(obo_id) = ANY (:ids)
            """;
    String[] lowerIds =
        Arrays.stream(ids).map(String::toLowerCase).map(String::trim).toArray(String[]::new);
    return withHandle(
        handle -> {
          ResultIterable<JsonObject> results =
              handle
                  .createQuery(query)
                  .bindArray("ids", lowerIds)
                  .map(new JsonMapper("json_document"));
          StreamingOutputIterator<JsonObject> iterator = new StreamingOutputIterator<>();
          return iterator.streamResults(results, handle);
        });
  }

  @Json
  default StreamingOutput findByQuery(String term, OntologyType type, Integer count) {
    String[] defaultTypes =
        type != null
            ? new String[] {type.name().toLowerCase()}
            : EnumSet.allOf(OntologyType.class).stream()
                .map(t -> t.name().toLowerCase())
                .toArray(String[]::new);
    String query =
        """
            SELECT json_document, ts_rank(search_vector, query) AS rank
              FROM ontology_index, to_tsquery('english', :term) query
             WHERE search_vector @@ query
               AND LOWER(ontology) = ANY (:types)
             ORDER BY rank DESC
             LIMIT :count
            """;
    return withHandle(
        handle -> {
          ResultIterable<JsonObject> results =
              handle
                  .createQuery(query)
                  .bindArray("types", defaultTypes)
                  .bind("term", term.toLowerCase().trim().replaceAll("\\s+", " \\| "))
                  .bind("count", count)
                  .map(new JsonMapper("json_document"));
          StreamingOutputIterator<JsonObject> iterator = new StreamingOutputIterator<>();
          return iterator.streamResults(results, handle);
        });
  }
}
