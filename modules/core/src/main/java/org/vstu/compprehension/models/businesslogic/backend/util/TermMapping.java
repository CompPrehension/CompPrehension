package org.vstu.compprehension.models.businesslogic.backend.util;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;


/** Term to RDF node mapping and inverse mapping.
 * It also counts consequent uses of cached items
 * and deletes them when reached some defined limit.
 *
 */
@Log4j2
public class TermMapping {
    public final static String BASE_URI_DEFAULT = "http://vstu.ru/poas/code";
    final static String DEFAULT_NS_PREFIX = "my";

    protected String baseNS;
    protected String baseNSPrefix;

    @Getter
    protected Model model;

    static BiMap<String, Resource> commonTerm2Resource;
    static {
        commonTerm2Resource = HashBiMap.create();
        commonTerm2Resource.put("rdf:type", RDF.type);
        commonTerm2Resource.put("rdf:Property", RDF.Property);
        commonTerm2Resource.put("rdf:resource", resource(RDF.uri, "resource"));
        commonTerm2Resource.put("rdf:subject", RDF.subject);

        commonTerm2Resource.put("rdfs:domain", RDFS.domain);
        commonTerm2Resource.put("rdfs:range", RDFS.range);
        commonTerm2Resource.put("rdfs:label", RDFS.label);
        commonTerm2Resource.put("rdfs:subPropertyOf", RDFS.subPropertyOf);
        commonTerm2Resource.put("rdfs:subClassOf", RDFS.subClassOf);

        commonTerm2Resource.put("owl:Thing", OWL.Thing);
        commonTerm2Resource.put("owl:Class", OWL.Class);
        commonTerm2Resource.put("owl:ObjectProperty", OWL.ObjectProperty);
        commonTerm2Resource.put("owl:DatatypeProperty", OWL.DatatypeProperty);
        commonTerm2Resource.put("owl:AnnotationProperty", OWL.AnnotationProperty);

        commonTerm2Resource.put("dc:identifier", DCTerms.identifier);
        commonTerm2Resource.put("meta:question", resource("http://meta.ns/", "question"));
    }

    protected static Resource resource(String uri, String local )
    { return ResourceFactory.createResource( uri + local ); }

    public TermMapping() {
        this(BASE_URI_DEFAULT + "#");
    }

    public TermMapping(String baseNSPrefix) {
        this.baseNSPrefix = baseNSPrefix;

        // cut separator char from prefix
        if (baseNSPrefix.endsWith("/") || baseNSPrefix.endsWith("#"))
            this.baseNS = baseNSPrefix.substring(0, baseNSPrefix.length() - 1);
        else
            this.baseNS = baseNSPrefix;

        initModel();
    }

    //
    private void initModel() {
        model = ModelFactory.createDefaultModel();
        model.setNsPrefix(DEFAULT_NS_PREFIX, baseNSPrefix);
        model.setNsPrefix("", baseNSPrefix);
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("rdfs",RDFS.getURI());
        model.setNsPrefix("owl", OWL.getURI());
        // http://www.w3.org/2004/02/skos/core#broader
        model.setNsPrefix("skos", SKOS.getURI());
        model.setNsPrefix("xsd", XSD.getURI());
        model.setNsPrefix("dc", DCTerms.getURI());
        model.setNsPrefix("meta", "http://meta.ns/");  // this is not a known uri, just invented "ad-hoc"
    }

    /** Expand simple name as local, or prefixed name as special
     * `a` `:a` `my:a`
     * rdf:type owl:Class
     * */
    public String termToUri(String s) {
        String uri;

        int colon = s.indexOf(':');
        if (colon == -1) {
            uri = baseNSPrefix + s;
        } else if (colon == 0) {
            uri = baseNSPrefix + s.substring(1);  // cut ':' from the beginning
        } else {
            uri = model.expandPrefix(s);
            /* String prefix = null;
            String ns = null;
            for (String key : commonPrefixes.keySet()) {
                if (s.startsWith(key)) {
                    uri = s.substring(key.length()) + commonPrefixes.get(key);
                    break;
                }
            }*/
        }
        return uri;
    }
    public Resource termToResource(String s) {
        // ask cache
        if (commonTerm2Resource.containsKey(s))
            return commonTerm2Resource.get(s);

        return ResourceFactory.createResource(termToUri(s));
    }
    public Property termToProperty(String s) {
        // ask cache
        if (commonTerm2Resource.containsKey(s))
            return commonTerm2Resource.get(s).as(Property.class);

        return ResourceFactory.createProperty(termToUri(s));
    }
    public RDFNode objectToLiteralOrResource(String obj) {
         return objectToLiteralOrResource(obj, null);
    }
    /**
     * 1, xsd:int                   -> int
     * op2__4, owl:NamedIndividual  -> Resource
     * true, xsd:bool               -> bool
     * true                         -> string
     * a                            -> string
     * */
    public RDFNode objectToLiteralOrResource(String obj, String objType) {
        if (objType == null || "".equals(objType)) {
            // just a string
            return model.createLiteral(obj);
        }
        if (objType.startsWith("xsd:")) {
            // datatype relation
            switch (objType) {
                case "xsd:int":
                case "xsd:integer":
                case "xsd:biginteger": {
                    return model.createTypedLiteral(Integer.parseInt(obj));
                }
                case "xsd:str":
                case "xsd:string": {
                    return model.createLiteral(obj);
                }
                case "xsd:bool":
                case "xsd:boolean": {
                    return model.createTypedLiteral(Boolean.parseBoolean(obj));
                }
                case "xsd:double": {
                    return model.createTypedLiteral(Double.parseDouble(obj));
                }
                case "xsd:float": {
                    return model.createTypedLiteral(Float.parseFloat(obj));
                }
                default:
                    throw new UnsupportedOperationException("TermMapping.objectToLiteralOrResource(): unknown datatype in objectType: " + objType);
            }
        } else if (objType.contains(":")) {
            // treat namespaced obType as resource
            return termToResource(obj);
        }
        // default to string (again)
        return model.createLiteral(obj);
    }

    /** Returns local name or prefixed special name */
    public String resourceToTerm(Resource r) {
        if (r == null) {
            log.info("uriToTerm(): Encountered NULL uri! Defaulting to '[]'");
            return "[]";
        }

        if (commonTerm2Resource.inverse().containsKey(r))
            return commonTerm2Resource.inverse().get(r);

        String ns = r.getNameSpace();
        if (ns == null) {
            String uri = r.getURI();
            if (uri == null)
                return "[" + r.getId() + "]";
            return uri;
        }
        if (ns.equals(baseNSPrefix)) {
            return r.getLocalName();
        } else {
            String uri = r.getURI();
            if (uri.startsWith(baseNSPrefix)) {
                return uri.substring(baseNSPrefix.length());
            }
        }

        // not a local name
        String uri = r.getURI();
        String s = model.qnameFor(uri);
        if (s == null) {
            // no qName available
            s = uri;
        }
        return s;
    }

    /** Returns local e or prefixed special name */
    public Pair<String, String> literalToTerm(Literal r) {
        return Pair.of(r.getLexicalForm(), "xsd:" + r.getDatatype().getJavaClass().getSimpleName().toLowerCase());
    }

    public Pair<String, String> literalOrResourceToStringAndType(RDFNode obj) {
        if (obj instanceof Resource) {
            Resource r = (Resource) obj;
            return Pair.of(resourceToTerm(r), "rdf:resource");
        }
        return literalToTerm((Literal) obj);
    }
}
