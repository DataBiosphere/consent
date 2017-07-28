package org.broadinstitute.consent.http.service.ontologyIndexer;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.elasticsearch.client.RestClient;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class IndexOntologyService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IndexOntologyService.class);

    static final String FIELD_DEFINITION_PROPERTY = "IAO_0000115";
    static final String FIELD_HAS_EXACT_SYNONYM_PROPERTY = "hasExactSynonym";
    static final String FIELD_LABEL_PROPERTY = "label";
    static final String FIELD_DEPRECATED_PROPERTY = "deprecated";
    private final String indexName;
    private IndexerUtils utils = new IndexerUtils();
    private final ElasticSearchConfiguration configuration;

    public IndexOntologyService(ElasticSearchConfiguration config) {
        this.configuration = config;
        this.indexName = config.getIndexName();
    }

    private RestClient getRestClient() {
        return ElasticSearchSupport.getRestClient(this.configuration);
    }

    /**
     * For each input stream, parse and upload ontology terms to the configured index.
     *
     * @param streamRecList List of StreamRec objects
     * @throws IOException The exception
     */
    public void indexOntologies(List<StreamRec> streamRecList) throws IOException {
        try(RestClient client = getRestClient()) {
            for (StreamRec streamRec : streamRecList) {

                // Deprecate everything that might already exist for this ontology file
                utils.bulkDeprecateTerms(client, indexName, streamRec.getOntologyType());

                //Just to be capable of read InputStream multiple times
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(streamRec.getStream(), baos);
                byte[] bytes = baos.toByteArray();

                OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(bytes));
                // Reset stream so it can be re-read by the storage service
                streamRec.setStream(new ByteArrayInputStream(bytes));

                OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
                OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

                HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();
                ontology.getAnnotationPropertiesInSignature().forEach((property) ->
                    annotationProperties.put(property.getIRI().getFragment(), property));

                // Some assertions to ensure we're not dealing with a problematic ontology file:
                if (!annotationProperties.containsKey(FIELD_HAS_EXACT_SYNONYM_PROPERTY)) {
                    logger.warn(streamRec.getFileName() + " is missing hasExactSynonym annotation property.");
                }
                if (!annotationProperties.containsKey(FIELD_LABEL_PROPERTY)) {
                    logger.warn(streamRec.getFileName() + " is missing label annotation property");
                }
                if (!annotationProperties.containsKey(FIELD_DEFINITION_PROPERTY)) {
                    logger.warn(streamRec.getFileName() + " is missing definition annotation property");
                }
                if (!annotationProperties.containsKey(FIELD_DEPRECATED_PROPERTY)) {
                    logger.warn(streamRec.getFileName() + " is missing deprecated annotation property");
                }

                Set<OWLClass> owlClasses = ontology.getClassesInSignature();
                Collection<Term> terms = owlClasses.stream().map(
                    (o) -> utils.generateTerm(o, streamRec.getOntologyType(), ontology, annotationProperties, reasoner)
                ).collect(Collectors.toList());

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
    public Boolean deprecateOntology(String ontologyType) throws IOException {
        try (RestClient client = getRestClient()) {
            utils.bulkDeprecateTerms(client, indexName, ontologyType);
        }
        return true;
    }

}
