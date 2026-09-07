package org.vstu.compprehension.services;

import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.businesslogic.lti.LtiDeepLinkingContext;

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
