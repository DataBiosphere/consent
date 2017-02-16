package org.broadinstitute.consent.http.service.ontologyIndexer;

import com.google.common.collect.Lists;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A suite of functions for handling index functions.
 */
public class IndexerUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IndexerUtils.class);
    // Parents and children may contain these unhelpful nodes:
    private static OWLDataFactory owlDataFactory = new OWLDataFactoryImpl();
    private static final OWLClass THING = owlDataFactory.getOWLThing();
    private static final OWLClass NOTHING = owlDataFactory.getOWLNothing();


    /**
     * Check to see if the index exists and create it otherwise.
     *
     * @throws InternalServerErrorException The exception
     */
    public void validateIndexExists(Client client, String indexName) throws InternalServerErrorException {
        IndicesExistsRequest existsRequest = new IndicesExistsRequest(indexName);
        if (!client.admin().indices().exists(existsRequest).actionGet().isExists()) {
            CreateIndexRequest indexRequest = new CreateIndexRequest(indexName);
            try {
                client.admin().indices().create(indexRequest).actionGet();
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new InternalServerErrorException();
            }
        }
    }

    /**
     * Delete all terms of a specific type (i.e. "Disease", "Organization")
     *
     * @param type The type
     */
    public void deleteByOntologyType(Client client, String indexName, String type) {
        validateIndexExists(client, indexName);
        DeleteByQueryRequestBuilder deleteByQuery = client.prepareDeleteByQuery(indexName);
        QueryBuilder queryBuilder = QueryBuilders.matchQuery(Term.FIELD_ONTOLOGY_TYPE, type);
        deleteByQuery.setQuery(queryBuilder);
        deleteByQuery.execute().actionGet();
    }

    /**
     * Generate a search Term that will be pushed to Elastic Search
     *
     * @param owlClass The OWLClass to generate Term content from
     * @param ontologyType The type (i.e. "Disease", "Organization")
     * @param ontology OWL API class that manages all OWL related functions
     * @param annotationProperties Map of annotation properties for given ontology.
     * @param reasoner OWLReasoner
     * @return Term
     */
    public Term generateTerm(OWLClass owlClass, String ontologyType, OWLOntology ontology,
                              HashMap<String, OWLAnnotationProperty> annotationProperties,
                              OWLReasoner reasoner) {

        OWLAnnotationProperty hasExactSynonym = annotationProperties.get(IndexOntologyService.FIELD_HAS_EXACT_SYNONYM_PROPERTY);
        OWLAnnotationProperty label = annotationProperties.get(IndexOntologyService.FIELD_LABEL_PROPERTY);
        OWLAnnotationProperty definition = annotationProperties.get(IndexOntologyService.FIELD_DEFINITION_PROPERTY);
        OWLAnnotationProperty deprecated = annotationProperties.get(IndexOntologyService.FIELD_DEPRECATED_PROPERTY);
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

        Term term = new Term(owlClass.toStringID(), ontologyType);
        if (!owlClass.getAnnotations(ontology, deprecated).isEmpty()) {
            term.setUsable(false);
        }

        owlClass.getAnnotations(ontology, hasExactSynonym).forEach((synonyms) ->
            term.addSynonym(synonyms.getValue().accept(visitor)));

        Set<OWLAnnotation> labels = owlClass.getAnnotations(ontology, label);
        assert labels.size() <= 1 : "Exactly 0 or 1 labels allowed per class";
        if (labels.size() == 1) {
            term.addLabel(labels.iterator().next().getValue().accept(visitor));
        }

        Set<OWLAnnotation> definitions = owlClass.getAnnotations(ontology, definition);
        assert definitions.size() <= 1 : "Exactly 0 or 1 definitions allowed per class";
        if (definitions.size() == 1) {
            term.addDefinition(definitions.iterator().next().getValue().accept(visitor));
        }

        int position = 0;
        for (OWLClass parent : getParents(owlClass, reasoner)) {
            if (parent.isOWLClass() && !parent.equals(THING) && !parent.equals(NOTHING)) {
                position ++;
                term.addParent(parent.toStringID(), position);
            }
        }

        return term;

    }

    /**
     * Recursively generate an ordered list of OWLClass Parents.
     *
     * @param owlClass The class to find parents for
     * @param reasoner Reasoner required to make inferences.
     * @return List of ordered OWLClass parents
     */
    public List<OWLClass> getParents(OWLClass owlClass, OWLReasoner reasoner) {
        List<OWLClass> parents = new ArrayList<>();
        Set<OWLClass> parentSet = reasoner.getSuperClasses(owlClass, true).getFlattened();
        OWLClass parent = ((OWLClass) parentSet.toArray()[0]);
        if (parent.isOWLClass() && !parent.equals(NOTHING) && !parent.equals(THING)) {
            parents.add(parent);
        }
        return (!parent.isOWLClass() || parent.equals(NOTHING) || parent.equals(THING))
            ? parents
            : Stream.concat(parents.stream(), getParents(parent, reasoner).stream()).collect(Collectors.toList());
    }

    /**
     * Push terms to the ES instance
     *
     * @param client The ES client
     * @param indexName The index
     * @param terms Collection of Terms that will be populated
     * @throws IOException The exception
     */
    public void bulkUploadTerms(Client client, String indexName, Collection<Term> terms) throws IOException {
        // Setting the partition relatively small so we can fail fast for incremental uploads
        List<List<Term>> termLists = Lists.partition(new ArrayList<>(terms), 100);
        for (List<Term> termList: termLists) {
            BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
            for (Term term: termList) {
                bulkRequestBuilder.add(client.prepareIndex(indexName, "ontology_term")
                    .setSource(term.document())
                    .setId(term.getId())
                );
            }
            BulkResponse response = bulkRequestBuilder.execute().actionGet();
            if (response.hasFailures()) {
                for (BulkItemResponse r : response.getItems()) {
                    logger.error(r.getFailureMessage());
                }
                throw new IOException("Bulk Upload has failures: " + response.buildFailureMessage());
            }
            client.prepareBulk();
        }
    }


}
