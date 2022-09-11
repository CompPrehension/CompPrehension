package org.vstu.compprehension.models.businesslogic.storage;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.jetbrains.annotations.NotNull;

public class NamespaceUtil {
    static String PATH_SEPARATORS = "/#";

    private final String prefix;
    private String base_cached = null;

    public NamespaceUtil(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return namespace prefix as-is
     */
    public String get() {
        return this.prefix;
    }

    /**
     * @return local name concatenated to namespace prefix
     */
    public String get(String name) {
        return this.prefix + name;
    }

    /**
     * @return local name and namespace prefix concatenation as URI
     */
    public Node getUri(String name) {
        return NodeFactory.createURI(get(name));
    }

    /** Create a Property instance with URI equal to this.get(name).
     * Subsequent operations on the returned property may modify the model.
     * @param model Model to create property for
     * @return a new property linked to model
     */
    public Property getPropertyOnModel(String name, @NotNull Model model) {
        return model.createProperty(get(), name);
    }
    /** Create a Resource instance with URI equal to this.get(name).
     * Operations on the result Resource may change the model.
     * @param model Model to create resource for
     * @return a new resource linked to model
     */
    public Resource getResourceOnModel(String name, @NotNull Model model) {
        return model.createResource(get(name));
    }

    /**
     * @return namespace prefix with stripped trailing path separators ('/' and '#'), if any
     */
    public String base() {
        if (base_cached == null) {
            int len = prefix.length();
            if (PATH_SEPARATORS.contains(prefix.substring(len - 1))) {
                base_cached = prefix.substring(0, len - 1);
            }
            else {
                base_cached = prefix;
            }
        }
        return base_cached;
    }

    /**
     * @return namespace prefix without trailing '/' or '#' as URI
     */
    public Node baseAsUri() {
        return NodeFactory.createURI(base());
    }

    /** Create a Property instance with URI equal to this.base().
     * Subsequent operations on the returned property may modify the model.
     * @param model Model to create property for
     * @return a new property linked to model
     */
    public Property baseAsPropertyOnModel(@NotNull Model model) {
        return model.createProperty(base());
    }
    /** Create a Resource instance with URI equal to this.base().
     * Operations on the result Resource may change the model.
     * @param model Model to create resource for
     * @return a new resource linked to model
     */
    public Resource baseAsResourceOnModel(@NotNull Model model) {
        return model.createResource(base());
    }


    /** Cut prefix from qualified fullname
     * @param fullname name with prefix, possibly the same as this namespace prefix
     * @return relative part of fullname (or whole fullname is prefixes do not match)
     */
    public String localize(String fullname) {
        if (fullname.startsWith(prefix)) {

            int len = prefix.length();
            return fullname.substring(len);
        }
        return fullname;
    }
}
