package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.services.EducationResourceService;

@Component
@RequiredArgsConstructor
public class EducationResourceFrontendServiceImpl implements EducationResourceFrontendService {
    private final EducationResourceService educationResourceService;

    @Override
    public void ensureTrusted(long educationResourceId) {
        educationResourceService.ensureTrusted(educationResourceId);
    }
}
