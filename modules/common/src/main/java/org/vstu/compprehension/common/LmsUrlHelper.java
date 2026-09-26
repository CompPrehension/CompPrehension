package org.vstu.compprehension.common;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;

public final class LmsUrlHelper {

    private LmsUrlHelper() {
    }

    /**
     * Канонический URL LMS для записи в {@code EducationResourceEntity.url}:
     * {@code scheme://authority} из LTI issuer claim (например {@code http://localhost:8081}),
     * в нижнем регистре, без path/query/fragment и trailing slash. Этот же формат ожидают
     * {@code WsFuncMoodleConfig.base-url} и {@code LtiRegistrationsProperties.issuer-url}.
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
