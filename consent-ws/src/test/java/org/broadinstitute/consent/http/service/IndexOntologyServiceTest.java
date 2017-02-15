package org.broadinstitute.consent.http.service;

import com.google.common.io.Resources;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexOntologyService;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexerUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.*;
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

    private IndexOntologyService ontologyService;
    private IndexerUtils indexUtils = new IndexerUtils();
    private Node node;
    private Client client;

    @Before
    public void setUp() throws Exception {
        // Mocks an in-memory elastic-search node
        node = NodeBuilder.nodeBuilder().node();
        client = node.client();
        this.ontologyService = new IndexOntologyService(client, "test-index");
    }

    @After
    public void shutDown() {
        client.close();
        node.close();
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
                List<OWLClass> parents = indexUtils.getParents(owlClass, reasoner);
                Assert.assertTrue("'postpoliomyelitis syndrome' should have 4 parents.", parents.size() == 4);
            }
            if (owlClass.toStringID().equals("http://purl.obolibrary.org/obo/DOID_1749")) {
                List<OWLClass> parents = indexUtils.getParents(owlClass, reasoner);
                Assert.assertTrue("'squamous cell carcinoma' should have 5 parents.", parents.size() == 5);
            }
         }

    }

    @Test
    public void testValidateIndexExists() {
        try {
            indexUtils.validateIndexExists(client, "test-index");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDeleteByType() {
        try {
            indexUtils.deleteByOntologyType(client, "test-index", "Test");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

//    TODO: Failing through mvn test, but works locally
//    @Test
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

//    TODO: Failing through mvn test, but works locally
//    @Test
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
