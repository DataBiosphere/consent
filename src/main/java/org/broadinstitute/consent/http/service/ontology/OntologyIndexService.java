package org.broadinstitute.consent.http.service.ontology;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.HasClassesInSignature;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;

public class OntologyIndexService implements ConsentLogger {

  private static final String FIELD_DEFINITION_PROPERTY = "IAO_0000115";
  private static final String FIELD_HAS_EXACT_SYNONYM_PROPERTY = "hasExactSynonym";
  private static final String FIELD_LABEL_PROPERTY = "label";
  private static final String FIELD_DEPRECATED_PROPERTY = "deprecated";
  private static final ArrayList<String> IRI_FILTERS =
      new ArrayList<>(Arrays.asList("DOID", "DUOS", "DUO"));

  public Collection<OntologyTerm> generateTerms(InputStream stream, String ontologyType)
      throws OWLOntologyCreationException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntologyFromOntologyDocument(stream);
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
    OntologyTerm ontologyTerm = new OntologyTerm(owlClass.toStringID(), version, ontologyType);
    Set<OWLAnnotation> classAnnotations =
        EntitySearcher.getAnnotations(owlClass, ontology).collect(Collectors.toSet());
    classAnnotations.addAll(
        EntitySearcher.getAnnotationObjects(owlClass, ontology.imports(), null)
            .collect(Collectors.toSet()));
    for (OWLAnnotation annotation : classAnnotations) {
      String propertyName = annotation.getProperty().getIRI().getRemainder().orElse("");
      String propertyValue =
          annotation.getValue().asLiteral().map(OWLLiteral::getLiteral).orElse("");
      if (propertyName.equals(FIELD_DEPRECATED_PROPERTY)) {
        ontologyTerm.setUsable(false);
      }
      if (propertyName.equals(FIELD_HAS_EXACT_SYNONYM_PROPERTY)) {
        ontologyTerm.addSynonym(propertyValue);
      }
      if (propertyName.equals(FIELD_LABEL_PROPERTY)) {
        ontologyTerm.setLabel(propertyValue);
      }
      if (propertyName.equals(FIELD_DEFINITION_PROPERTY)) {
        ontologyTerm.setDefinition(propertyValue);
      }
    }

    int position = 0;
    for (Set<OWLClass> parentSet : getFilteredParentSets(owlClass, reasoner)) {
      position++;
      for (OWLClass parentClass : parentSet) {
        Set<OWLAnnotation> parentAnnotations =
            EntitySearcher.getAnnotations(parentClass, ontology).collect(Collectors.toSet());
        String label = getPropFromAnnotations(parentAnnotations);
        ontologyTerm.addParent(parentClass.toStringID(), label, position);
      }
    }
    logDebug("Generated term: " + ontologyTerm);
    return ontologyTerm;
  }

  private List<Set<OWLClass>> getFilteredParentSets(OWLClass owlClass, OWLReasoner reasoner) {
    List<Set<OWLClass>> parentSets = getParentSets(owlClass, reasoner);
    // TODO: Review if we really need to reverse the parent sets
    // Collections.reverse(parentSets);
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
    Collections.reverse(filteredSets);
    return filteredSets;
  }

  private String getPropFromAnnotations(Set<OWLAnnotation> annotations) {
    for (OWLAnnotation annotation : annotations) {
      String propertyName = annotation.getProperty().getIRI().getRemainder().orElse("");
      if (propertyName.equals(FIELD_LABEL_PROPERTY)) {
        return annotation.getValue().asLiteral().map(Object::toString).orElse(null);
      }
    }
    return null;
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
