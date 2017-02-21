package org.broadinstitute.consent.http.service.ontologyIndexer;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.Client;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class IndexOntologyService {

    static final String FIELD_DEFINITION_PROPERTY = "IAO_0000115";
    static final String FIELD_HAS_EXACT_SYNONYM_PROPERTY = "hasExactSynonym";
    static final String FIELD_LABEL_PROPERTY = "label";
    static final String FIELD_DEPRECATED_PROPERTY = "deprecated";
    private final Client client;
    private final String indexName;
    private IndexerUtils utils = new IndexerUtils();

    public IndexOntologyService(Client client, String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }

    /**
     * For each input stream, parse and upload ontology terms to the configured index.
     *
     * @param streamRecList List of StreamRec objects
     * @throws IOException The exception
     */
    public void indexOntologies(List<StreamRec> streamRecList) throws IOException {

        utils.validateIndexExists(client, indexName);

        try {
            for (StreamRec streamRec : streamRecList) {

                // Remove any terms that already exist for this type. Type in this context refers to "Disease" or "Organization", etc.
                utils.deleteByOntologyType(client, indexName, streamRec.getOntologyType());

                OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                OWLOntology ontology = manager.loadOntologyFromOntologyDocument(streamRec.getStream());

                OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
                OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

                HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();
                ontology.getAnnotationPropertiesInSignature().forEach((property) ->
                    annotationProperties.put(property.getIRI().getFragment(), property));

                // Some assertions to ensure we're not dealing with a problematic ontology file:
                assert annotationProperties.get(FIELD_HAS_EXACT_SYNONYM_PROPERTY) != null : "Need hasExactSynonym annotation property.";
                assert annotationProperties.get(FIELD_LABEL_PROPERTY) != null : "Need label annotation property";
                assert annotationProperties.get(FIELD_DEFINITION_PROPERTY) != null : "Need definition annotation property";
                assert annotationProperties.get(FIELD_DEPRECATED_PROPERTY) != null : "Need deprecated annotation property";

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

    Boolean deleteOntologiesByFile(InputStream fileStream, String prefix) {

        Boolean atLeastOneDeletion = false;
        List<String> toDeleteIds = new ArrayList<>();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
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
            if (CollectionUtils.isEmpty(toDeleteIds)) return atLeastOneDeletion;

            BulkRequestBuilder bulk = client.prepareBulk();
            for (String id : toDeleteIds) {
                DeleteRequestBuilder deleteRequestBuilder =
                    client.prepareDelete(indexName, "ontology_term", id);
                bulk.add(deleteRequestBuilder);
            }

            bulk.execute().actionGet();
            atLeastOneDeletion = true;
            return atLeastOneDeletion;

        } catch (OWLOntologyCreationException e) {
            throw new BadRequestException("Problem with OWL file.");
        }
    }

}
