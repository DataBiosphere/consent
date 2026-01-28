package org.broadinstitute.consent.http.service.ontology;

import com.google.cloud.storage.BlobId;
import com.google.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.HasClassesInSignature;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;

public class OntologyIndexService implements ConsentLogger {

  private static final List<String> IRI_FILTERS = List.of("DOID", "DUO");
  private static final String OBSOLETE_CLASS_IRI =
      "http://www.geneontology.org/formats/oboInOwl#ObsoleteClass";

  private final GCSService gcsService;
  private final StoreConfiguration storeConfiguration;

  @Inject
  public OntologyIndexService(GCSService gcsService, StoreConfiguration storeConfiguration) {
    this.gcsService = gcsService;
    this.storeConfiguration = storeConfiguration;
  }

  public Collection<OntologyTerm> generateTerms(String ontologyFile, String ontologyType)
      throws OWLOntologyCreationException {
    BlobId blobId = BlobId.of(storeConfiguration.getBucket(), "ontology/" + ontologyFile);
    InputStream is = gcsService.getDocument(blobId);
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntologyFromOntologyDocument(is);
    OWLOntologyID ontologyID = ontology.getOntologyID();
    Optional<IRI> versionIRI = ontologyID.getVersionIRI();
    String version = versionIRI.map(IRI::toString).orElse("unknown_version");
    OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
    Set<OWLClass> owlClasses = ontology.classesInSignature().collect(Collectors.toSet());
    owlClasses.addAll(
        ontology
            .directImports()
            .flatMap(HasClassesInSignature::classesInSignature)
            .collect(Collectors.toSet()));
    return owlClasses.stream()
        .filter(this::isValidOWLClass)
        .map(o -> generateTerm(o, ontologyType, ontology, reasoner, version))
        .filter(t -> t.usable)
        .collect(Collectors.toSet());
  }

  private OntologyTerm generateTerm(
      OWLClass owlClass,
      String ontologyType,
      OWLOntology ontology,
      OWLReasoner reasoner,
      String version) {
    // Process basic term fields
    OntologyTerm ontologyTerm = new OntologyTerm(owlClass.toStringID(), version, ontologyType);
    OWLClass obsoleteClass =
        ontology
            .getOWLOntologyManager()
            .getOWLDataFactory()
            .getOWLClass(IRI.create(OBSOLETE_CLASS_IRI));
    Set<OWLAnnotation> classAnnotations =
        EntitySearcher.getAnnotations(owlClass, ontology).collect(Collectors.toSet());
    classAnnotations.addAll(
        EntitySearcher.getAnnotationObjects(owlClass, ontology.imports(), null)
            .collect(Collectors.toSet()));
    OwlClassFields classFields = new OwlClassFields(classAnnotations);
    ontologyTerm.setLabel(classFields.getLabel());
    ontologyTerm.setDefinition(classFields.getDefinition());
    ontologyTerm.setOboId(classFields.getOboId());
    ontologyTerm.setSynonyms(classFields.getSynonyms());
    boolean isObsolete = reasoner.getSuperClasses(owlClass, false).containsEntity(obsoleteClass);
    ontologyTerm.setUsable(!isObsolete && !Boolean.TRUE.equals(classFields.getDeprecated()));

    // Process parents if the term is usable
    if (Boolean.TRUE.equals(ontologyTerm.usable)) {
      int position = 0;
      for (Set<OWLClass> parentSet : getFilteredParentSets(owlClass, reasoner)) {
        position++;
        for (OWLClass parentClass : parentSet) {
          Set<OWLAnnotation> parentAnnotations =
              EntitySearcher.getAnnotations(parentClass, ontology).collect(Collectors.toSet());
          OwlClassFields parentClassFields = new OwlClassFields(parentAnnotations);
          ParentTerm parentTerm = new ParentTerm();
          parentTerm.setId(parentClass.toStringID());
          parentTerm.setOrder(position);
          parentTerm.setLabel(parentClassFields.getLabel());
          parentTerm.setDefinition(parentClassFields.getDefinition());
          parentTerm.setSynonyms(parentClassFields.getSynonyms());
          ontologyTerm.addParent(parentTerm);
        }
      }
    }
    return ontologyTerm;
  }

  private List<Set<OWLClass>> getFilteredParentSets(OWLClass owlClass, OWLReasoner reasoner) {
    List<Set<OWLClass>> parentSets = getParentSets(owlClass, reasoner);
    List<Set<OWLClass>> filteredSets = new ArrayList<>();
    List<String> owlClassCache = new ArrayList<>();
    for (Set<OWLClass> classSet : parentSets) {
      // For any nodes in this set that have not been seen, create a new node set for adding
      Set<OWLClass> filteredParentSet =
          classSet.stream()
              .filter(oc -> !owlClassCache.contains(oc.toStringID()))
              .collect(Collectors.toSet());
      // Make sure all new nodes have their IDs added to the cache for future cache checking
      owlClassCache.addAll(filteredParentSet.stream().map(OWLClass::toStringID).toList());
      // Finally, if we have a non-empty node, make sure it gets back into the queue
      if (!filteredParentSet.isEmpty()) {
        filteredSets.add(filteredParentSet);
      }
    }
    return filteredSets;
  }

  private boolean isValidOWLClass(OWLClass owlClass) {
    return owlClass != null
        && owlClass.isOWLClass()
        && !owlClass.isOWLThing()
        && !owlClass.isOWLNothing()
        && IRI_FILTERS.stream().anyMatch(f -> owlClass.getIRI().toString().contains(f));
  }

  private List<Set<OWLClass>> getParentSets(OWLClass owlClass, OWLReasoner reasoner) {
    List<Set<OWLClass>> parents = new ArrayList<>();
    Set<OWLClass> parentSet =
        reasoner.getSuperClasses(owlClass, true).entities().collect(Collectors.toSet());
    Set<OWLClass> validParentSet = new HashSet<>();
    parentSet.forEach(
        p -> {
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
                validParentSet.stream().map(p -> getParentSets(p, reasoner)).flatMap(List::stream))
            .toList();
  }
}
