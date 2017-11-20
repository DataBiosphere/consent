package org.broadinstitute.consent.http.service.ontology;

import com.google.common.collect.Lists;
import com.twitter.util.CountDownLatch;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A suite of functions for handling ontology indexing functions.
 */
public class IndexerUtils {

    private static final String FIELD_DEFINITION_PROPERTY = "IAO_0000115";
    private static final String FIELD_HAS_EXACT_SYNONYM_PROPERTY = "hasExactSynonym";
    private static final String FIELD_LABEL_PROPERTY = "label";
    private static final String FIELD_DEPRECATED_PROPERTY = "deprecated";
    private static final Logger logger = LoggerFactory.getLogger(IndexerUtils.class);
    private static final ArrayList<String> IRI_FILTERS = new ArrayList<>(Arrays.asList("DOID", "DUOS", "DUO"));

    private Boolean isValidOWLClass(OWLClass owlClass) {
        return owlClass != null &&
            owlClass.isOWLClass() &&
            !owlClass.isOWLThing() &&
            !owlClass.isOWLNothing() &&
            IRI_FILTERS.stream().anyMatch(f -> owlClass.getIRI().toString().contains(f));
    }

    void checkIndex(RestClient client, String indexName) {
        try {
            client.performRequest(
                    "GET",
                    ElasticSearchSupport.getIndexPath(indexName),
                    ElasticSearchSupport.jsonHeader);
        } catch (ResponseException re) {
            // Response exception indicates a status code such as 404 not found, so try creating it.
            try {
                client.performRequest(
                        "PUT",
                        ElasticSearchSupport.getIndexPath(indexName),
                        ElasticSearchSupport.jsonHeader);
            } catch (IOException ioe) {
                logger.error("Exception creating index: " + indexName + ": " + ioe.getMessage());
            }
        } catch (IOException e) {
            logger.error("Exception checking for index: " + indexName + ": " + e.getMessage());
        }
    }

    /**
     * Generate terms from an ontology file, along with any direct ontology imports.
     *
     * @param streamRec The StreamRec that refers to an an ontology file.
     * @return Collection of Terms generated from the ontology file
     * @throws IOException The IOException
     * @throws OWLOntologyCreationException The OWLOntologyCreationException
     */
    public Collection<Term> generateTerms(StreamRec streamRec) throws IOException, OWLOntologyCreationException {
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

        Set<OWLClass> owlClasses = ontology.classesInSignature().collect(Collectors.toSet());
        owlClasses.addAll(ontology.
                directImports().
                flatMap(HasClassesInSignature::classesInSignature).
                collect(Collectors.toSet()));
        return owlClasses.
                stream().
                filter(this::isValidOWLClass).
                map(o -> generateTerm(o, streamRec.getOntologyType(), ontology, reasoner)).
                collect(Collectors.toSet());
    }

    /**
     * Generate a search Term that will be pushed to Elastic Search
     *
     * @param owlClass The OWLClass to generate Term content from
     * @param ontologyType The type (i.e. "Disease", "Organization")
     * @param ontology OWL API class that manages all OWL related functions
     * @param reasoner OWLReasoner
     * @return Term
     */
    public Term generateTerm(OWLClass owlClass, String ontologyType, OWLOntology ontology, OWLReasoner reasoner) {
        Term term = new Term(owlClass.toStringID(), ontologyType);
        Set<OWLAnnotation> classAnnotations = EntitySearcher.getAnnotations(owlClass, ontology).collect(Collectors.toSet());
        classAnnotations.addAll(EntitySearcher.getAnnotationObjects(owlClass, ontology.imports(), null).collect(Collectors.toSet()));
        for (OWLAnnotation annotation : classAnnotations) {
            String propertyName = annotation.getProperty().getIRI().getRemainder().orElse("");
            String propertyValue = "";
            if (annotation.getValue().asLiteral().isPresent()) {
                propertyValue = annotation.getValue().asLiteral().get().getLiteral();
            }
            if (propertyName.equals(FIELD_DEPRECATED_PROPERTY)) {
                term.setUsable(false);
            }
            if (propertyName.equals(FIELD_HAS_EXACT_SYNONYM_PROPERTY)) {
                term.addSynonym(propertyValue);
            }
            if (propertyName.equals(FIELD_LABEL_PROPERTY)) {
                term.addLabel(propertyValue);
            }
            if (propertyName.equals(FIELD_DEFINITION_PROPERTY)) {
                term.addDefinition(propertyValue);
            }
        }

        int position = 0;
        for (Set<OWLClass> parentSet : getFilteredParentSets(owlClass, reasoner)) {
            position ++;
            for (OWLClass parentClass : parentSet) {
                Set<OWLAnnotation> parentAnnotations = EntitySearcher.getAnnotations(parentClass, ontology).collect(Collectors.toSet());
                String label = getPropFromAnnotations(parentAnnotations, FIELD_LABEL_PROPERTY);
                term.addParent(parentClass.toStringID(), label, position);
            }
        }
        return term;
    }


