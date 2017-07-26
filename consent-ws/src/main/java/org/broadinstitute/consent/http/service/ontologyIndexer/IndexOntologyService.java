package org.broadinstitute.consent.http.service.ontologyIndexer;

import org.apache.commons.collections.CollectionUtils;
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
import java.io.InputStream;
import java.util.*;
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
            utils.validateIndexExists(client, indexName);
            for (StreamRec streamRec : streamRecList) {

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

    public Boolean deleteOntologiesByFile(InputStream fileStream, String prefix) throws IOException {
        List<String> toDeleteIds = new ArrayList<>();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try (RestClient client = getRestClient()) {
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(fileStream);
            HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();
            ontology.getAnnotationPropertiesInSignature().forEach((property) ->
                annotationProperties.put(property.getIRI().getFragment(), property));
            for (OWLClass owlClass : ontology.getClassesInSignature()) {
                OWLAnnotationValueVisitorEx<String> visitor = new OWLAnnotationValueVisitorEx<String>() {
                    @Override
                    public String visit(IRI iri) {
                        return iri.toString();
                    }

                    @Override
                    public String visit(OWLAnonymousIndividual owlAnonymousIndividual) {
                        return owlAnonymousIndividual.toStringID();
                    }

                    @Override
                    public String visit(OWLLiteral owlLiteral) {
                        return owlLiteral.getLiteral();
                    }
                };

                Set<OWLAnnotation> ids = owlClass.getAnnotations(ontology, annotationProperties.get("id"));
                if (ids.size() != 1 || !ids.iterator().next().getValue().accept(visitor).startsWith(prefix)) {
                    continue;
                }
                toDeleteIds.add(owlClass.toStringID());
            }
            // Nothing to delete, return.
            if (CollectionUtils.isEmpty(toDeleteIds)) return true;

            // Else, iterate over terms to remove from the index.
            return utils.bulkDeleteTerms(client, indexName, toDeleteIds);
        } catch (OWLOntologyCreationException e) {
            throw new BadRequestException("Problem with OWL file.");
        }
    }

}
