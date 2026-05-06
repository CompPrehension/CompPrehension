package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.businesslogic.lti.LtiContext;

import java.util.Optional;

/**
 * Out-port: LTI-контекст текущего запроса.
 */
public interface LtiContextProvider {
    Optional<LtiContext> getCurrentLtiContext();
}
