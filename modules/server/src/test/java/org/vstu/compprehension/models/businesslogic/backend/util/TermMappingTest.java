package org.vstu.compprehension.models.businesslogic.backend.util;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TermMappingTest {
    TermMapping tm;

    @BeforeEach
    public void setUp() throws Exception {
        tm = new TermMapping();
    }
    @Test
    public void test_termToResource() {
        // Expand simple name as local, or prefixed name as special
        String ns = TermMapping.BASE_URI_DEFAULT + "#";

        Assertions.assertEquals(ns + "a", tm.termToResource("a").getURI());
        Assertions.assertEquals(ns + "a", tm.termToResource(":a").getURI());
        Assertions.assertEquals(ns + "a", tm.termToResource("my:a").getURI());

        Assertions.assertEquals(RDF.type.getURI(), tm.termToResource("rdf:type").getURI());

        Assertions.assertEquals(OWL.Class.getURI(), tm.termToResource("owl:Class").getURI());
    }
    @Test
    public void test_objectToLiteralOrResource() {
        /* 1, xsd:int                   -> int
         * op2__4, owl:NamedIndividual  -> Resource
         * true, xsd:bool               -> bool
         * true                         -> string
         * a                            -> string
         * */

        String ns = TermMapping.BASE_URI_DEFAULT + "#";

        Assertions.assertEquals(1, tm.objectToLiteralOrResource("1", "xsd:int").asLiteral().getInt());

        Assertions.assertEquals("op2__4", tm.objectToLiteralOrResource("op2__4", "owl:NamedIndividual").asResource().getLocalName());

        Assertions.assertEquals(true, tm.objectToLiteralOrResource("true", "xsd:bool").asLiteral().getBoolean());

        Assertions.assertEquals("true", tm.objectToLiteralOrResource("true").asLiteral().getString());

        Assertions.assertEquals("a", tm.objectToLiteralOrResource("a").asLiteral().getString());

        Assertions.assertEquals("1", tm.objectToLiteralOrResource("1").asLiteral().getString());
    }

    @Test
    public void test_resourceToTerm() {
        /* 1, xsd:int                   -> int
         * op2__4, owl:NamedIndividual  -> Resource
         * true, xsd:bool               -> bool
         * true                         -> string
         * a                            -> string
         * */

        Assertions.assertEquals("_1", tm.resourceToTerm(tm.termToResource("_1")));
        Assertions.assertEquals("", tm.resourceToTerm(tm.termToResource("")));

        Assertions.assertEquals("a", tm.resourceToTerm(tm.termToResource("a")));
        Assertions.assertEquals("a", tm.resourceToTerm(tm.termToResource(":a")));
        Assertions.assertEquals("a", tm.resourceToTerm(tm.termToResource("my:a")));

        Assertions.assertEquals("rdf:type", tm.resourceToTerm(tm.termToResource("rdf:type")));
        Assertions.assertEquals("rdf:type_XXYZ", tm.resourceToTerm(tm.termToResource("rdf:type_XXYZ")));
        Assertions.assertEquals("owl:Class", tm.resourceToTerm(tm.termToResource("owl:Class")));
        Assertions.assertEquals("owl:Class_XXYZ", tm.resourceToTerm(tm.termToResource("owl:Class_XXYZ")));
    }
}
