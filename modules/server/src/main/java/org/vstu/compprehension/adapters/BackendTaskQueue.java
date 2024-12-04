package org.vstu.compprehension.adapters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.utils.TaskQueue;

import javax.inject.Singleton;

@Component @Singleton
public class BackendTaskQueue extends TaskQueue {
    public BackendTaskQueue(@Value("${config.property.backendsPoolSize:4}") int backendsPoolSize, @Value("${config.property.backendsQueueSize:30}") int backendsQueueSize) {
        super(backendsPoolSize, backendsQueueSize);
    }
}
