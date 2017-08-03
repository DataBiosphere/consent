package org.broadinstitute.consent.http.service;

import com.google.common.io.Resources;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.broadinstitute.consent.http.service.ontologyIndexer.ElasticSearchSupport;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexOntologyService;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexerUtils;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class IndexOntologyServiceTest {

    private static final String INDEX_NAME = "test-index";
    private IndexOntologyService ontologyService;
    private IndexerUtils indexUtils = new IndexerUtils();
    private ClientAndServer server;
    private RestClient client;

    @Before
    public void setUp() throws Exception {
        ElasticSearchConfiguration configuration = new ElasticSearchConfiguration();
        configuration.setIndexName(INDEX_NAME);
        configuration.setServers(Collections.singletonList("localhost"));
        client = ElasticSearchSupport.createRestClient(configuration);
        this.ontologyService = new IndexOntologyService(configuration);
        server = startClientAndServer(9200);
        server.when(request()).respond(response().withStatusCode(200));
    }

    @After
    public void shutDown() throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }

    @Test
    public void testReIndexOntology() {
        try {
            URL url = Resources.getResource("data-use.owl");
            StreamRec streamRec = new StreamRec(
                url.openStream(),
                "Test",
                "DUOS",
                "owl",
                "data-use.owl");
            ontologyService.indexOntologies(Collections.singletonList(streamRec));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetParents() throws Exception {
        URL url = Resources.getResource("data-use.owl");
        StreamRec streamRec = new StreamRec(
            url.openStream(),
            "test_ontology",
            "DUOS",
            "owl",
            "data-use.owl");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(streamRec.getStream());
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
        HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();
        ontology.getAnnotationPropertiesInSignature().forEach((property) ->
            annotationProperties.put(property.getIRI().getFragment(), property));

        for (OWLClass owlClass: ontology.getClassesInSignature()) {
            if (owlClass.toStringID().equals("http://www.broadinstitute.org/ontologies/DUOS/MGH")) {
                List<Set<OWLClass>> parents = indexUtils.getFilteredParentSets(owlClass, reasoner);
                Assert.assertTrue("'MGH' should have 4 parents.", parents.size() == 4);
            }
            if (owlClass.toStringID().equals("http://www.broadinstitute.org/ontologies/DUOS/girls")) {
                List<Set<OWLClass>> parents = indexUtils.getFilteredParentSets(owlClass, reasoner);
                Assert.assertTrue("'girls' should have 2 parents.", parents.size() == 2);
            }
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
            indexUtils.bulkUploadTerms(client, INDEX_NAME, terms);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testBulkDeprecateTerms() {
        try {
            indexUtils.bulkDeprecateTerms(client, INDEX_NAME, "organization");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


    private Collection<Term> getTerms() throws OWLOntologyCreationException, IOException {
        URL url = Resources.getResource("data-use.owl");
        StreamRec streamRec = new StreamRec(
            url.openStream(),
            "test_ontology",
            "DUOS",
            "owl",
            "data-use.owl");
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
