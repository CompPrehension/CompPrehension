package org.vstu.compprehension.businesslogic.lti;

import java.util.List;

public record LtiDeepLinkingContext(
        String platformIssuer,
        String deploymentId,
        String deepLinkReturnUrl,
        String data,
        String lineitemsUrl,
        List<String> agsScopes
) {
}
