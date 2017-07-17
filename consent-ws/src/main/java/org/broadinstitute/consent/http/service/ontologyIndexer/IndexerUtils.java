package org.broadinstitute.consent.http.service.ontologyIndexer;

import com.google.common.collect.Lists;
import com.twitter.util.CountDownLatch;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
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
    public void validateIndexExists(RestClient client, String indexName) throws InternalServerErrorException {
        try {
            Response esResponse = client.performRequest("GET", ElasticSearchSupport.getIndexPath(indexName));
            if (esResponse.getStatusLine().getStatusCode() != 200) {
                logger.error("Invalid index request: " + esResponse.getStatusLine().getReasonPhrase());
                throw new InternalServerErrorException(esResponse.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new InternalServerErrorException(e.getMessage());
        }
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
        if (deprecated != null && !owlClass.getAnnotations(ontology, deprecated).isEmpty()) {
            term.setUsable(false);
        }

        if (hasExactSynonym != null) {
            owlClass.getAnnotations(ontology, hasExactSynonym).forEach((synonyms) ->
                term.addSynonym(synonyms.getValue().accept(visitor)));
        }

        if (label != null) {
            Set<OWLAnnotation> labels = owlClass.getAnnotations(ontology, label);
            assert labels.size() <= 1 : "Exactly 0 or 1 labels allowed per class";
            if (labels.size() == 1) {
                term.addLabel(labels.iterator().next().getValue().accept(visitor));
            }
        }

        if (definition != null) {
            Set<OWLAnnotation> definitions = owlClass.getAnnotations(ontology, definition);
            assert definitions.size() <= 1 : "Exactly 0 or 1 definitions allowed per class";
            if (definitions.size() == 1) {
                term.addDefinition(definitions.iterator().next().getValue().accept(visitor));
            }
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
     * @return Ordered list of OWLClass parent sets
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
                validParentSet.stream().map(p -> getParentSets(p, reasoner)).flatMap(List::stream)
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
    public Boolean bulkUploadTerms(RestClient client, String indexName, Collection<Term> terms) throws IOException {
        // Setting the partition relatively small so we can fail fast for incremental uploads
        List<List<Term>> termLists = Lists.partition(new ArrayList<>(terms), 100);
        for (List<Term> termList: termLists) {
            final CountDownLatch latch = new CountDownLatch(termList.size());
            for (Term term: termList) {
                HttpEntity entity = new NStringEntity(
                    term.toString(),
                    ContentType.APPLICATION_JSON);
                client.performRequestAsync("PUT",
                    ElasticSearchSupport.getTermIdPath(indexName, term.getId()),
                    Collections.emptyMap(),
                    entity,
                    new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            latch.countDown();
                        }
                        @Override
                        public void onFailure(Exception exception) {
                            logger.error(exception.getMessage());
                            latch.countDown();
                            try {
                                throw new IOException(exception);
                            } catch (IOException e) {
                                logger.error(exception.getMessage());
                            }
                        }
                    });
            }
            latch.await();
        }
        return true;
    }

    /**
     * Delete terms from the ES instance
     *
     * @param client The ES client
     * @param indexName The index
     * @param termIds Collection of Term IDs that will be deleted
     * @return True if there are no errors, false otherwise
     * @throws IOException The exception
     */
    public Boolean bulkDeleteTerms(RestClient client, String indexName, Collection<String> termIds) throws IOException {
        final CountDownLatch latch = new CountDownLatch(termIds.size());
        for (String id : termIds) {
            client.performRequestAsync("DELETE",
                ElasticSearchSupport.getTermIdPath(indexName, id),
                Collections.emptyMap(),
                new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception exception) {
                        logger.error(exception.getMessage());
                        latch.countDown();
                        try {
                            throw new IOException(exception);
                        } catch (IOException e) {
                            logger.error(exception.getMessage());
                        }
                    }
                });
        }
        latch.await();
        return true;
    }

}
