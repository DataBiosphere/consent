package org.broadinstitute.consent.http.service.ontology;

import com.google.gson.JsonObject;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.StreamingOutputIterator;
import org.broadinstitute.consent.http.db.mapper.JsonMapper;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
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

  /**
   * Reconciles the ontology terms referenced by Datasets and DARs against the terms in the
   * ontology_index table. Returns one row per referenced term id that is either missing from the
   * index or present but flagged unusable. Dataset disease restrictions are read from the
   * dataset.data_use (text) column and DAR ontology entries from the data_access_request.data
   * (jsonb) column. Comparison is case-insensitive and whitespace-trimmed to mirror the lookup
   * behavior of {@link #findByTermIds(String[])}.
   */
  @RegisterConstructorMapper(OntologyReconciliationResult.class)
  @SqlQuery(
      """
          WITH referenced_terms AS (
              SELECT 'DATASET' AS source,
                     d.dataset_id::text AS source_id,
                     jsonb_array_elements_text(d.data_use::jsonb -> 'diseaseRestrictions') AS term_id
              FROM dataset d
              WHERE d.data_use IS NOT NULL
                AND jsonb_typeof(d.data_use::jsonb -> 'diseaseRestrictions') = 'array'
              UNION ALL
              SELECT 'DAR' AS source,
                     dar.reference_id AS source_id,
                     jsonb_array_elements(dar.data -> 'ontologies') ->> 'id' AS term_id
              FROM data_access_request dar
              WHERE jsonb_typeof(dar.data -> 'ontologies') = 'array'
          ),
          clean AS (
              SELECT source, source_id, term_id, LOWER(TRIM(term_id)) AS norm_id
              FROM referenced_terms
              WHERE term_id IS NOT NULL AND TRIM(term_id) <> ''
          )
          SELECT
              c.term_id AS term_id,
              CASE WHEN oi.id IS NULL THEN 'MISSING_FROM_INDEX'
                   WHEN oi.usable IS NOT TRUE THEN 'PRESENT_BUT_UNUSABLE' END AS issue,
              oi.ontology AS ontology,
              COUNT(*) AS reference_count,
              COUNT(*) FILTER (WHERE c.source = 'DATASET') AS dataset_refs,
              COUNT(*) FILTER (WHERE c.source = 'DAR') AS dar_refs,
              STRING_AGG(DISTINCT c.source || ':' || c.source_id, ', ') AS referenced_by
          FROM clean c
          LEFT JOIN ontology_index oi ON LOWER(TRIM(oi.id)) = c.norm_id
          WHERE oi.id IS NULL OR oi.usable IS NOT TRUE
          GROUP BY c.term_id, oi.id, oi.usable, oi.ontology
          ORDER BY issue, reference_count DESC
          """)
  List<OntologyReconciliationResult> findReferencedTermsMissingFromIndex();

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
    String formattedTerm = sanitizeForTsQuery(term);
    if (formattedTerm.isBlank()) {
      return outputStream -> outputStream.write("[]".getBytes());
    }
    String[] defaultTypes =
        type != null
            ? new String[] {type.name().toLowerCase()}
            : EnumSet.allOf(OntologyType.class).stream()
                .map(t -> t.name().toLowerCase())
                .toArray(String[]::new);
    Integer defaultCount = (count == null || count < 1) ? 20 : count;
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
                  .bind("term", formattedTerm)
                  .bind("count", defaultCount)
                  .map(new JsonMapper("json_document"));
          StreamingOutputIterator<JsonObject> iterator = new StreamingOutputIterator<>();
          return iterator.streamResults(results, handle);
        });
  }

  /**
   * Sanitizes a string for use in a postgres to_tsquery statement. Removes non-alphanumeric
   * characters and formats tokens for prefix matching.
   *
   * @param term The user input string
   * @return A formatted tsquery string (e.g., "term1:* | term2:*")
   */
  default String sanitizeForTsQuery(String term) {
    if (term == null || term.isBlank()) {
      return "";
    }
    // Remove characters that conflict with tsquery syntax (keep alphanumeric and whitespace)
    String cleanTerm = term.replaceAll("[^a-zA-Z0-9\\s]", "");

    return Arrays.stream(cleanTerm.trim().split("\\s+"))
        .map(s -> s + ":*")
        .collect(Collectors.joining(" | "));
  }
}
