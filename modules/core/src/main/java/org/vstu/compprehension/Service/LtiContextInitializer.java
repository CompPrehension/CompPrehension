package org.vstu.compprehension.Service;

import java.util.Map;

public interface LtiContextInitializer {
    void init(Map<String, Object> claims);
}
