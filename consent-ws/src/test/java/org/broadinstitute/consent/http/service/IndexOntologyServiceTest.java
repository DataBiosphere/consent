package org.broadinstitute.consent.http.service;

import com.google.common.io.Resources;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.broadinstitute.consent.http.service.ontology.IndexOntologyService;
import org.broadinstitute.consent.http.service.ontology.IndexerUtils;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    public void testParentTerms() throws Exception {
        Collection<Term> terms = getTerms();
        Term female = null;
        for (Term term : terms) {
            if (term.getId().equals("http://www.broadinstitute.org/ontologies/DUOS/female")) {
                female = term;
            }
        }
        Assert.assertNotNull(female);

        Term.Parent phenotypicSex = null;
        for (Term.Parent p : female.getParents()) {
            if (p.getId().equals("http://www.broadinstitute.org/ontologies/DUOS/phenotypic_sex")) {
                phenotypicSex = p;
            }
        }
        Assert.assertNotNull(phenotypicSex);
        Assert.assertNotNull(phenotypicSex.getLabel());
        Assert.assertNotNull(phenotypicSex.getOrder());

        Assert.assertEquals("phenotypic sex", phenotypicSex.getLabel());
        Assert.assertEquals(Integer.valueOf(1), phenotypicSex.getOrder());
    }

    @Test
    public void testGenerateTerms() {
        try {
            URL url = Resources.getResource("data-use.owl");
            StreamRec streamRec = new StreamRec(
                    url.openStream(),
                    "Test",
                    "DUOS",
                    "owl",
                    "data-use.owl");
            Collection<Term> terms = indexUtils.generateTerms(streamRec);
            Assert.assertNotNull(terms);
            Assert.assertFalse(terms.isEmpty());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
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
        for (OWLClass owlClass: ontology.classesInSignature().collect(Collectors.toSet())) {
            if (owlClass.toStringID().equals("http://www.broadinstitute.org/ontologies/DUOS/control")) {
                List<Set<OWLClass>> parents = indexUtils.getFilteredParentSets(owlClass, reasoner);
                Assert.assertTrue("'control' should have 1 parent.", parents.size() == 1);
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
        Set<OWLClass> owlClasses = ontology.classesInSignature().collect(Collectors.toSet());
        return owlClasses.stream().map(
            (o) -> indexUtils.generateTerm(o, streamRec.getOntologyType(), ontology, reasoner)
        ).collect(Collectors.toList());
    }

}
