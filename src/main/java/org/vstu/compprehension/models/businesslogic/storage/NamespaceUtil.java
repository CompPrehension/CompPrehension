package org.vstu.compprehension.models.businesslogic.storage;

public class NamespaceUtil {
    static String PATH_SEPARATORS = "/#";

    private String prefix;

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
        int len = prefix.length();
        if (PATH_SEPARATORS.contains(prefix.substring(len - 1)))
            return prefix.substring(0, len - 1);
        return prefix;
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
