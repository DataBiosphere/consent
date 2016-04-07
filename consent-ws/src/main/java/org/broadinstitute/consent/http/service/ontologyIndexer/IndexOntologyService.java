package org.broadinstitute.consent.http.service.ontologyIndexer;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.models.ontology.StreamRec;
import org.broadinstitute.consent.http.models.ontology.Term;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.client.Client;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by SantiagoSaucedo on 3/11/2016.
 */
public class IndexOntologyService {

    private final Client client;
    private final String indexName;



    public IndexOntologyService(Client client, String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    private static final String FIELD_DEFINITION_CLASS = "IAO_0000115";


    public void indexOntologies(List<StreamRec> fileCompList) throws IOException {

        int count = 0;
        BulkRequestBuilder bulk = client.prepareBulk();

       /*
       * This block checks the existence of an index with name @indexName, if not creates one.
       */
        IndicesExistsRequest existsRequest = new IndicesExistsRequest(indexName);
        if(!client.admin().indices().exists(existsRequest).actionGet().isExists()){
        CreateIndexRequest indexRequest = new CreateIndexRequest(indexName);
        try {
            client.admin().indices().create(indexRequest).actionGet();
        } catch(Exception e){
                throw new InternalServerErrorException();
            }
        }
        try{
        for (StreamRec streamRec : fileCompList) {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            //Just to be capable of read Inputstream multiple times
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            org.apache.commons.io.IOUtils.copy(streamRec.getStream(), baos);
            byte[] bytes = baos.toByteArray();

            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(bytes));
            streamRec.setStream(new ByteArrayInputStream(bytes));

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

            Set<String> ontologyIdList = ontology.getClassesInSignature().stream().map(s -> s.toStringID()).collect(Collectors.toSet());
            MultiGetResponse multiGetItemResponses = client.prepareMultiGet().add(indexName,null,ontologyIdList).get();
            Boolean alreadyIndexedOntology = Arrays.asList(multiGetItemResponses.getResponses()).stream().anyMatch(r -> r.getResponse().isExists());
            if(alreadyIndexedOntology ){
                throw new BadRequestException("Ontologies already indexed, please delete the related file before upload the new file.");
            }
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

                Term term = new Term(owlClass.toStringID(), streamRec.getOntologyType());
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
                if (ids.size() != 1 || !ids.iterator().next().getValue().accept(visitor).startsWith(streamRec.getPrefix())) {
                    continue;
                }else{
                    streamRec.setAtLeastOneOntologyIndexed(true);
                }

                Set<OWLAnnotation> defs = owlClass.getAnnotations(ontology, def);
                assert defs.size() <= 1 : "Exactly 0 or 1 definitions allowed per class";
                if (defs.size() == 1) {
                    term.addDefinition(defs.iterator().next().getValue().accept(visitor));
                }
                    bulk.add(client.prepareIndex(indexName, "ontology_term")
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
        } catch (OWLOntologyCreationException e){
            throw  new BadRequestException("Problem with OWL file.");
        }


      }


    public Boolean deleteOntologiesByFile(InputStream fileStream, String prefix) {

        Boolean atLeastOneDeletion = false;
        List<String> toDeleteIds = new ArrayList<>();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(fileStream);
            HashMap<String, OWLAnnotationProperty> annotationProperties = new HashMap<>();
            ontology.getAnnotationPropertiesInSignature().stream().forEach((property) -> {
                annotationProperties.put(property.getIRI().getFragment(), property);
            });
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
