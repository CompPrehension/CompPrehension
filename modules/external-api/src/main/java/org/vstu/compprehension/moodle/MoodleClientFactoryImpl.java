package org.vstu.compprehension.moodle;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
class MoodleClientFactoryImpl implements MoodleClientFactory {
    private final RestTemplate restTemplate;

    MoodleClientFactoryImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public MoodleClient create(String baseUrl, String wsToken) {
        return new MoodleClientImpl(restTemplate, baseUrl, wsToken);
    }
}
