package org.vstu.compprehension.service.lti;

import java.util.Map;

public interface LtiContextInitializer {
    void init(Map<String, Object> claims);
}
