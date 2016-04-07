package org.broadinstitute.datause.indexer;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;

/*
usage : java -jar consent-indexer.jar DOID disease /path/to/HumanDO_xp.obo DURPO organization /path/to/data-use.owl
 */
public class Indexer {

    private static final String FIELD_DEFINITION_CLASS = "IAO_0000115";

    public static void main(String[] args) throws IOException, OWLOntologyCreationException {
        Yaml yaml = new Yaml(new Constructor(IndexConfiguration.class));

        IndexConfiguration configuration = (IndexConfiguration) yaml.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("index.yml"));

        TransportClient client = new TransportClient(ImmutableSettings.settingsBuilder()
                .put("cluster.name", configuration.getClusterName()));

        configuration.getServers().stream().forEach((server) -> {
            client.addTransportAddress(new InetSocketTransportAddress(server, 9300));
        });

        BulkRequestBuilder bulk = client.prepareBulk();
        int count = 0;

        for (int arg = 0; arg + 1 < args.length; arg += 3) {
            String idPrefix = args[arg]; // ontology to load, based on a prefix of its ID
            String ontologyType = args[arg + 1]; // ontology type : disease, organization 
            String fileName = args[arg + 2];
            InputStream stream = new FileInputStream(fileName);

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(stream);

            HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();

            ontology.getAnnotationPropertiesInSignature().stream().forEach((property) -> {
                annotationProperties.put(property.getIRI().getFragment(), property);
            });

            OWLAnnotationProperty hasExactSynonym = annotationProperties.get("hasExactSynonym");
            assert hasExactSynonym != null : "Need hasExactSynonym annotation property.";
            OWLAnnotationProperty label = annotationProperties.get("label");
            assert label != null : "Need label annotation property";
            OWLAnnotationProperty def = annotationProperties.get(FIELD_DEFINITION_CLASS);
            assert def != null : "Need def annotation property";
            OWLAnnotationProperty deprecated = annotationProperties.get("deprecated");
            assert deprecated != null : "Need deprecated annotation property";

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

                Term term = new Term(owlClass.toStringID(), ontologyType);
                if (!owlClass.getAnnotations(ontology, deprecated).isEmpty()) {
                    term.setUsable(false);
                }

                owlClass.getAnnotations(ontology, hasExactSynonym).stream().forEach((synonyms) -> {
                    term.addSynonym(synonyms.getValue().accept(visitor));
                });

                Set<OWLAnnotation> labels = owlClass.getAnnotations(ontology, label);
                assert labels.size() <= 1 : "Exactly 0 or 1 labels allowed per class";
                if (labels.size() == 1) {
                    term.addLabel(labels.iterator().next().getValue().accept(visitor));
                }

                // Only index those terms whose IDs begin with the specified prefix
                Set<OWLAnnotation> ids = owlClass.getAnnotations(ontology, annotationProperties.get("id"));
                if (ids.size() != 1 || !ids.iterator().next().getValue().accept(visitor).startsWith(idPrefix)) {
                    continue;
                }

                Set<OWLAnnotation> defs = owlClass.getAnnotations(ontology, def);
                assert defs.size() <= 1 : "Exactly 0 or 1 definitions allowed per class";
                if (defs.size() == 1) {
                    term.addDefinition(defs.iterator().next().getValue().accept(visitor));
                }

                bulk.add(client.prepareIndex(configuration.getIndexName(), "ontology_term")
                        .setSource(term.document())
                        .setId(owlClass.toStringID())
                );

                if (count++ > 1000) {
                    bulk.execute().actionGet();
                    bulk = client.prepareBulk();
                    count = 0;
                }
            }
        }

        if (count > 0) {
            bulk.execute().actionGet();
        }

        client.close();
    }
}
