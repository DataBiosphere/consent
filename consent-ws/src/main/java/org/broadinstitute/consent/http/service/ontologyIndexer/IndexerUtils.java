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

    private Boolean isValidOWLClass(OWLClass owlClass) {
        return owlClass != null &&
            owlClass.isOWLClass() &&
            !owlClass.isOWLThing() &&
            !owlClass.isOWLNothing();
    }

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
        QueryBuilder queryBuilder = QueryBuilders.termQuery(Term.FIELD_ONTOLOGY_TYPE, type);
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
        for (Set<OWLClass> parentSet : getFilteredParentSets(owlClass, reasoner)) {
            position ++;
            for (OWLClass p : parentSet) {
                term.addParent(p.toStringID(), position);
            }
        }
        return term;

    }

    /**
     * We need to check for duplicate sets from the top down. For instance, if we have DOID_4 as the top-level node,
     * we should filter it out of all lower level nodes. This happens when a class has two parents and each of those
     * parents have a common ancestor, which is true in 100% of the cases. We need to keep the top-most node and filter
     * out the duplicates lower down in the tree.
     *
     * @param owlClass The class to find parents for
     * @param reasoner Reasoner required to make inferences.
     * @return List of ordered OWLClass parents
     */
    public List<Set<OWLClass>> getFilteredParentSets(OWLClass owlClass, OWLReasoner reasoner) {
        List<Set<OWLClass>> parentSets = getParentSets(owlClass, reasoner);
        Collections.reverse(parentSets);
        List<Set<OWLClass>> filteredSets = new ArrayList<>();
        List<String> owlClassCache = new ArrayList<>();
        for (Set<OWLClass> classSet : parentSets) {
            // For any nodes in this set that have not been seen, create a new node set for adding
            Set<OWLClass> filteredParentSet = classSet.stream().
                filter(oc -> !owlClassCache.contains(oc.toStringID())).
                collect(Collectors.toSet());
            // Make sure all new nodes have their IDs added to the cache for future cache checking
            owlClassCache.addAll(filteredParentSet.stream().map(OWLClass::toStringID).collect(Collectors.toList()));
            // Finally, if we have a non-empty node, make sure it gets back into the queue
            if (!filteredParentSet.isEmpty()) {
                filteredSets.add(filteredParentSet);
            }
        }
        Collections.reverse(filteredSets);
        return filteredSets;
    }

    /**
     * Recursively generate an ordered list of OWLClass Parent Sets.
     *
     * @param owlClass The class to find parents for
     * @param reasoner Reasoner required to make inferences.
     * @return List of ordered OWLClass parents
     */
    private List<Set<OWLClass>> getParentSets(OWLClass owlClass, OWLReasoner reasoner) {
        List<Set<OWLClass>> parents = new ArrayList<>();
        Set<OWLClass> parentSet = reasoner.getSuperClasses(owlClass, true).getFlattened();
        Set<OWLClass> validParentSet = new HashSet<>();
        parentSet.forEach(p -> {
            if (isValidOWLClass(p)) {
                validParentSet.add(p);
            }
        });
        if (!validParentSet.isEmpty()) {
            parents.add(validParentSet);
        }
        return validParentSet.isEmpty()
            ? parents
            : Stream.concat(
                parents.stream(),
                // Apologies for this monstrosity of a call
                validParentSet.stream().map(p -> getParentSets(p, reasoner)).collect(Collectors.toList()).stream().flatMap(List::stream).collect(Collectors.toList()).stream()
            ).collect(Collectors.toList());
    }

    /**
     * Push terms to the ES instance
     *
     * @param client The ES client
     * @param indexName The index
     * @param terms Collection of Terms that will be populated
     * @return True if there are no errors, false otherwise
     * @throws IOException The exception
     */
    public Boolean bulkUploadTerms(Client client, String indexName, Collection<Term> terms) throws IOException {
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
                    if (r.isFailed()) {
                        logger.error(r.getFailureMessage());
                    }
                }
                return false;
            }
            client.prepareBulk();
        }
        return true;
    }


}
