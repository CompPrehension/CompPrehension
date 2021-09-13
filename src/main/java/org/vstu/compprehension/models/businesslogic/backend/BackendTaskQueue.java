package org.vstu.compprehension.models.businesslogic.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.utils.TaskQueue;

import javax.inject.Singleton;

@Component @Singleton
public class BackendTaskQueue extends TaskQueue {
    public BackendTaskQueue(@Value("${config.property.backendsPoolSize}") int backendsPoolSize, @Value("${config.property.backendsQueueSize}") int backendsQueueSize) {
        super(backendsPoolSize, backendsQueueSize);
    }
}
