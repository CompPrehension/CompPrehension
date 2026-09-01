package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiDeepLinkingContext;

import java.util.Optional;

/**
 * Out-port: LTI-контекст текущего запроса.
 */
public interface LtiContextProvider {
    Optional<LtiContext> getCurrentLtiContext();

    /**
     * Deep Linking контекст текущей сессии (если запуск был {@code LtiDeepLinkingRequest}).
     */
    Optional<LtiDeepLinkingContext> getCurrentDeepLinkingContext();
}
