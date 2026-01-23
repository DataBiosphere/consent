package org.broadinstitute.consent.http.service.ontology;

import java.util.Collection;
import java.util.List;
import org.jdbi.v3.core.Handle;
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

  default List<String> findByTerms(Collection<String> terms) {
    String query =
        """
        SELECT json_document
        FROM ontology_index, to_tsquery('english', :term) query
        WHERE search_vector @@ query
        """;
    return inTransaction(
        dao -> {
          Handle handle = getHandle();
          return handle
              .createQuery(query)
              .bind("term", String.join(" | ", terms))
              .mapTo(String.class)
              .list();
        });
  }
}
