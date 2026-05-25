package org.vstu.compprehension.adapters;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.service.lti.LtiContextInitializer;
import org.vstu.compprehension.Service.LtiContextProvider;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;

import java.io.Serializable;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@SessionScope
public class LtiContextHolder implements LtiContextProvider, LtiContextInitializer, Serializable {

    private static final String LTI_CLAIM_CONTEXT       = "https://purl.imsglobal.org/spec/lti/claim/context";
    private static final String LTI_CLAIM_AGS           = "https://purl.imsglobal.org/spec/lti-ags/claim/endpoint";
    private static final String LTI_CLAIM_TOOL_PLATFORM = "https://purl.imsglobal.org/spec/lti/claim/tool_platform";
    private static final String LTI_CLAIM_CUSTOM        = "https://purl.imsglobal.org/spec/lti/claim/custom";

    private LtiContext context;

    @Override
    public Optional<LtiContext> getCurrentLtiContext() {
        return Optional.ofNullable(context);
    }

    @Override
    public void init(Map<String, Object> claims) {

        String issuer = (String) claims.get("iss");
        String lmsUrl = canonicalLmsUrl(issuer);

        String lmsName = null;
        EducationResourceType lmsType = EducationResourceType.UNKNOWN;
        Map<?, ?> toolPlatform = (Map<?, ?>) claims.get(LTI_CLAIM_TOOL_PLATFORM);
        if (toolPlatform != null) {
            Object name = toolPlatform.get("name");
            if (name != null) lmsName = String.valueOf(name);
            Object familyCode = toolPlatform.get("product_family_code");
            lmsType = EducationResourceType.fromString(familyCode != null ? String.valueOf(familyCode) : null);
        }

        LtiCourseContext course = null;
        Map<?, ?> ltiContext = (Map<?, ?>) claims.get(LTI_CLAIM_CONTEXT);
        if (ltiContext != null) {
            Object id = ltiContext.get("id");
            Object title = ltiContext.get("title");
            if (id != null) {
                course = new LtiCourseContext(String.valueOf(id), title != null ? String.valueOf(title) : null);
            }
        }

        String lineitemUrl = null;
        Map<?, ?> agsEndpoint = (Map<?, ?>) claims.get(LTI_CLAIM_AGS);
        if (agsEndpoint != null && agsEndpoint.get("lineitem") != null) {
            lineitemUrl = String.valueOf(agsEndpoint.get("lineitem"));
        }

        Long exerciseId = null;
        Map<?, ?> customClaims = (Map<?, ?>) claims.get(LTI_CLAIM_CUSTOM);
        if (customClaims != null) {
            Object exerciseIdRaw = customClaims.get("exercise_id");
            if (exerciseIdRaw != null) {
                exerciseId = Long.parseLong(String.valueOf(exerciseIdRaw));
            }
        }

        this.context = new LtiContext(lineitemUrl, course, lmsUrl, lmsName, lmsType, exerciseId);
    }

    /**
     * Канонический URL LMS для записи в {@code EducationResourceEntity.url}:
     * {@code scheme://authority} из LTI issuer claim (например {@code http://localhost:8081}),
     * в нижнем регистре, без path/query/fragment и trailing slash. Этот же формат ожидают
     * {@code WsFuncMoodleConfig.base-url} и {@code LtiRegistrationsProperties.issuer-url}.
     */
    private static String canonicalLmsUrl(String issuer) {
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
