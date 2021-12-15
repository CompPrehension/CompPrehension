package org.vstu.compprehension.models.businesslogic.storage;

public class NamespaceUtil {
    static String PATH_SEPARATORS = "/#";

    private final String prefix;
    private String base_cached = null;

    public NamespaceUtil(String prefix) {
        this.prefix = prefix;
    }

    public String get() {
        return this.prefix;
    }

    public String get(String name) {
        return this.prefix + name;
    }

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
