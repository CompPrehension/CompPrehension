package org.vstu.compprehension.utils.threads;

import lombok.val;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.ThreadContextDataInjector;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.Callable;

public class ContextAwareCallable<T> implements Callable<T> {
    private Callable<T> task;
    private RequestAttributes context;
    private Map<String, String> loggingCtx;

    public ContextAwareCallable(Callable<T> task, RequestAttributes context, Map<String, String> loggingCtx) {
        this.task = task;
        this.context = context;
        this.loggingCtx = loggingCtx;
    }

    @Override
    public T call() throws Exception {
        if (context != null) {
            RequestContextHolder.setRequestAttributes(context);
        }
        if (loggingCtx != null) {
            ThreadContext.putAll(loggingCtx);
        };

        try {
            return task.call();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
