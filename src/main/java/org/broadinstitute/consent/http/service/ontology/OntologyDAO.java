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
      INSERT INTO ontology_index (id, version, ontology, synonyms, label, definition, usable, obo_id, json_document, create_user_id)
      VALUES (:id, :version, :ontology, :synonyms, :label, :definition, :usable, :oboId, :jsonDocument::jsonb, :userId)
      ON CONFLICT (id) DO NOTHING
    """)
  void batchInsertTerms(
      @BindMethods Collection<OntologyTerm> terms, @Bind("userId") Integer userId);

  @SqlQuery("SELECT COUNT(*) FROM ontology_index")
  int countTerms();

  @Json
  default List<JsonObject> findByIds(Collection<String> ids) {
    String query =
        """
        SELECT json_document
        FROM ontology_index
        WHERE LOWER(id) = ANY (:ids)
           OR LOWER(obo_id) = ANY (:ids)
        """;
    String[] idArray = ids.stream().map(term -> term.trim().toLowerCase()).toArray(String[]::new);
    return inTransaction(
        _ -> {
          Handle handle = getHandle();
          return handle.createQuery(query).bindArray("ids", idArray).map(new JsonMapper()).list();
        });
  }

  class JsonMapper implements RowMapper<JsonObject> {
    @Override
    public JsonObject map(ResultSet rs, StatementContext ctx) throws SQLException {
      return GsonUtil.getInstance().fromJson(rs.getString("json_document"), JsonObject.class);
    }
  }
}
