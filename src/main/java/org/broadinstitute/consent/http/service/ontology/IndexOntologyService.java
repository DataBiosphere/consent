package org.broadinstitute.consent.http.service.ontology;

import io.dropwizard.lifecycle.Managed;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.elasticsearch.client.RestClient;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class IndexOntologyService implements Managed {

    private final String indexName;
    private IndexerUtils utils = new IndexerUtils();
    private RestClient client;

    @Override
    public void start() throws Exception { }

    @Override
    public void stop() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    public IndexOntologyService(ElasticSearchConfiguration config) {
        this.indexName = config.getIndexName();
        this.client = ElasticSearchSupport.createRestClient(config);
    }

    /**
     * For each input stream, parse and upload ontology terms to the configured index.
     *
     * @param streamRecList List of StreamRec objects
     * @throws IOException The exception
     */
    public void indexOntologies(List<StreamRec> streamRecList) throws IOException {
        utils.checkIndex(client, indexName);
        try {
            for (StreamRec streamRec : streamRecList) {
                // Deprecate everything that might already exist for this ontology file
                utils.bulkDeprecateTerms(client, indexName, streamRec.getOntologyType());
                Collection<Term> terms = utils.generateTerms(streamRec);
                Boolean successfulUpload = utils.bulkUploadTerms(client, indexName, terms);
                streamRec.setAtLeastOneOntologyIndexed(successfulUpload);

            }
        } catch (OWLOntologyCreationException e) {
            throw new BadRequestException("Problem with OWL file.");
        }

    }

    /**
     * Deprecate any indexed terms for the specified type
     *
     * @param ontologyType The ontology type (e.g. "Disease", or "Organization") to mark as deprecated (i.e. usable=false)
     * @return True if no errors, exception otherwise.
     * @throws IOException The exception
     */
    Boolean deprecateOntology(String ontologyType) throws IOException {
        utils.bulkDeprecateTerms(client, indexName, ontologyType);
        return true;
    }

}
