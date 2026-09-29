package org.vstu.compprehension.services;

public interface EducationResourceService {
    /** @throws SecurityException если LMS не доверенная */
    void ensureTrusted(long educationResourceId);
}
