package org.broadinstitute.consent.http.service.ontology;

import com.google.gson.JsonObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface OntologyDAO extends Transactional<OntologyDAO> {

  @SqlBatch(
      """
      INSERT INTO ontology_index (term_id, version, ontology, synonyms, label, definition, usable, obo_id, json_document, create_user_id)
      VALUES (:termId, :version, :ontology, :synonyms, :label, :definition, :usable, :oboId, :jsonDocument::jsonb, :userId)
    """)
  void batchInsertTerms(
      @BindMethods Collection<OntologyTerm> terms, @Bind("userId") Integer userId);

  @SqlQuery("SELECT COUNT(*) FROM ontology_index")
  int countTerms();

  @Json
  default List<JsonObject> findByTerms(Collection<String> terms) {
    String query =
        """
        SELECT json_document
        FROM ontology_index, plainto_tsquery('english', :term) query
        WHERE search_vector @@ query
        """;
    return inTransaction(
        dao -> {
          Handle handle = getHandle();
          return handle
              .createQuery(query)
              .bind("term", String.join(" | ", terms))
              .map(new JsonMapper())
              .list();
        });
  }

  class JsonMapper implements RowMapper<JsonObject> {
    @Override
    public JsonObject map(ResultSet rs, StatementContext ctx) throws SQLException {
      String json = rs.getString("json_document");
      return GsonUtil.getInstance().fromJson(json, JsonObject.class);
    }
  }
}
