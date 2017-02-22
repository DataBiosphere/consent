package org.broadinstitute.consent.http.service;

import com.google.common.io.Resources;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexOntologyService;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexerUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class IndexOntologyServiceTest {

    private static final String INDEX_NAME = "test-index";
    private IndexOntologyService ontologyService;
    private IndexerUtils indexUtils = new IndexerUtils();
    private Node node;
    private Client client;

    @Before
    public void setUp() throws Exception {
        // Mocks an in-memory elastic-search node
        node = NodeBuilder.nodeBuilder().node();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> node.close()));
        node.start();
        client = node.client();
        this.ontologyService = new IndexOntologyService(client, INDEX_NAME);
    }

    @Test
    public void testGetParents() throws Exception {
        URL url = Resources.getResource("diseases.owl");
        StreamRec streamRec = new StreamRec(
            url.openStream(),
            "test_ontology",
            "DOID",
            "owl",
            "diseases.owl");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(streamRec.getStream());
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
        HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();
        ontology.getAnnotationPropertiesInSignature().forEach((property) ->
            annotationProperties.put(property.getIRI().getFragment(), property));

        for (OWLClass owlClass: ontology.getClassesInSignature()) {
            if (owlClass.toStringID().equals("http://purl.obolibrary.org/obo/DOID_4952")) {
                List<Set<OWLClass>> parents = indexUtils.getParentSets(owlClass, reasoner);
                Assert.assertTrue("'postpoliomyelitis syndrome' should have 4 parents.", parents.size() == 4);
            }
            if (owlClass.toStringID().equals("http://purl.obolibrary.org/obo/DOID_1749")) {
                List<Set<OWLClass>> parents = indexUtils.getParentSets(owlClass, reasoner);
                Assert.assertTrue("'squamous cell carcinoma' should have 5 parents.", parents.size() == 5);
            }
            // fetal alcohol spectrum disorder has 5 overall parents and two parents at level 1
            if (owlClass.toStringID().equals("http://purl.obolibrary.org/obo/DOID_0050696")) {
                List<Set<OWLClass>> parents = indexUtils.getParentSets(owlClass, reasoner);
                Assert.assertTrue("'fetal alcohol spectrum disorder' should have 5 parents.", parents.size() == 5);
                Assert.assertTrue("'fetal alcohol spectrum disorder' should have 2 first level parents.", parents.get(0).size() == 2);
            }
         }

    }

    @Test
    public void testDeleteByType() {
        try {
            indexUtils.validateIndexExists(client, INDEX_NAME);
            indexUtils.deleteByOntologyType(client, INDEX_NAME, "Test");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testReIndexOntology() {
        try {
            URL url = Resources.getResource("diseases.owl");
            StreamRec streamRec = new StreamRec(
                url.openStream(),
                "Test",
                "DOID",
                "owl",
                "diseases.owl");
            ontologyService.indexOntologies(Collections.singletonList(streamRec));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testTerms() {
        try {
            Collection<Term> terms = getTerms();
            Assert.assertTrue(!terms.isEmpty());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testBulkUploadTerms() {
        try {
            Collection<Term> terms = getTerms();
            indexUtils.bulkUploadTerms(client, ontologyService.getIndexName(), terms);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private Collection<Term> getTerms() throws OWLOntologyCreationException, IOException {
        URL url = Resources.getResource("diseases.owl");
        StreamRec streamRec = new StreamRec(
            url.openStream(),
            "test_ontology",
            "DOID",
            "owl",
            "diseases.owl");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(streamRec.getStream());
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
        HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();
        ontology.getAnnotationPropertiesInSignature().forEach((property) ->
            annotationProperties.put(property.getIRI().getFragment(), property));
        Set<OWLClass> owlClasses = ontology.getClassesInSignature();
        return owlClasses.stream().map(
            (o) -> indexUtils.generateTerm(o, streamRec.getOntologyType(), ontology, annotationProperties, reasoner)
        ).collect(Collectors.toList());
    }

}
