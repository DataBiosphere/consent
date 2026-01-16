package org.broadinstitute.consent.http.service.ontology;

import java.util.Collection;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface OntologyDAO extends Transactional<OntologyDAO> {

  @SqlBatch(
      """
      INSERT INTO ontology_index (term_id, version, ontology, synonyms, label, definition, usable, json_document, create_user_id)
      VALUES (:termId, :version, :ontology, :synonyms, :label, :definition, :usable, :jsonDocument::jsonb, :userId)
    """)
  void batchInsertTerms(
      @BindMethods Collection<OntologyTerm> terms, @Bind("userId") Integer userId);

  @SqlQuery("SELECT COUNT(*) FROM ontology_index")
  int countTerms();
}
