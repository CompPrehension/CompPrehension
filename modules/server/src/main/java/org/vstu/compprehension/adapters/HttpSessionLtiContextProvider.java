package org.vstu.compprehension.adapters;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.Service.LtiContextProvider;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;

import java.util.Optional;

/**
 * Читает LTI-контекст из атрибутов текущей HTTP-сессии (их кладёт
 * {@link org.vstu.compprehension.controllers.LtiController} при LTI launch).
 * Request-scoped: данные читаются заново на каждый запрос, чтобы не закешировать устаревшее значение после повторного
 * LTI launch в той же сессии.
 */
@Component
@RequestScope
public class HttpSessionLtiContextProvider implements LtiContextProvider {

    private final HttpSession session;

    public HttpSessionLtiContextProvider(HttpSession session) {
        this.session = session;
    }

    @Override
    public Optional<LtiContext> getCurrentLtiContext() {
        String lineitemUrl = (String) session.getAttribute("ltiLineitemUrl");
        if (lineitemUrl == null) return Optional.empty();
        String contextId = (String) session.getAttribute("ltiContextId");
        return Optional.of(new LtiContext(lineitemUrl, contextId));
    }
}
