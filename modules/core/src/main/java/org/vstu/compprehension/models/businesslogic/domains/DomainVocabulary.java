package org.vstu.compprehension.models.businesslogic.domains;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Concept;

import java.util.*;

public class DomainVocabulary {
    String vocabularyPath;
    Model model;

    public DomainVocabulary(String vocabularyPath) {
        this.vocabularyPath = vocabularyPath;
        model = ModelFactory.createDefaultModel();

        // read an RDF file
        model.read(vocabularyPath);

        ////    If the syntax is not as the file extension, a language can be declared:
        //    model.read("data.foo", "TURTLE") ;
    }

    public Model getModel() {
        return model;
    }

    public List<Concept> readConcepts() {

        HashMap<String, HashSet<String>> conceptName2bases = new HashMap<>();

        // make concepts in advance: baseConcepts can be appended after the instance created
        HashMap<String, Concept> concepts = new HashMap<>();
        // Resource ConceptClass = SKOS.Concept;
//         Property subConceptProp = SKOS.broader;

        // find all [top] concepts and recurse from them
//        ResIterator iter = model.listSubjectsWithProperty(RDFS.subClassOf, ConceptClass);
//        ResIterator iter = model.listSubjectsWithProperty(RDF.type);  // do not take all subjects: exclude third-party vocabularies
        ResIterator iter = model.listSubjectsWithProperty(model.createProperty(model.expandPrefix(":has_bitflags")));  // consider as concepts only those subjects that were marked so.
        while (iter.hasNext()) {
            Resource conceptNode = iter.nextResource();
            readConceptFromResource(conceptNode, conceptName2bases, concepts, null);
        }

        // make concepts
//        HashMap<String, Concept> concepts = new HashMap<>();

        // make concepts in order to create independent earlier
        while (! conceptName2bases.isEmpty()) {
            boolean nothingFound = true;  // check for circular dependencies
            for (String name : new HashSet<String>(conceptName2bases.keySet())) {
                HashSet<String> bases = conceptName2bases.get(name);
                if (bases.isEmpty() || concepts.keySet().containsAll(bases)) {
                    /// System.out.println("Create new: " + name + " - " +  bases);

                    // copy base concepts to new list ...
                    ArrayList<Concept> baseConcepts = new ArrayList<>();
                    for (String base : bases) {
                        baseConcepts.add(concepts.get(base));
                    }
                    // fill concept with base concepts that already exist
                    concepts.get(name).getBaseConcepts().addAll(baseConcepts);

                    conceptName2bases.remove(name);
                    nothingFound = false;
                }
            }
            if (nothingFound) {
                throw new RuntimeException("Error reading concepts from file: " + this.vocabularyPath + "\n\tThe following concepts are interdependent and cannot be created:\n\t" + conceptName2bases.keySet());
            }
        }

        return new ArrayList<>(concepts.values());
    }

    /**
     * @param conceptNode RDFNode of concept
     * @param conceptName2bases [in-out]
     * @param concepts          [in-out]
     * @param baseConceptName [optional]
     */
    protected void readConceptFromResource(@NotNull Resource conceptNode, @NotNull HashMap<String, HashSet<String>> conceptName2bases, HashMap<String, Concept> concepts, String baseConceptName) {
        String name = conceptNode.getLocalName();

        // read flags from resource's field
        int bitflags = Optional.ofNullable(conceptNode.getProperty(
                model.createProperty(model.expandPrefix(":has_bitflags"))
                ))
                .map(statement -> statement.getLiteral().getInt())
                .orElse(Concept.DEFAULT_FLAGS);

        /// System.out.println("adding: " + name + " - " +  baseConceptName);

        if (!concepts.containsKey(name)) {
            // do not add any baseConcepts here (we'll add all later)
            concepts.put(name, new Concept(name, List.of(), bitflags));
        }

        boolean shouldNotRecurse = conceptName2bases.containsKey(name);
        conceptName2bases.putIfAbsent(name, new HashSet<>());
        if (baseConceptName != null) {
            conceptName2bases.get(name).add(baseConceptName);
        }

        if (shouldNotRecurse)
            return;

        // find all child concepts
        ResIterator iter = model.listSubjectsWithProperty(SKOS.broader, conceptNode);
        while (iter.hasNext()) {
            Resource childConceptNode = iter.nextResource();
            readConceptFromResource(childConceptNode, conceptName2bases, concepts, name);
        }
    }

