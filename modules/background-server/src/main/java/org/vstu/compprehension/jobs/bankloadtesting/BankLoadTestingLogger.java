package org.vstu.compprehension.jobs.bankloadtesting;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class BankLoadTestingLogger {
    
    void setContextVariable(String name, String value) {
        ThreadContext.put("userId", value);
    }

    void removeContextVariable(String name) {
        ThreadContext.remove(name);
    }

    void info(String message, Object p0) {
        log.info(message, p0);
    }

    void info(String message, Object p0, Object p1) {
        log.info(message, p0, p1);
    }

    void info(String message, Object p0, Object p1, Object p2) {
        log.info(message, p0, p1, p2);
    }
    
    void warn(String message) {
        log.warn(message);
    }

    void error(String message, Object p0) {
        log.error(message, p0);
    }

    void error(String message, Object p0, Object p1) {
        log.error(message, p0, p1);
    }
    
    void error(String message, Object p0, Object p1, Object p2) {
        log.error(message, p0, p1, p2);
    }

    void debug(String message, Object p0) {
        log.debug(message, p0);
    }

    void debug(String message, Object p0, Object p1) {
        log.debug(message, p0, p1);
    }

    void debug(String message, Object p0, Object p1, Object p2) {
        log.debug(message, p0, p1, p2);
    }
}
