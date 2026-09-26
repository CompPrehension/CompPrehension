package org.vstu.compprehension.common;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;

public final class LmsUrlHelper {

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
            return (scheme + "://" + authority).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
