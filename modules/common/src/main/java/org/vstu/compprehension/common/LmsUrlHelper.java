package org.vstu.compprehension.common;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class LmsUrlHelper {
    private static final Pattern TRAILING_SLASHES = Pattern.compile("/+$");

    private LmsUrlHelper() {
    }

    /**
     * Канонический URL LMS.
     */
    public static @Nullable String toCanonicalLmsUrl(@Nullable String issuer) {
        if (issuer == null || issuer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(issuer.trim());
            String scheme = uri.getScheme();
            String authority = uri.getAuthority();
            if (scheme == null || authority == null) {
                return null;
            }
            String path = uri.getRawPath() == null ? "" : TRAILING_SLASHES.matcher(uri.getRawPath()).replaceAll("");
            return (scheme + "://" + authority).toLowerCase(Locale.ROOT) + path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