    private String getPropFromAnnotations(Set<OWLAnnotation> annotations, String propName) {
        for (OWLAnnotation annotation : annotations) {
            String propertyName = annotation.getProperty().getIRI().getRemainder().orElse("");
            if (annotation.getValue().asLiteral().isPresent() && propertyName.equals(propName)) {
                return annotation.getValue().asLiteral().get().getLiteral();
            }
        }
        return null;
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
        Set<OWLClass> parentSet = reasoner.
                getSuperClasses(owlClass, true).
                entities().
                collect(Collectors.toSet());
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
     * @return True if there are no errors, exception otherwise
     * @throws IOException The exception
     */
    public Boolean bulkUploadTerms(RestClient client, String indexName, Collection<Term> terms) throws IOException {
        // Set the partition relatively small so we can fail fast for incremental uploads
        List<List<Term>> termLists = Lists.partition(new ArrayList<>(terms), 100);
        for (List<Term> termList: termLists) {
            final CountDownLatch latch = new CountDownLatch(termList.size());
            ResponseListener listener = createResponseListener(latch);
            for (Term term: termList) {
                HttpEntity entity = new NStringEntity(
                    term.toString(),
                    ContentType.APPLICATION_JSON);
                client.performRequestAsync("PUT",
                    ElasticSearchSupport.getTermIdPath(indexName, term.getId()),
                    Collections.emptyMap(),
                    entity,
                    listener,
                    ElasticSearchSupport.jsonHeader);
            }
            latch.await();
        }
        return true;
    }

    /**
     * Set all terms of a particular ontology type to `usable=false`, thereby deprecating them.
     *
     * @param client The ES client
     * @param indexName The index
     * @param ontologyType The ontology type (e.g. "Disease", or "Organization")
     */
    public void bulkDeprecateTerms(RestClient client, String indexName, String ontologyType) throws IOException, InternalServerErrorException {
        String query = "{" +
            "  \"script\": {" +
            "    \"inline\": \"ctx._source.usable=false\"," +
            "    \"lang\": \"painless\"" +
            "  }," +
            "  \"query\": {" +
            "    \"term\": {" +
            "      \"ontology\": \"" + ontologyType.toLowerCase() + "\"" +
            "    }" +
            "  }" +
            "}";
        String path = "/" + indexName + "/_update_by_query";
        Response esResponse = client.performRequest(
            "POST",
            path,
            Collections.emptyMap(),
            new NStringEntity(query, ContentType.APPLICATION_JSON),
            ElasticSearchSupport.jsonHeader);
        if (esResponse.getStatusLine().getStatusCode() != 200) {
            logger.error("Error in bulk deprecate response: " + esResponse.getStatusLine().getReasonPhrase());
            throw new InternalServerErrorException(esResponse.getStatusLine().getReasonPhrase());
        }
    }

    /**
     * Create a response handler that will appropriately deal with the response from Elastic Search
     *
     * @param latch The countdown latch to decrement with each successful response
     * @return The ResponseListener
     */
    private ResponseListener createResponseListener(CountDownLatch latch) {
        return new ResponseListener() {
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
        };
    }

}