    /** Find all direct & indirect subclasses of given class
     *  */
    public List<String> classDescendants(String className) {
        ArrayList<String> result = new ArrayList<>();
        addDescendants(className, result, -1, RDFS.subClassOf);
        return result;
    }

    /** Extends given list with local names of subclasses till given depth limit.
     * Intended for internal use but can be utilized as-is.
     * maxDepth unlimited search if < 0; get only direct children if == 1.
     *  */
    public void addDescendants(String className, List<String> classes, int maxDepth, Property childOf) {
        if (maxDepth == 0)
            return;
        // find all child classes
        String ns = model.getNsPrefixURI("");
        Resource classNode = model.getResource(ns + className);
        ResIterator iter = model.listSubjectsWithProperty(childOf, classNode);
        while (iter.hasNext()) {
            Resource childClassNode = iter.nextResource();
            String childClassName = childClassNode.getLocalName();
            if (!classes.contains(childClassName)) {
                classes.add(childClassName);
            }
            addDescendants(childClassName, classes, maxDepth - 1, childOf);
        }
    }

    public List<String> propertyDescendants(String propertyName) {
        ArrayList<String> result = new ArrayList<>();
        addDescendants(propertyName, result, -1, RDFS.subPropertyOf);
        return result;
    }


//    def get_base_classes(classes) -> set:
//            return {sup for cl in classes for sup in cl.is_a}
//
//
//    def get_leaf_classes(classes) -> set:
//            # print(classes, "-" ,base_classes)
//		return set(classes) - get_base_classes(classes)

    /** Get set-like list of base class names */
    public List<String> getBaseClasses(List<String> classes) {
        ArrayList<String> result = new ArrayList<>();
        for (String cls : classes) {
            for (String base : classDescendants(cls)) {
                if (!result.contains(base))
                    result.add(base);
            }
        }
        return result;
    }
    public List<String> getBaseClasses(String cls) {
        return classDescendants(cls);
    }
    public List<String> getLeafClasses(List<String> classes) {
        HashSet<String> set = new HashSet<>();
        for (String cls : classes) {
            set.addAll(classDescendants(cls));
        }
        ArrayList<String> result = new ArrayList<>(classes);
        result.removeAll(set);
        result.sort(String::compareTo);
        return result;
    }

    /**
     * Return sub-set of `classes` that have no inheritance relations, keeping the most specific ones.
     * @param classes list of classes to filter
     * @return filtered version of `classes` list
     */
    public static List<OntClass> retainLeafOntClasses(List<OntClass> classes) {
        HashSet<OntClass> set = new HashSet<>();
        for (OntClass cls : classes) {
            set.addAll(cls.listSuperClasses(false).toSet());
        }
        ArrayList<OntClass> result = new ArrayList<>(classes);
        result.removeAll(set);
        return result;
    }

    public static boolean testSubClassOfTransitive(OntClass a, OntClass b) {
        if (a.equals(b))
            return true; /// ???

        HashSet<OntClass> supers = new HashSet<>(a.listSuperClasses(false).toSet());
        boolean found = supers.contains(b);
        while (!found && !supers.isEmpty()) {
            HashSet<OntClass> tested = (HashSet<OntClass>) supers.clone();
            // add all supers of tested
            tested.forEach(ontClass -> supers.addAll(ontClass.listSuperClasses(false).toSet()));
            // remove tested
            supers.removeAll(tested);
            // check again
            found = supers.contains(b);
        }
        return found;
    }

    /// debug
    public static void main(String[] args) {
        DomainVocabulary voc = new DomainVocabulary("c:\\D\\Work\\YDev\\CompPr\\CompPrehension\\modules\\server\\src" +
                "\\main\\resources\\org\\vstu\\compprehension\\models\\businesslogic\\domains\\control-flow" +
                "-statements-domain-schema.rdf");
        voc.readConcepts();
    }
}
