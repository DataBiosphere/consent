package org.broadinstitute.consent.http.service.ontology;

import java.util.Collection;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface OntologyDAO extends Transactional<OntologyDAO> {

  default void insertTerms(User user, Collection<OntologyTerm> terms) {
    String insertTerm =
        """
              INSERT INTO ontology_index (term_id, version, ontology, synonyms, label, usable, json, create_user_id)
              VALUES (:termId, :version, :ontology, :synonyms, :label, :usable, :json::jsonb, :userId)
        """;
    for (OntologyTerm term : terms) {
      inTransaction(
          _ -> {
            Handle handle = getHandle();
            try {
              handle
                  .createUpdate(insertTerm)
                  .bind("termId", term.termId)
                  .bind("version", term.version)
                  .bind("ontology", term.ontology)
                  .bind("synonyms", String.join(" ", term.synonyms))
                  .bind("label", term.label)
                  .bind("usable", term.usable)
                  .bind("json", term.toString())
                  .bind("userId", user.getUserId())
                  .execute();
              handle.commit();
            } catch (Exception e) {
              // Log and continue
              System.out.println("Failed to insert term: " + e.getMessage());
            }
            return null;
          });
    }
  }

  @SqlQuery("SELECT COUNT(*) FROM ontology_index")
  int countTerms();

}
