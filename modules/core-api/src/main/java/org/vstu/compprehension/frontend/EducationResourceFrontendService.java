package org.vstu.compprehension.frontend;

public interface EducationResourceFrontendService {
    /** @throws SecurityException если LMS не доверенная */
    void ensureTrusted(long educationResourceId);
}
